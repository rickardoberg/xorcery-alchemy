package dev.xorcery.alchemy.common.transmute;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.alchemy.jar.JarConfiguration;
import dev.xorcery.alchemy.jar.RecipeConfiguration;
import dev.xorcery.alchemy.jar.TransmuteJar;
import dev.xorcery.collections.Element;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service(name = "retry", metadata = "enabled=jars.enabled")
public class RetryTransmuteJar
        implements TransmuteJar {
    @Inject
    public RetryTransmuteJar() {
    }

    @Override
    public BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> newTransmute(JarConfiguration configuration, RecipeConfiguration recipeConfiguration) {
        Retry retry = RetrySpec.backoff(
                        configuration.getLong("maxAttempts").filter(v -> v != -1).orElse(Long.MAX_VALUE),
                        Duration.parse("PT" + configuration.getString("minBackoff").orElseThrow(Element.missing("minBackoff"))))
                .maxBackoff(Duration.parse("PT" + configuration.getString("maxBackoff").orElseThrow(Element.missing("maxBackoff"))))
                .jitter(configuration.getDouble("jitter").orElse(0.5))
                .multiplier(configuration.getDouble("multiplier").orElse(2D))
                .filter(retryFilter(configuration));
        return (flux, context) -> flux.retryWhen(retry);
    }

    protected Predicate<? super Throwable> retryFilter(JarConfiguration configuration) {
        List<String> includes = configuration.configuration().getListAs("includes", JsonNode::asText).orElse(Collections.emptyList());
        List<String> excludes = configuration.configuration().getListAs("excludes", JsonNode::asText).orElse(Collections.emptyList());
        Predicate<? super Throwable> retryPredicate = includes.isEmpty()
                ? t -> true
                : t -> throwableAndCauses(t).anyMatch(throwableOrCause -> includes.stream().anyMatch(name -> throwableOrCause.getClass().getName().equals(name) || throwableOrCause.getClass().getSimpleName().equals(name)));
        return excludes.isEmpty()
                ? retryPredicate
                : retryPredicate.and(t -> throwableAndCauses((Throwable) t)
                .noneMatch(throwableOrCause -> includes.stream().noneMatch(name -> throwableOrCause.getClass().getName().equals(name) || throwableOrCause.getClass().getSimpleName().equals(name))));
    }

    protected Stream<Throwable> throwableAndCauses(Throwable throwable) {
        return Stream.iterate(throwable, t -> t.getCause() != null, Throwable::getCause);
    }
}
