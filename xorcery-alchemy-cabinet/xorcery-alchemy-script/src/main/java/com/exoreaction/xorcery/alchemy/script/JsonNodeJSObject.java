package com.exoreaction.xorcery.alchemy.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.JSObject;

import java.util.*;

public class JsonNodeJSObject
        extends AbstractJSObject {
    private final JsonNode jsonNode;

    public JsonNodeJSObject(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    @Override
    public Object getMember(String name) {
        return wrap(jsonNode.get(name));
    }

    @Override
    public void setMember(String name, Object value) {
        if (jsonNode instanceof ObjectNode objectNode) {
            if (value instanceof JSObject objectValue) {
                objectNode.set(name, unwrap(objectValue));
            } else if (value instanceof String typedValue) {
                objectNode.put(name, typedValue);
            } else if (value instanceof Double typedValue) {
                objectNode.put(name, typedValue);
            }else if (value instanceof Float typedValue) {
                objectNode.put(name, typedValue);
            }else if (value instanceof Long typedValue) {
                objectNode.put(name, typedValue);
            }else if (value instanceof Integer typedValue) {
                objectNode.put(name, typedValue);
            }else if (value instanceof Short typedValue) {
                objectNode.put(name, typedValue);
            }
        }
    }

    @Override
    public boolean hasMember(String name) {
        return jsonNode instanceof ObjectNode objectNode ? objectNode.has(name) : false;
    }

    @Override
    public boolean isArray() {
        return jsonNode.isArray();
    }

    @Override
    public boolean hasSlot(int slot) {
        return jsonNode instanceof ArrayNode arrayNode && slot < arrayNode.size();
    }

    @Override
    public Object getSlot(int index) {
        return jsonNode instanceof ArrayNode arrayNode && index < arrayNode.size() ? wrap(arrayNode.get(index)) : null;
    }

    @Override
    public Set<String> keySet() {
        if (jsonNode instanceof ObjectNode objectNode) {
            Set<String> keys = new HashSet<>();
            objectNode.fieldNames().forEachRemaining(keys::add);
            return keys;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<Object> values() {
        if (jsonNode instanceof ObjectNode objectNode) {
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
                values.add(wrap(entry.getValue()));
            }
            return values;
        } else {
            return Collections.emptySet();
        }
    }

    private Object wrap(Object object) {
        if (object instanceof ContainerNode<?> json)
            return new JsonNodeJSObject(json);
        else if (object instanceof TextNode textNode) {
            return textNode.asText();
        } else if (object instanceof NumericNode numberNode) {
            if (numberNode.isIntegralNumber())
                return numberNode.longValue();
            else
                return numberNode.doubleValue();
        } else if (object instanceof BooleanNode booleanNode) {
            return booleanNode.asBoolean();
        } else
            return object;
    }

    private ContainerNode<?> unwrap(JSObject jsObject) {
        if (jsObject.isArray()) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            int index = 0;
            while (jsObject.hasSlot(index)) {
                Object value = jsObject.getSlot(index);
                if (value instanceof JSObject objectValue) {
                    arrayNode.add(unwrap(objectValue));
                } else {
                    arrayNode.addPOJO(value);
                }
                index++;
            }
            return arrayNode;
        } else {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            for (String key : jsObject.keySet()) {
                Object value = jsObject.getMember(key);
                if (value instanceof JSObject objectValue) {
                    objectNode.set(key, unwrap(objectValue));
                } else {
                    objectNode.putPOJO(key, value);
                }
            }
            return objectNode;
        }
    }

    @Override
    public Object getDefaultValue(Class<?> hint) throws UnsupportedOperationException {
        return jsonNode.toString();
    }
}
