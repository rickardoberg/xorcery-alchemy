package com.exoreaction.xorcery.alchemy.script;

import javax.script.*;

interface ScriptExecutor {

    static ScriptExecutor getScriptExecutor(ScriptEngine engine, String script)
            throws ScriptException
    {
        if (engine instanceof Compilable compilable)
        {
            CompiledScript compiledScript = compilable.compile(script);
            return compiledScript::eval;
        } else
        {
            return bindings -> engine.eval(script, bindings);
        }
    }

    void call(Bindings bindings)
            throws ScriptException;
}
