package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import static java.lang.String.format;

public interface ThroughputLimiter {
    void check(TopicName topic, Metered throughput);
    default void start() {}
    default void stop() {}


    public static class QuotaViolationException extends RuntimeException {
        QuotaViolationException(long current, long limit) {
            super(format("Current throughput exceeded limit [current:%s, limit:%s].",
                    current, limit));
        }
    }

    public static class GlobalQuotaViolationException extends RuntimeException {
        GlobalQuotaViolationException() {
            super("Global throughput exceeded. Sorry for the inconvenience.");
        }
    }
}
