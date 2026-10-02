package com.kunfei.bookshelf.help;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class SourceRuntimeTest {
    @Test public void trailingSourceSlashDoesNotLoseModernMetadata() {
        Source3Store.save("https://example.org/","{\"bookSourceUrl\":\"https://example.org/\",\"jsLib\":\"const prefix='book';\",\"ruleSearch\":{\"name\":\"$.name\"}}");
        assertEquals("https://example.org",Source3Store.find("https://example.org").getKey());
        assertTrue(Source3Store.find("https://example.org/search").jsLib.contains("prefix"));
    }
    @Test public void javaListsSupportModernArrayFunctions() {
        Map<String,Object> bindings=new HashMap<>();bindings.put("items",Arrays.asList("a","b"));
        assertEquals(Arrays.asList("A","B"),new Es6ScriptEngine().eval("items.map(x=>x.toUpperCase())",bindings));
    }
    private RuleUrlParser.Evaluator eval(String key,int page) {
        Es6ScriptEngine engine=new Es6ScriptEngine();
        return (script,result)->{Map<String,Object> bindings=new HashMap<>();bindings.put("key",key);bindings.put("page",page);bindings.put("result",result);bindings.put("baseUrl","https://example.org");return engine.eval(script,bindings);};
    }
    @Test public void modernJavascriptWorks()throws Exception {
        assertEquals("book1-book2",eval("",1).eval("const x='book'; [1,2].map(v=>`${x}${v}`).join('-')",null));
        assertEquals("ok",eval("",1).eval("try { throw new Error('test'); } catch { 'ok'; }",null));
    }
    @Test public void javascriptUrlKeepsSpacesAndOptions()throws Exception {
        RuleUrlParser.Request request=RuleUrlParser.parse("@js: const url=baseUrl+\"/search?key={{key}},{'method':'GET','headers':{'User-Agent':'a b'}}\"; url;",eval("三国",1));
        assertEquals("https://example.org/search?key=三国",request.url);assertEquals("a b",request.headers.get("User-Agent"));
    }
    @Test public void gbkPostAndPageExpressions()throws Exception {
        RuleUrlParser.Request request=RuleUrlParser.parse("/search,{'method':'POST','charset':'GBK','body':'keyword={{key}}&page={{(page-1)*50}}'}",eval("三国",3));
        assertEquals("keyword=%C8%FD%B9%FA&page=100",request.body);assertEquals("POST",request.method);
    }
    @Test public void interpolationDoesNotCorruptQuotesDollarOrBackslash()throws Exception {
        RuleUrlParser.Request request=RuleUrlParser.parse("/search,{'method':'POST','body':'q={{key}}'}",eval("O'Reilly$\\test",1));
        assertEquals("q=O%27Reilly%24%5Ctest",request.body);
    }
    @Test public void preludeThenRelativeUrl()throws Exception {
        RuleUrlParser.Request request=RuleUrlParser.parse("<js> if (false) 'unused'; </js>/search?key={{key}}",eval("book",1));assertEquals("/search?key=book",request.url);
    }
    @Test public void sourceLibraryIsAvailable()throws Exception {
        Map<String,Object> bindings=new HashMap<>();bindings.put("__jsLib","function title(x){ return `书：${x}`; }");assertEquals("书：测试",new Es6ScriptEngine().eval("title('测试')",bindings));
    }
    @Test public void simultaneousSearchesDoNotShareScope()throws Exception {
        Es6ScriptEngine engine=new Es6ScriptEngine();ExecutorService threads=Executors.newFixedThreadPool(4);
        try{List<Future<Object>> results=new ArrayList<>();for(int i=0;i<24;i++){final int id=i;results.add(threads.submit(()->{Map<String,Object> b=new HashMap<>();b.put("key","key"+id);return engine.eval("const local=key; local",b);}));}for(int i=0;i<24;i++)assertEquals("key"+i,results.get(i).get());}finally{threads.shutdownNow();}
    }
}
