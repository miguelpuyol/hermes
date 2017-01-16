package pl.allegro.tech.hermes.frontend.publishing.handlers;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;

import javax.inject.Inject;

public class ThroughputLimiterFactory implements Factory<ThroughputLimiter> {
    private ConfigFactory configs;
    private HermesMetrics hermesMetrics;

    private enum ThroughputLimiterType {UNLIMITED, FIXED, DYNAMIC}

    @Inject
    public ThroughputLimiterFactory(ConfigFactory configs, HermesMetrics hermesMetrics) {
        this.configs = configs;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public ThroughputLimiter provide() {
        switch (ThroughputLimiterType.valueOf(configs.getStringProperty(Configs.FRONTEND_THROUGHPUT_TYPE).toUpperCase())) {
            case UNLIMITED:
                return (a, b) -> {};
            case FIXED:
                return new FixedThroughputLimiter(configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_MAX));
            case DYNAMIC:
                return new DynamicThroughputLimiter(
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_MAX),
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_THRESHOLD),
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_DESIRED),
                        hermesMetrics.meter(Meters.THROUGHPUT_BYTES)
                        );
            default:
                throw new IllegalArgumentException("Unknown throughput limiter type.");
        }
    }

    @Override
    public void dispose(ThroughputLimiter instance) {

    }
}
