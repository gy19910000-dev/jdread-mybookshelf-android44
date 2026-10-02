package com.kunfei.bookshelf.help;
import androidx.annotation.Keep;
import com.kunfei.bookshelf.DbHelper;
import com.kunfei.bookshelf.bean.CookieBean;
@Keep public class SourceCookie {
    private String key(String url){Source3Store.Source source=Source3Store.find(url);return source==null?url:source.getKey();}
    public String getCookie(String url){CookieBean bean=DbHelper.getDaoSession().getCookieBeanDao().load(key(url));return bean==null||bean.getCookie()==null?"":bean.getCookie();}
    public String getCookie(String url,String name){for(String part:getCookie(url).split(";")){int eq=part.indexOf('=');if(eq>=0&&part.substring(0,eq).trim().equals(name))return part.substring(eq+1).trim();}return "";}
    public void setCookie(String url,String cookie){DbHelper.getDaoSession().getCookieBeanDao().insertOrReplace(new CookieBean(key(url),cookie));}
    public void removeCookie(String url){DbHelper.getDaoSession().getCookieBeanDao().deleteByKey(key(url));}
    public String get(String url){return getCookie(url);}public void set(String url,String cookie){setCookie(url,cookie);}public void remove(String url){removeCookie(url);}
}
