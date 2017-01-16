package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DynamicThroughputLimiter implements ThroughputLimiter, Runnable {
    private final long max;
    private final long threshold;
    private final long desired;
    private final Metered globalThroughputMeter;

    private ConcurrentHashMap<TopicName, Throughput> users = new ConcurrentHashMap<>(200);

    public DynamicThroughputLimiter(long max, long threshold, long desired, Metered globalThroughput) {
        this.max = max;
        this.threshold = threshold;
        this.desired = desired;
        this.globalThroughputMeter = globalThroughput;
    }

    @Override
    public void check(TopicName topic, Metered rate) {
        if (!users.containsKey(topic)) {
            users.putIfAbsent(topic, new Throughput(rate, max));
        }

        Throughput throughput = users.get(topic);
        long value = throughput.getOneMinuteRate();

        if (value > throughput.max) {
            throw new QuotaViolationException(value, throughput.max);
        }

        if (globalThroughputMeter.getOneMinuteRate() > max) {
            throw new GlobalQuotaViolationException();
        }
    }

    @Override
    public void start() {
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if (globalThroughputMeter.getOneMinuteRate() > threshold) {
            calibrateLimits();
        }
    }

    private void calibrateLimits() {
        long total = users.reduceValuesToLong(10, Throughput::getOneMinuteRate, 0, ((left, right) -> left + right));
        long mean = total / users.size();
        long desiredMean = desired / users.size();
        users.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getOneMinuteRate() > mean)
                .forEach(entry -> entry.getValue().max = desiredMean);
    }

    static class Throughput {
        Metered current;
        volatile long max;

        public Throughput(Metered current, long max) {
            this.current = current;
            this.max = max;
        }

        private long getOneMinuteRate() {
            return (long) Math.floor(current.getOneMinuteRate());
        }

    }
}
