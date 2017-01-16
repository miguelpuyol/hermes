package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.TopicName;

public class FixedThroughputLimiter implements ThroughputLimiter {
    private long limit;

    public FixedThroughputLimiter(long limit) {
        this.limit = limit;
    }

    @Override
    public void check(TopicName topic, Metered throughput) {
        long rate = (long) Math.floor(throughput.getOneMinuteRate());
        if (rate > limit) {
            throw new QuotaViolationException(rate, limit);
        }
    }

}
