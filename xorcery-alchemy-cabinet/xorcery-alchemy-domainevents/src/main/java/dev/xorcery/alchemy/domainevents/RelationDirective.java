package dev.xorcery.alchemy.domainevents;

import graphql.schema.GraphQLFieldDefinition;

import java.util.Optional;

public record RelationDirective(String name, String direction) {
    public static final String RELATION = "relation";
    public static final String NAME = "name";
    public static final String DIRECTION = "direction";

    public static Optional<RelationDirective> get(GraphQLFieldDefinition fieldDefinition)
    {
        return Optional.ofNullable(fieldDefinition.getDirective(RelationDirective.RELATION)).map(dir ->
        {
            String name = getMandatoryArgument(dir, RelationDirective.NAME, fieldDefinition.getName());
            String direction = getMandatoryArgument(dir, RelationDirective.DIRECTION, Direction.OUTGOING.name());
            return new RelationDirective(name, parseDirection(direction));
        });
    }

    public static String parseDirection(String name)
    {
        return switch (name.toUpperCase())
        {
            case "BOTH" -> "BOTH";
            case "IN" -> "INCOMING";
            case "INCOMING" -> "INCOMING";
            default -> "OUTGOING";
        };
    }
}
