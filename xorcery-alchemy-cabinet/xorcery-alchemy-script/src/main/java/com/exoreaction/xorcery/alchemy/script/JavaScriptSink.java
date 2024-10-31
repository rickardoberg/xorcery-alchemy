package com.exoreaction.xorcery.alchemy.script;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import reactor.core.publisher.SynchronousSink;

public class JavaScriptSink
    extends AbstractJSObject
{
    private final SynchronousSink<MetadataJsonNode<JsonNode>> sink;

    public JavaScriptSink(SynchronousSink<MetadataJsonNode<JsonNode>> sink) {
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
                        sink.next(new MetadataJsonNode<>(new Metadata((ObjectNode)itemJson.get("metadata")), itemJson.get("data")));
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
                    sink.error(new IllegalStateException(msg));
                }
                return null;
            });

            default -> null;
        };
    }
}
