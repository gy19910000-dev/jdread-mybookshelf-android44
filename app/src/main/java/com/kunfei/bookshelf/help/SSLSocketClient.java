package com.kunfei.bookshelf.help;

import android.annotation.SuppressLint;

import java.net.InetAddress;
import java.net.Socket;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by GKF on 2018/3/1.
 * 忽略证书
 *
 * 2026 修改（Android 4.4 兼容性）：
 *
 * 背景：Android 4.4（API 19）平台自带的 OpenSSL 协议栈太老，与要求 TLS1.2 的现代站点
 * 握手会失败：tlsv1 alert protocol version / ssl3 alert handshake failure /
 * unsupported protocol，导致书源的搜索与正文抓取全部失败。原实现还用了
 * SSLContext.getInstance("SSL")，在 4.4 上会落到 SSLv3 时代的老协议。
 *
 * 修改点：
 *   1) 优先使用 Conscrypt（org.conscrypt:conscrypt-android），它自带一套现代 TLS
 *      实现和 armeabi-v7a 原生库，绕开平台协议栈的限制；
 *      用法：Conscrypt.newProvider() → SSLContext.getInstance("TLS", provider)
 *   2) Conscrypt 不可用时回退到平台实现，但仍强制启用设备真实支持的现代协议
 *      （TLSv1.3/1.2/1.1/1.0 中 getSupportedProtocols() 里存在的那些）
 *   3) SNI 由 OkHttp 在 createSocket(raw, host, port, true) 时传入 host 自动处理
 */
public class SSLSocketClient {

    /** 协议优先级：从高到低，仅启用设备/提供方实际支持的那些 */
    private static final String[] PREFERRED_PROTOCOLS = {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};

    /** 缓存，避免每次请求都重建 Provider 与 SSLContext */
    private static volatile SSLSocketFactory cachedFactory;

    //获取这个SSLSocketFactory
    public static SSLSocketFactory getSSLSocketFactory() {
        SSLSocketFactory factory = cachedFactory;
        if (factory != null) {
            return factory;
        }
        synchronized (SSLSocketClient.class) {
            if (cachedFactory == null) {
                cachedFactory = build();
            }
            return cachedFactory;
        }
    }

    private static SSLSocketFactory build() {
        // 1) 优先 Conscrypt
        try {
            Provider provider = org.conscrypt.Conscrypt.newProvider();
            SSLContext sslContext = SSLContext.getInstance("TLS", provider);
            sslContext.init(null, new TrustManager[]{createTrustAllManager()}, new SecureRandom());
            return new ProtocolEnforcingSocketFactory(sslContext.getSocketFactory());
        } catch (Throwable ignored) {
            // Conscrypt 不可用（缺对应 ABI 的原生库等）时回退
        }
        // 2) 回退：平台 TLS
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{createTrustAllManager()}, new SecureRandom());
            return new ProtocolEnforcingSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 包装 delegate：创建 socket 后强制启用现代协议。
     * OkHttp 的 ConnectionSpec 之后仍可在此基础上做进一步收窄。
     */
    private static class ProtocolEnforcingSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;

        ProtocolEnforcingSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private Socket configure(Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) socket;
                try {
                    List<String> supported = Arrays.asList(sslSocket.getSupportedProtocols());
                    List<String> enable = new ArrayList<>();
                    for (String p : PREFERRED_PROTOCOLS) {
                        if (supported.contains(p)) {
                            enable.add(p);
                        }
                    }
                    if (!enable.isEmpty()) {
                        sslSocket.setEnabledProtocols(enable.toArray(new String[0]));
                    }
                } catch (Exception ignored) {
                    // 个别 ROM 行为异常时保持默认，不影响后续握手
                }
            }
            return socket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws java.io.IOException {
            return configure(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws java.io.IOException {
            return configure(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws java.io.IOException {
            return configure(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws java.io.IOException {
            return configure(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws java.io.IOException {
            return configure(delegate.createSocket(address, port, localAddress, localPort));
        }
    }

    public static X509TrustManager createTrustAllManager() {
        X509TrustManager tm = null;
        try {
            tm = new X509TrustManager() {
                @SuppressLint("TrustAllX509TrustManager")
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    //do nothing，接受任意客户端证书
                }

                @SuppressLint("TrustAllX509TrustManager")
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    //do nothing，接受任意服务端证书
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
        } catch (Exception ignored) {
        }
        return tm;
    }

    //获取HostnameVerifier
    public static HostnameVerifier getHostnameVerifier() {
        return (s, sslSession) -> true;
    }
}
