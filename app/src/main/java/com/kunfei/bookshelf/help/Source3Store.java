package com.kunfei.bookshelf.help;
import android.content.SharedPreferences;
import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.kunfei.bookshelf.MApplication;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Keep modern source metadata without changing the reading-history database schema. */
public final class Source3Store {
    private static final Map<String,Source> cache=new ConcurrentHashMap<>();
    private static SharedPreferences prefs(){return MApplication.getInstance()==null?null:MApplication.getInstance().getSharedPreferences("source3_rules",0);}
    private static String normalize(String key){return key==null?null:key.replaceAll("/+$","");}
    public static void save(String key,String json){Source source=new Gson().fromJson(json,Source.class);key=normalize(key);source.bookSourceUrl=key;com.google.gson.JsonObject raw=new Gson().fromJson(json,com.google.gson.JsonObject.class);raw.addProperty("bookSourceUrl",key);json=raw.toString();cache.put(key,source);SharedPreferences p=prefs();if(p!=null)p.edit().putString("rule:"+key,json).apply();}
    public static Source find(String url){
        if(url==null)return null;url=normalize(url);Source direct=cache.get(url);if(direct!=null)return direct;
        SharedPreferences p=prefs();if(p!=null){String json=p.getString("rule:"+url,null);if(json!=null){direct=new Gson().fromJson(json,Source.class);cache.put(url,direct);return direct;}
            for(Map.Entry<String,?> entry:p.getAll().entrySet())if(entry.getKey().startsWith("rule:")){String key=entry.getKey().substring(5);if(url.equals(key)||url.startsWith(key+"/")){Source s=cache.get(key);if(s==null){s=new Gson().fromJson(entry.getValue().toString(),Source.class);cache.put(key,s);}return s;}}
        }
        for(Map.Entry<String,Source> entry:cache.entrySet())if(url.startsWith(entry.getKey()+"/"))return entry.getValue();return null;
    }
    @Keep public static class Source {
        public String bookSourceUrl,bookSourceName,bookSourceGroup,bookSourceComment,loginUrl,header,jsLib;
        public int bookSourceType;
        private transient Map<String,String> values;
        private synchronized Map<String,String> values(){if(values==null)values=new ConcurrentHashMap<>();return values;}
        public String getKey(){return bookSourceUrl;}
        public String getVariable(){SharedPreferences p=prefs();return p==null?values().get("__variable"):p.getString("variable:"+bookSourceUrl,"");}
        public String setVariable(Object value){String text=String.valueOf(value);values().put("__variable",text);SharedPreferences p=prefs();if(p!=null)p.edit().putString("variable:"+bookSourceUrl,text).apply();return text;}
        public Map<String,String> getLoginInfoMap(){SharedPreferences p=prefs();String json=p==null?"{}":p.getString("login:"+bookSourceUrl,"{}");try{return new Gson().fromJson(json,Map.class);}catch(Exception e){return new HashMap<>();}}
        public String put(String key,String value){values().put(key,value==null?"":value);return value;}
        public String get(String key){return values().get(key);}
    }
}
