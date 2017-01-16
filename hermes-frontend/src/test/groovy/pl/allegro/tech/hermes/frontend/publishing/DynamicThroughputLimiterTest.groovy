package pl.allegro.tech.hermes.frontend.publishing

import com.codahale.metrics.Metered
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.frontend.publishing.handlers.DynamicThroughputLimiter
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter
import spock.lang.Specification

class DynamicThroughputLimiterTest extends Specification {

    def max = 10_000
    def threshold = 8_000
    def desired = 6_000

    double globalRate = 0.0
    def globalMeter = [getOneMinuteRate: {globalRate}] as Metered

    def localMeter = Mock(Metered)

    def topicName = new TopicName("group", "name")

    DynamicThroughputLimiter limiter = new DynamicThroughputLimiter(max, threshold, desired, globalMeter)

    def "global allow"() {
        given:
        globalMeter.oneMinuteRate >> 10
        localMeter.oneMinuteRate >> 1000

        expect:
        limiter.check(topicName, localMeter)
    }

    def "global block"() {
        given:
        globalRate = 12_000
        localMeter.oneMinuteRate >> 1000

        when:
        limiter.check(topicName, localMeter)

        then:
        thrown ThroughputLimiter.GlobalQuotaViolationException
    }

    def "single abuser"() {
        given:
        registerTopic("group1", "name1", 1000)
        registerTopic("group2", "name2", 1000)

        double abuserRate = 7000
        def abuserMeter = [getOneMinuteRate: {abuserRate}] as Metered
        def topic3 = registerTopic("group3", "name3", abuserMeter)

        limiter.run()

        when:
        abuserRate = 2001
        limiter.check(topic3, abuserMeter)

        then:
        thrown ThroughputLimiter.QuotaViolationException
    }

    private Metered meter(double rate) {
        Mock(Metered) { getOneMinuteRate() >> rate }
    }

    private TopicName registerTopic(String group, String name, double rate) {
        registerTopic(group, name, meter(rate))
    }

    private TopicName registerTopic(String group, String name, Metered rate) {
        def topic = new TopicName(group, name)
        globalRate += rate.getOneMinuteRate()
        limiter.check(topic, rate)
        topic
    }
}
