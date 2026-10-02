package com.kunfei.bookshelf.help;

import com.google.gson.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

/** Reading 3 request syntax. Parse options before interpolation to preserve quoted search text. */
public final class RuleUrlParser {
    public interface Evaluator { Object eval(String script, Object result) throws Exception; }
    public static final class Request {
        public String url, method="GET", body, charset;
        public final Map<String,String> headers=new LinkedHashMap<>();
    }
    private static final Pattern TEMPLATE=Pattern.compile("\\{\\{([\\s\\S]*?)\\}\\}");

    public static Request parse(String rule, Evaluator evaluator) throws Exception {
        String expanded=expandScripts(rule.trim(),evaluator);
        if(expanded.isEmpty()) throw new IllegalArgumentException("书源返回了空网址");
        int options=optionsStart(expanded);
        Request request=new Request();
        request.url=interpolate(options<0?expanded:expanded.substring(0,options),evaluator);
        if(options>=0) {
            JsonObject object=new Gson().fromJson(expanded.substring(options+1),JsonObject.class);
            if(object.has("method")) request.method=text(object.get("method"),evaluator).toUpperCase(Locale.ROOT);
            if(object.has("charset")) request.charset=text(object.get("charset"),evaluator);
            if(object.has("body")) request.body=text(object.get("body"),evaluator);
            if(object.has("headers") && !object.get("headers").isJsonNull()) {
                JsonElement headers=object.get("headers");
                if(headers.isJsonPrimitive()) headers=new JsonParser().parse(text(headers,evaluator));
                for(Map.Entry<String,JsonElement> h:headers.getAsJsonObject().entrySet()) request.headers.put(h.getKey(),text(h.getValue(),evaluator));
            }
            if(object.has("webView") && object.get("webView").getAsBoolean())
                throw new IllegalArgumentException("此网址需要网页执行模式");
        }
        if(request.body!=null && !request.headers.containsKey("Content-Type"))
            request.headers.put("Content-Type", request.body.trim().startsWith("{")?"application/json":"application/x-www-form-urlencoded");
        if(request.body!=null && request.headers.get("Content-Type").startsWith("application/x-www-form-urlencoded"))
            request.body=encodeForm(request.body,request.charset==null?"UTF-8":request.charset);
        return request;
    }
    private static String text(JsonElement element,Evaluator evaluator) throws Exception {
        return interpolate(element.isJsonPrimitive()?element.getAsString():element.toString(),evaluator);
    }
    public static String interpolate(String text,Evaluator evaluator) throws Exception {
        Matcher matcher=TEMPLATE.matcher(text);StringBuffer out=new StringBuffer();
        while(matcher.find()) {
            Object value=evaluator.eval(matcher.group(1),null);
            String s=value==null?"":value instanceof Number && ((Number)value).doubleValue()==((Number)value).longValue()?Long.toString(((Number)value).longValue()):value.toString();
            matcher.appendReplacement(out,Matcher.quoteReplacement(s));
        }
        matcher.appendTail(out);return out.toString();
    }
    private static String expandScripts(String rule,Evaluator evaluator) throws Exception {
        if(rule.regionMatches(true,0,"@js:",0,4)) {Object value=evaluator.eval(rule.substring(4),rule);return value==null?"":value.toString();}
        StringBuilder out=new StringBuilder();int cursor=0;
        while(true) {
            int start=rule.toLowerCase(Locale.ROOT).indexOf("<js>",cursor);
            if(start<0){out.append(rule.substring(cursor));break;}
            out.append(rule.substring(cursor,start));int end=rule.toLowerCase(Locale.ROOT).indexOf("</js>",start+4);
            if(end<0)throw new IllegalArgumentException("书源缺少 </js>");
            Object value=evaluator.eval(rule.substring(start+4,end),out.toString());
            if(value!=null){out.setLength(0);out.append(value.toString());}
            cursor=end+5;
        }
        return out.toString();
    }
    private static int optionsStart(String text) {
        for(int i=0;i<text.length()-1;i++)if(text.charAt(i)==','){
            int j=i+1;while(j<text.length()&&Character.isWhitespace(text.charAt(j)))j++;
            if(j<text.length()&&text.charAt(j)=='{')return i;
        }
        return -1;
    }
    private static String encodeForm(String body,String charset)throws Exception {
        StringBuilder out=new StringBuilder();
        for(String part:body.split("&",-1)) {
            if(out.length()>0)out.append('&');int eq=part.indexOf('=');
            String key=eq<0?part:part.substring(0,eq),value=eq<0?"":part.substring(eq+1);
            out.append(encoded(key)?key:URLEncoder.encode(key,charset)).append('=');
            out.append(encoded(value)?value:URLEncoder.encode(value,charset));
        }
        return out.toString();
    }
    private static boolean encoded(String value){return Pattern.compile("%[0-9a-fA-F]{2}").matcher(value).find();}
}
