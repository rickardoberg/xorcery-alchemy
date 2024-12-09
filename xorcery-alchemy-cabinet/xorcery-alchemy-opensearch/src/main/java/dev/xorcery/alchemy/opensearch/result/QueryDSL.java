package dev.xorcery.alchemy.opensearch.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

public interface QueryDSL {

    ObjectMapper mapper = new JsonMapper().findAndRegisterModules();

    static ObjectNode term(String name, Object value)
    {
        return instance.objectNode().set("term",
                instance.objectNode().set(name, instance.objectNode().set("value", mapper.valueToTree(value))));
    }

    static ObjectNode match_phrase(String name, Object value)
    {
        return instance.objectNode().set("match_phrase",
                instance.objectNode().set(name, mapper.valueToTree(value)));
    }
}
