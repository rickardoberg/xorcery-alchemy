package dev.xorcery.alchemy.domainevents;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.util.Resources;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

@Service
public class GraphQLSchemaFactory
    implements Factory<GraphQLSchema>
{

    private final GraphQLConfiguration graphQLConfiguration;

    @Inject
    public GraphQLSchemaFactory(Configuration configuration) {
        graphQLConfiguration = GraphQLConfiguration.get(configuration);
    }

    @Override
    public GraphQLSchema provide() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = null;
        for (String schemaResource : graphQLConfiguration.getSchemas()) {
            URL url = Resources.getResource(schemaResource).orElseThrow(()->new IllegalArgumentException("Cannot find schema file:"+schemaResource));
            try (InputStream schemaFile = url.openStream()) {
                TypeDefinitionRegistry fileTypeDefinitionRegistry = schemaParser.parse(schemaFile);
                typeDefinitionRegistry = typeDefinitionRegistry == null
                        ? fileTypeDefinitionRegistry
                        : fileTypeDefinitionRegistry.merge(typeDefinitionRegistry);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not parse GraphQL schema file:"+url, e);
            }
        }

        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
        GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(
                typeDefinitionRegistry,
                runtimeWiringBuilder.codeRegistry(codeRegistryBuilder).build()
        );
        return graphQLSchema;
    }

    @Override
    public void dispose(GraphQLSchema instance) {

    }
}
