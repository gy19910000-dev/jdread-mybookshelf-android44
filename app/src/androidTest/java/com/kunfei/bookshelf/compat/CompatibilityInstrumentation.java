package com.kunfei.bookshelf.compat;

import android.app.Instrumentation;
import android.os.Bundle;
import com.google.gson.*;
import com.kunfei.bookshelf.bean.*;
import com.kunfei.bookshelf.help.*;
import com.kunfei.bookshelf.model.*;
import com.kunfei.bookshelf.model.analyzeRule.*;
import com.kunfei.bookshelf.view.activity.SourceVerificationActivity;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Operates only on the supplied public test sources; reports counts instead of book contents. */
public class CompatibilityInstrumentation extends Instrumentation {
    private Bundle args;
    public void onCreate(Bundle arguments){super.onCreate(arguments);androidx.multidex.MultiDex.installInstrumentation(getContext(),getTargetContext());args=arguments==null?new Bundle():arguments;start();}
    private void report(String text){Bundle message=new Bundle();message.putString("stream",text+"\n");sendStatus(0,message);}
    public void onStart(){new Thread(()->runChecks()).start();}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void runChecks(){Bundle result=new Bundle();try{
        SourceVerificationActivity.allowVerification.set(false);
        require("abc1,abc2".equals(com.kunfei.bookshelf.constant.AppConstant.SCRIPT_ENGINE.eval("const x='abc'; [1,2].map(v=>`${x}${v}`).join(',')")),"ES6 runtime");
        AnalyzeRule analyzer=new AnalyzeRule(null).setContent("{\"book_id\":\"123\",\"name\":\"测试$书\"}","https://example.org/");
        require("https://example.org/book/123".equals(analyzer.getString("/book/{{$.book_id}}",true)),"JSON path interpolation");
        require(analyzer.getString("$.name").contains("$"),"literal dollar preservation");
        report("DEVICE_RUNTIME PASS api="+android.os.Build.VERSION.SDK_INT);
        String supplied=args.getString("fixture","");
        java.io.InputStream stream=supplied.equals("current")?new java.io.FileInputStream("/sdcard/Download/jdread-compat-sources.json"):getContext().getAssets().open("sources.json");java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=stream.read(buffer))!=-1)bytes.write(buffer,0,n);stream.close();
        String json=new String(bytes.toByteArray(),"UTF-8");List<BookSourceBean> sources=BookSourceManager.parseBookSourceArray(json);
        for(BookSourceBean source:sources)BookSourceManager.addBookSource(source);
        report("IMPORT PASS count="+sources.size());
        if("true".equals(args.getString("configure"))&&supplied.equals("current")){
            com.kunfei.bookshelf.MApplication.getConfigPreferences().edit().putString("search_group","JDRead实测").commit();
            com.kunfei.bookshelf.MApplication.SEARCH_GROUP="JDRead实测";report("SEARCH GROUP JDRead实测");
        }
        String filter=args.getString("sources","");String keyword=args.getString("key","斗破苍穹");int ok=0,failed=0;
        for(BookSourceBean source:sources){if(!filter.isEmpty()&&!Arrays.asList(filter.split(",")).contains(source.getBookSourceName()))continue;
            try{
                if("true".equals(args.getString("diagnostic"))) {
                    AnalyzeUrl request=new AnalyzeUrl(source.getRuleSearchUrl(),keyword,1,com.kunfei.bookshelf.model.analyzeRule.AnalyzeHeaders.getMap(source),source.getBookSourceUrl());
                    JsHttp.Result response=JsHttp.send(request,true);String body=response.body();
                    report("HTTP "+source.getBookSourceName()+" status="+response.code()+" chars="+body.length()+" url="+response.url());
                    if(body.trim().startsWith("{")){JsonObject root=JsonParser.parseString(body).getAsJsonObject();report("JSON keys="+root.keySet()+" code="+root.get("code")+" status="+root.get("status")+" data_null="+(!root.has("data")||root.get("data").isJsonNull()));}
                    else report("HTML shape: form="+body.contains("<form")+" challenge="+(body.contains("Cloudflare")||body.contains("challenge"))+" list_rule="+source.getRuleSearchList());
                    AnalyzeRule rules=new AnalyzeRule(null).setSource(source).setContent(body,response.url());if(!body.trim().startsWith("{"))report("HTML title="+rules.getString("tag.title@text"));List<Object> items=rules.getElements(source.getRuleSearchList());report("RULE items="+items.size());if(!items.isEmpty())report("RULE first_name="+rules.setContent(items.get(0)).getString(source.getRuleSearchName()));
                    continue;
                }
                List<SearchBookBean> books=WebBookModel.getInstance().searchBook(keyword,1,source.getBookSourceUrl()).timeout(25,TimeUnit.SECONDS).blockingFirst();
                require(!books.isEmpty(),"empty results");ok++;report("SEARCH PASS "+source.getBookSourceName()+" count="+books.size()+" title="+books.get(0).getName());
                if("true".equals(args.getString("read"))){
                    try{BookShelfBean shelf=BookshelfHelp.getBookFromSearchBook(books.get(0));shelf=WebBookModel.getInstance().getBookInfo(shelf).timeout(25,TimeUnit.SECONDS).blockingFirst();List<BookChapterBean> chapters=WebBookModel.getInstance().getChapterList(shelf).timeout(40,TimeUnit.SECONDS).blockingFirst();require(!chapters.isEmpty(),"no chapters");report("TOC PASS "+source.getBookSourceName()+" chapters="+chapters.size());BookContentBean content=WebBookModel.getInstance().getBookContent(shelf,chapters.get(0),chapters.size()>1?chapters.get(1):null).timeout(25,TimeUnit.SECONDS).blockingFirst();require(content.getDurChapterContent()!=null&&!content.getDurChapterContent().isEmpty(),"empty chapter");report("CONTENT PASS "+source.getBookSourceName()+" chars="+content.getDurChapterContent().length());}
                    catch(Throwable error){report("READ FAIL "+source.getBookSourceName()+" "+describe(error));}
                }
            }catch(Throwable error){failed++;report("SEARCH FAIL "+source.getBookSourceName()+" "+describe(error));}
        }
        if("true".equals(args.getString("ui"))){
            android.content.Intent intent=new android.content.Intent(getTargetContext(),com.kunfei.bookshelf.view.activity.SearchBookActivity.class).putExtra("searchKey",keyword).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            android.app.Activity screen=startActivitySync(intent);int[] count={0};long end=System.currentTimeMillis()+25000;
            while(System.currentTimeMillis()<end){Thread.sleep(250);runOnMainSync(()->{try{Object adapter=screen.getClass().getMethod("getSearchBookAdapter").invoke(screen);count[0]=((Number)adapter.getClass().getMethod("getICount").invoke(adapter)).intValue();}catch(Exception error){count[0]=-1;}});if(count[0]!=0)break;}
            require(count[0]>0,"search screen has no results");report("UI SEARCH PASS visible_results="+count[0]);
        }
        result.putString("stream","COMPLETE searches_pass="+ok+" searches_failed="+failed+"\n");finish(0,result);
    }catch(Throwable error){result.putString("stream","DEVICE FAIL "+describe(error)+"\n");finish(1,result);}}
    private String describe(Throwable error){Throwable root=error;while(root.getCause()!=null&&root.getCause()!=root)root=root.getCause();StringBuilder trace=new StringBuilder(error.getMessage()+"; "+root.getClass().getSimpleName()+": "+root.getMessage());for(int i=0;i<Math.min(root.getStackTrace().length,28);i++)trace.append("\n  ").append(root.getStackTrace()[i]);return trace.toString();}
}
