package com.kunfei.bookshelf.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import com.kunfei.bookshelf.MApplication;
import com.kunfei.bookshelf.help.*;
import java.util.concurrent.*;

/** User completes a site's verification; no automated CAPTCHA bypass. */
public class SourceVerificationActivity extends Activity {
    private static final ConcurrentHashMap<String,Pending> pending=new ConcurrentHashMap<>();
    private static final Semaphore visible=new Semaphore(1);
    public static final ThreadLocal<Boolean> allowVerification=new ThreadLocal<>();
    private WebView web;private Pending work;private String key;
    private static class Pending {final CountDownLatch done=new CountDownLatch(1);String html="",sourceKey;Pending(String sourceKey){this.sourceKey=sourceKey;}}
    public static String await(String url,String title,Source3Store.Source source)throws Exception {
        if(Boolean.FALSE.equals(allowVerification.get()))throw new IllegalStateException("书源需要手动网页验证");
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("网页验证需要在搜索线程运行");
        if(!visible.tryAcquire())throw new IllegalStateException("正在验证另一个书源，请稍后重试");
        String key=java.util.UUID.randomUUID().toString();Pending work=new Pending(source==null?url:source.getKey());pending.put(key,work);
        Intent intent=new Intent(MApplication.getInstance(),SourceVerificationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("key",key).putExtra("url",url).putExtra("title",title);
        new Handler(Looper.getMainLooper()).post(()->MApplication.getInstance().startActivity(intent));
        try{if(!work.done.await(120,TimeUnit.SECONDS))throw new IllegalStateException("网页验证未完成，请完成验证后重新搜索");return work.html;}finally{pending.remove(key);visible.release();}
    }
    protected void onCreate(Bundle state){super.onCreate(state);key=getIntent().getStringExtra("key");work=pending.get(key);if(work==null){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);TextView title=new TextView(this);title.setText(getIntent().getStringExtra("title"));title.setTextSize(19);title.setPadding(16,16,16,16);root.addView(title);
        Button done=new Button(this);done.setText("验证完成，继续搜索");root.addView(done);web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setUserAgentString(com.kunfei.bookshelf.constant.AppConstant.DEFAULT_USER_AGENT);web.setWebViewClient(new WebViewClient());root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        String saved=new SourceCookie().getCookie(work.sourceKey);if(!saved.isEmpty())CookieManager.getInstance().setCookie(getIntent().getStringExtra("url"),saved);web.loadUrl(getIntent().getStringExtra("url"));
        done.setOnClickListener(v->{String cookie=CookieManager.getInstance().getCookie(web.getUrl());if(cookie!=null)new SourceCookie().setCookie(work.sourceKey,cookie);web.evaluateJavascript("document.documentElement.outerHTML",value->{try{work.html=new com.google.gson.Gson().fromJson(value,String.class);}catch(Exception ignored){}work.done.countDown();finish();});});
    }
    protected void onDestroy(){if(work!=null)work.done.countDown();if(web!=null)web.destroy();super.onDestroy();}
}
