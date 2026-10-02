package com.kunfei.bookshelf.help;
import androidx.annotation.Keep;
import com.kunfei.bookshelf.bean.*;


public final class JsBindings {
    public static java.util.Map<String,Object> create(Object bridge,Object result,String baseUrl,BaseBookBean book,Source3Store.Source source){
        java.util.Map<String,Object> bindings=new java.util.HashMap<String,Object>();bindings.put("java",bridge);bindings.put("result",result);bindings.put("baseUrl",baseUrl);bindings.put("source",source);bindings.put("cookie",new SourceCookie());bindings.put("book",new Book(book));if(source!=null)bindings.put("__jsLib",source.jsLib);return bindings;
    }
    @Keep public static class Book {
        public String name="",author="",bookUrl="",origin="",durChapterTitle="";public int type=0;
        Book(BaseBookBean bean){if(bean==null)return;bookUrl=bean.getNoteUrl();origin=bean.getTag();
            if(bean instanceof BookShelfBean){BookShelfBean shelf=(BookShelfBean)bean;name=shelf.getBookInfoBean().getName();author=shelf.getBookInfoBean().getAuthor();durChapterTitle=shelf.getDurChapterName();}
            else if(bean instanceof SearchBookBean){SearchBookBean search=(SearchBookBean)bean;name=search.getName();author=search.getAuthor();}
        }
    }
}
