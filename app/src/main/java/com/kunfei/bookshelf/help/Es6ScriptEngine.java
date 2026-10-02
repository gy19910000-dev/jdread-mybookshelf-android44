package com.kunfei.bookshelf.help;

import org.mozilla.javascript.*;
import java.io.Reader;
import java.io.IOException;
import java.util.Map;

/** Each evaluation has its own scope; concurrent source searches must not share bindings. */
public final class Es6ScriptEngine {
    private final ContextFactory factory = new ContextFactory() {
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1); // Dalvik cannot execute generated JVM bytecode.
            cx.setMaximumInterpreterStackDepth(2048);
            return cx;
        }
    };

    public Object eval(String script) { return eval(script, java.util.Collections.emptyMap()); }
    public Object eval(String script, Map<String,Object> bindings) {
        Context cx = factory.enterContext();
        try {
            WrapFactory wrappers=new WrapFactory(){
                public Scriptable wrapAsJavaObject(Context context,Scriptable scope,Object value,Class<?> type){
                    // A desugared Jsoup Elements exposes Java 8 bridge methods that Dalvik
                    // cannot reflect. Sources expect JS arrays, so expose lists as arrays.
                    if(value instanceof java.util.List){
                        java.util.List<?> list=(java.util.List<?>)value;Object[] items=new Object[list.size()];
                        for(int i=0;i<items.length;i++)items[i]=Context.javaToJS(list.get(i),scope);
                        return context.newArray(scope,items);
                    }
                    try{return super.wrapAsJavaObject(context,scope,value,type);}
                    catch(LinkageError error){throw new IllegalStateException("JS interface unavailable: "+value.getClass().getName(),error);}
                }
            };
            wrappers.setJavaPrimitiveWrap(false);cx.setWrapFactory(wrappers);
            Scriptable scope = new ImporterTopLevel(cx);
            bind(scope, bindings);
            Object library = bindings.get("__jsLib");
            if (library instanceof String && !((String) library).trim().isEmpty())
                cx.evaluateString(scope, (String) library, "source-library", 1, null);
            Object result = cx.evaluateString(scope, script, "source-rule", 1, null);
            while (result instanceof Wrapper) result = ((Wrapper) result).unwrap();
            if (result == Undefined.instance || result == Scriptable.NOT_FOUND) return null;
            if(result instanceof NativeArray){
                java.util.List<Object> list=new java.util.ArrayList<>();
                for(Object item:(NativeArray)result){while(item instanceof Wrapper)item=((Wrapper)item).unwrap();list.add(item);}
                return list;
            }
            return result instanceof CharSequence ? result.toString() : result;
        } finally { Context.exit(); }
    }

    private void bind(Scriptable scope, Map<String,Object> bindings) {
        for (Map.Entry<String,Object> entry : bindings.entrySet())
            ScriptableObject.putProperty(scope, entry.getKey(), Context.javaToJS(entry.getValue(), scope));
    }
    public Object eval(Reader reader, Map<String,Object> bindings) throws IOException {
        try { StringBuilder text=new StringBuilder(); char[] buf=new char[4096]; int n;
            while((n=reader.read(buf))!=-1) text.append(buf,0,n); return eval(text.toString(),bindings);
        } catch(IOException e) { throw e; }
    }
}
