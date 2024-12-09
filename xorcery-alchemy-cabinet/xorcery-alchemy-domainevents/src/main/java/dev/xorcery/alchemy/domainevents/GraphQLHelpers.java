package dev.xorcery.alchemy.domainevents;

import graphql.language.*;
import graphql.schema.*;

import java.util.*;

public interface GraphQLHelpers {
    static GraphQLType inner(GraphQLType type) {
        if (type instanceof GraphQLNonNull nn) {
            return inner(nn.getWrappedType());
        } else if (type instanceof GraphQLList list) {
            return inner(list.getWrappedType());
        } else {
            return type;
        }
    }

    static boolean isEntity(GraphQLType nodeType) {
        return (GraphQLHelpers.inner(nodeType) instanceof GraphQLObjectType ot && ot.getInterfaces().stream().anyMatch(t -> t.getName().equals("Entity"))) ||
                GraphQLHelpers.inner(nodeType) instanceof GraphQLInterfaceType it && (it.getName().equals("Entity") || it.getInterfaces().stream().anyMatch(t -> t.getName().equals("Entity")));
    }

    static boolean isList(GraphQLType type) {
        return type instanceof GraphQLNonNull nnt ? isList(nnt.getWrappedType()) : type instanceof GraphQLList;
    }

    static <T> T getMandatoryArgument(GraphQLDirective dir, String argumentName, T defaultValue) {
        return Optional.ofNullable(getArgument(dir, argumentName, defaultValue))
                .orElseThrow(() -> new IllegalStateException(argumentName + " is required for @" + dir.getName()));
    }

    static <T> T getArgument(GraphQLDirective directive, String argumentName, T defaultValue) {
        GraphQLArgument argument = directive.getArgument(argumentName);
        return argument.getArgumentValue().isSet() && argument.getArgumentValue().getValue() != null
                ? (T) toJavaValue(argument.getArgumentValue().getValue())
                : argument.getArgumentDefaultValue().isSet() ? (T) toJavaValue(argument.getArgumentDefaultValue().getValue())
                : defaultValue;
    }

    static Object toJavaValue(Object value) {
        return value instanceof Value ? toJavaValue((Value) value) : value;
    }

    static Object toJavaValue(Value value) {
        // TODO Complete this
        if (value instanceof StringValue sv) {
            return sv.getValue();
        } else if (value instanceof EnumValue ev) {
            return ev.getName();
        } else if (value instanceof NullValue nv) {
            return null;
        } else if (value instanceof BooleanValue bv) {
            return bv.isValue();
        } else if (value instanceof FloatValue fv) {
            return fv.getValue().doubleValue();
        } else if (value instanceof IntValue iv) {
            return iv.getValue().longValueExact();
        } else if (value instanceof VariableReference vr) {
            return vr;
        } else if (value instanceof ArrayValue av) {
            return toList(av);
        } else if (value instanceof ObjectValue ov) {
            return toMap(ov);
        } else
            throw new IllegalStateException("Unhandled value " + value);
    }

    static Map<String, Object> toMap(ObjectValue objectValue) {
        Map<String, Object> map = new HashMap<>();
        for (ObjectField objectField : objectValue.getObjectFields()) {
            Object value = toJavaValue(objectField.getValue());
            if (value != null) {
                map.put(objectField.getName(), value);
            }
        }
        return map;
    }

    static List<Object> toList(ArrayValue arrayValue) {
        List<Object> list = new ArrayList<>();
        for (Value value : arrayValue.getValues()) {
            Object val = toJavaValue(value);
            if (val != null)
                list.add(val);
        }
        return list;
    }
}
