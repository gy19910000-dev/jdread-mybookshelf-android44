package com.kunfei.bookshelf.help;
import androidx.annotation.Keep;
import com.kunfei.bookshelf.base.BaseModelImpl;
import com.kunfei.bookshelf.model.analyzeRule.AnalyzeUrl;
import java.nio.charset.Charset;
import java.util.*;
import okhttp3.*;

public final class JsHttp {
    @Keep public static class Result {
        private final String body,url;private final int code;private final Headers headers;
        Result(String body,String url,int code,Headers headers){this.body=body;this.url=url;this.code=code;this.headers=headers;}
        public String body(){return body;}public String url(){return url;}public int code(){return code;}public int statusCode(){return code;}public String header(String name){return headers.get(name);}public Map<String,List<String>> headers(){return headers.toMultimap();}
        public String toString(){return body;}
    }
    public static okhttp3.Response execute(AnalyzeUrl analyze,boolean redirects)throws Exception {
        String target=analyze.getUrl();
        if(analyze.getModernRequest()==null && analyze.getUrlMode()==AnalyzeUrl.UrlMode.GET && analyze.getQueryStr()!=null) target+="?"+analyze.getQueryStr();
        Request.Builder request=new Request.Builder().url(target);
        for(Map.Entry<String,String> entry:analyze.getHeaderMap().entrySet())if(entry.getValue()!=null)request.header(entry.getKey(),entry.getValue());
        RuleUrlParser.Request spec=analyze.getModernRequest();String method=spec==null?analyze.getUrlMode()==AnalyzeUrl.UrlMode.POST?"POST":"GET":spec.method;
        RequestBody body=null;
        if(!method.equals("GET")&&!method.equals("HEAD")){
            String contentType=analyze.getHeaderMap().get("Content-Type");if(contentType==null)contentType="application/x-www-form-urlencoded";
            byte[] bytes=spec!=null?(spec.body==null?new byte[0]:spec.body.getBytes(Charset.forName(spec.charset==null?"UTF-8":spec.charset))):analyze.getPostData();
            body=RequestBody.create(MediaType.parse(contentType),bytes);
        }
        request.method(method,body);
        return BaseModelImpl.getClient().newBuilder().followRedirects(redirects).followSslRedirects(redirects).build().newCall(request.build()).execute();
    }
    public static String decode(okhttp3.Response response,String charset)throws Exception {
        if(response.body()==null)return "";
        return EncodeConverter.create(charset).responseBodyConverter(String.class,new java.lang.annotation.Annotation[0],null).convert(response.body());
    }
    public static Result send(AnalyzeUrl analyze,boolean redirects)throws Exception{
        okhttp3.Response response=execute(analyze,redirects);try{
            String text=decode(response,analyze.getCharCode());Source3Store.Source source=analyze.jsSource();
            if(source!=null&&!response.headers("Set-Cookie").isEmpty()){
                SourceCookie cookie=new SourceCookie();LinkedHashMap<String,String> values=new LinkedHashMap<>();
                for(String item:cookie.getCookie(source.getKey()).split(";")){int eq=item.indexOf('=');if(eq>0)values.put(item.substring(0,eq).trim(),item.substring(eq+1));}
                for(String line:response.headers("Set-Cookie")){String item=line.split(";",2)[0];int eq=item.indexOf('=');if(eq>0)values.put(item.substring(0,eq).trim(),item.substring(eq+1));}
                StringBuilder merged=new StringBuilder();for(Map.Entry<String,String> item:values.entrySet())merged.append(item.getKey()).append('=').append(item.getValue()).append(';');cookie.setCookie(source.getKey(),merged.toString());
            }
            return new Result(text,response.request().url().toString(),response.code(),response.headers());
        }finally{response.close();}
    }
}
