package dev.xorcery.alchemy.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.JarException;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.ContextView;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

public class ScriptFlux
        implements BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> {
    private final Logger logger;
    private final ScriptExecutor subscribe;
    private final ScriptExecutor next;
    private final ScriptExecutor error;
    private final ScriptExecutor complete;

    private final ScriptEngine engine;
    private final JarConfiguration configuration;
    private final RecipeConfiguration recipeConfiguration;

    public ScriptFlux(ScriptEngine engine, JarConfiguration configuration, RecipeConfiguration recipeConfiguration, Logger logger) {
        this.logger = logger;
        this.subscribe = configuration.getString("subscribe").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
        this.next = configuration.getString("next").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
        this.error = configuration.getString("error").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
        this.complete = configuration.getString("complete").map(script -> ScriptExecutor.getScriptExecutor(engine, script)).orElse(null);
        this.engine = engine;
        this.configuration = configuration;
        this.recipeConfiguration = recipeConfiguration;
    }

    @Override
    public Publisher<MetadataJsonNode<JsonNode>> apply(Flux<MetadataJsonNode<JsonNode>> flux, ContextView contextView) {
        ByteArrayOutputStream out = new ByteArrayOutputStreamWithoutNewLine();
        engine.getContext().setWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        return Flux.push(sink -> new ScriptPusher(flux, out).push(sink), FluxSink.OverflowStrategy.BUFFER);
    }

    class ScriptPusher
            extends BaseSubscriber<MetadataJsonNode<JsonNode>> {
        private final Flux<MetadataJsonNode<JsonNode>> flux;
        private final ByteArrayOutputStream out;

        private FluxSink<MetadataJsonNode<JsonNode>> sink;

        public ScriptPusher(Flux<MetadataJsonNode<JsonNode>> flux, ByteArrayOutputStream out) {
            this.flux = flux;
            this.out = out;
        }

        void push(FluxSink<MetadataJsonNode<JsonNode>> sink) {
            this.sink = sink;
            flux.contextWrite(sink.contextView()).subscribe(this);
            sink.onDispose(this::cancel);
            sink.onRequest(this::request);
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            try {
                Bindings bindings = engine.createBindings();
                if (subscribe != null) {
                    subscribe.call(bindings);
                }
            } catch (Throwable e) {
                sink.error(new JarException(configuration, recipeConfiguration, e));
            }
            if (out.size() > 0) {
                logger.info(out.toString(StandardCharsets.UTF_8));
                out.reset();
            }
            super.hookOnSubscribe(subscription);
        }

        @Override
        protected void hookOnNext(MetadataJsonNode<JsonNode> item) {
            if (next != null) {

                try {
                    Bindings bindings = engine.createBindings();
                    ObjectNode itemJson = JsonNodeFactory.instance.objectNode();
                    itemJson.set("metadata", item.metadata().json());
                    itemJson.set("data", item.data());
                    bindings.put("item", new JsonNodeJSObject(itemJson));
                    bindings.put("sink", new JavaScriptFluxSink(sink));
                    next.call(bindings);
                } catch (Throwable e) {
                    sink.error(new JarException(configuration, recipeConfiguration, e));
                }
                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }
            } else {
                sink.next(item);
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            if (error != null) {
                try {
                    Bindings bindings = engine.createBindings();
                    bindings.put("error", new JsonNodeJSObject(JsonNodeFactory.instance.textNode(throwable.getMessage())));
                    bindings.put("sink", new JavaScriptFluxSink(sink));
                    error.call(bindings);
                } catch (Throwable e) {
                    sink.error(new JarException(configuration, recipeConfiguration, e));
                }
                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }
            } else {
                sink.error(throwable);
            }
        }

        @Override
        protected void hookOnComplete() {
            if (complete != null)
            {
                try {
                    Bindings bindings = engine.createBindings();
                    bindings.put("sink", new JavaScriptFluxSink(sink));
                    complete.call(bindings);

                    sink.complete();
                } catch (Throwable e) {
                    sink.error(new JarException(configuration, recipeConfiguration, e));
                }
                if (out.size() > 0) {
                    logger.info(out.toString(StandardCharsets.UTF_8));
                    out.reset();
                }
            } else {
                sink.complete();
            }
        }
    }
}
