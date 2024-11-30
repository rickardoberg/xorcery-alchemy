package dev.xorcery.alchemy.script;

import javax.script.*;

public interface ScriptExecutor {

    static ScriptExecutor getScriptExecutor(ScriptEngine engine, String script)
            throws IllegalArgumentException
    {
        if (engine instanceof Compilable compilable)
        {
            try {
                CompiledScript compiledScript = compilable.compile(script);
                return compiledScript::eval;
            } catch (ScriptException e) {
                throw new IllegalArgumentException(e);
            }
        } else
        {
            return bindings -> engine.eval(script, bindings);
        }
    }

    void call(Bindings bindings)
            throws ScriptException;
}
