package dev.xorcery.alchemy.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.JSObject;
import reactor.core.publisher.FluxSink;

import javax.script.ScriptException;

public class JavaScriptFluxSink
    extends AbstractJSObject
{
    private final FluxSink<MetadataJsonNode<JsonNode>> sink;

    public JavaScriptFluxSink(FluxSink<MetadataJsonNode<JsonNode>> sink) {
        this.sink = sink;
    }

    @Override
    public Object getMember(String name) {
        return switch (name)
        {
            case "next" -> new JavaScriptMethodCall(args -> {
                if (args[0] instanceof JsonNodeJSObject jsObject)
                {
                    if (jsObject.getJsonNode() instanceof ObjectNode itemJson)
                    {
                        sink.next(new MetadataJsonNode<>(new Metadata(itemJson.get("metadata").deepCopy()), itemJson.get("data").deepCopy()));
                    }
                } else if (args[0] instanceof JSObject jsObject)
                {
                    if (JsonNodeJSObject.unwrap(jsObject) instanceof ObjectNode itemJson)
                    {
                        sink.next(new MetadataJsonNode<>(new Metadata(itemJson.get("metadata").deepCopy()), itemJson.get("data").deepCopy()));
                    }
                }
                return null;
            });
            case "complete" -> new JavaScriptMethodCall(args -> {
                sink.complete();
                return null;
            });
            case "error" -> new JavaScriptMethodCall(args -> {
                if (args[0] instanceof String msg)
                {
                    sink.error(new ScriptException(msg));
                }
                return null;
            });

            default -> null;
        };
    }
}
