package com.exoreaction.xorcery.alchemy.script;

import org.openjdk.nashorn.api.scripting.AbstractJSObject;

import java.util.function.Function;

public class JavaScriptMethodCall
    extends AbstractJSObject
{
    private final Function<Object[], Object> invoker;

    public JavaScriptMethodCall(Function<Object[], Object> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object call(Object thiz, Object... args) {
        return invoker.apply(args);
    }
}
