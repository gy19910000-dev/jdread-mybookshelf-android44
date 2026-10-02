package com.kunfei.bookshelf.help;

import com.kunfei.bookshelf.base.BaseModelImpl;
import com.kunfei.bookshelf.model.analyzeRule.AnalyzeUrl;
import com.kunfei.bookshelf.utils.StringUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Response;

@SuppressWarnings({"unused", "WeakerAccess"})
public interface JsExtensions {

    default String jsBaseUrl() { return null; }
    default Source3Store.Source jsSource() { return Source3Store.find(jsBaseUrl()); }
    default Map<String,String> jsHeaders() {
        java.util.HashMap<String,String> headers=new java.util.HashMap<>();
        headers.put("User-Agent",com.kunfei.bookshelf.constant.AppConstant.DEFAULT_USER_AGENT);
        Source3Store.Source source=jsSource();
        if(source!=null) {
            try { if(source.header!=null && source.header.trim().startsWith("{")) headers.putAll(new com.google.gson.Gson().fromJson(source.header,com.kunfei.bookshelf.constant.AppConstant.MAP_STRING)); }catch(Exception ignored){}
            String cookie=new SourceCookie().getCookie(source.getKey());if(!cookie.isEmpty())headers.put("Cookie",cookie);
        }
        return headers;
    }

    /**
     * js实现跨域访问,不能删
     */
    default String ajax(String urlStr) {
        try {
            AnalyzeUrl analyzeUrl = new AnalyzeUrl(urlStr, jsHeaders(), jsBaseUrl());
            return JsHttp.send(analyzeUrl,true).body();
        } catch (Exception e) {
            throw new IllegalStateException("书源请求失败: " + e.getLocalizedMessage(),e);
        }
    }

    /**
     * js实现解码,不能删
     */
    default String base64Decoder(String base64) {
        return StringUtils.base64Decode(base64);
    }

    /**
     * 章节数转数字
     */
    default String toNumChapter(String s) {
        if (s == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("(第)(.+?)(章)");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1) + StringUtils.stringToInt(matcher.group(2)) + matcher.group(3);
        }
        return s;
    }

    /**
     * js实现重定向拦截,不能删
     */
    default JsHttp.Result get(String urlStr, Map<String, String> headers) throws Exception {
        Map<String,String> all=jsHeaders();if(headers!=null)all.putAll(headers);
        return JsHttp.send(new AnalyzeUrl(urlStr,all,jsBaseUrl()),false);
    }

    /**
     * js实现重定向拦截,不能删
     */
    default JsHttp.Result post(String urlStr, String body, Map<String, String> headers) throws Exception {
        Map<String,String> all=jsHeaders();if(headers!=null)all.putAll(headers);
        com.google.gson.JsonObject options=new com.google.gson.JsonObject();options.addProperty("method","POST");options.addProperty("body",body);
        return JsHttp.send(new AnalyzeUrl(urlStr+","+options,all,jsBaseUrl()),false);
    }

    default String encodeURI(String value) throws Exception { return encodeURI(value,"UTF-8"); }
    default String encodeURI(String value,String charset) throws Exception { return java.net.URLEncoder.encode(value,charset).replace("+","%20"); }
    default String base64Decode(String value) { return new String(android.util.Base64.decode(value,android.util.Base64.DEFAULT),java.nio.charset.Charset.forName("UTF-8")); }
    default String base64Encode(String value) { return android.util.Base64.encodeToString(value.getBytes(java.nio.charset.Charset.forName("UTF-8")),android.util.Base64.NO_WRAP); }
    default String hexDecodeToString(String value) { java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();for(int i=0;i+1<value.length();i+=2)out.write(Integer.parseInt(value.substring(i,i+2),16));return new String(out.toByteArray(),java.nio.charset.Charset.forName("UTF-8")); }
    default String timeFormat(long millis) { return timeFormat(millis,"yyyy-MM-dd HH:mm:ss"); }
    default String timeFormat(long millis,String format) { return new java.text.SimpleDateFormat(format,java.util.Locale.CHINA).format(new java.util.Date(millis)); }
    default String androidId() { return android.provider.Settings.Secure.getString(com.kunfei.bookshelf.MApplication.getInstance().getContentResolver(),android.provider.Settings.Secure.ANDROID_ID); }
    default String getCookie(String url) { return new SourceCookie().getCookie(url); }
    default String getCookie(String url,String name) { return new SourceCookie().getCookie(url,name); }
    default void log(Object text) { android.util.Log.d("SourceScript",String.valueOf(text)); }
    default void toast(Object text) { new android.os.Handler(android.os.Looper.getMainLooper()).post(()->android.widget.Toast.makeText(com.kunfei.bookshelf.MApplication.getInstance(),String.valueOf(text),android.widget.Toast.LENGTH_SHORT).show()); }
    default void longToast(Object text) { new android.os.Handler(android.os.Looper.getMainLooper()).post(()->android.widget.Toast.makeText(com.kunfei.bookshelf.MApplication.getInstance(),String.valueOf(text),android.widget.Toast.LENGTH_LONG).show()); }
    default String startBrowserAwait(String url,String title) throws Exception { return com.kunfei.bookshelf.view.activity.SourceVerificationActivity.await(url,title,jsSource()); }


}
