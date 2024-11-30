package dev.xorcery.alchemy.jslt.transmute.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.schibsted.spt.data.jslt.Function;

public class PowerFunction
    implements Function
{
    public PowerFunction() {
    }

    @Override
    public String getName() {
        return "power";
    }

    @Override
    public int getMinArguments() {
        return 2;
    }

    @Override
    public int getMaxArguments() {
        return 2;
    }

    @Override
    public JsonNode call(JsonNode input, JsonNode[] params) {
        int base = params[0].asInt();
        int power = params[1].asInt();

        int result = 1;
        for (int ix = 0; ix < power; ix++)
            result = result * base;

        return new IntNode(result);
    }
}
