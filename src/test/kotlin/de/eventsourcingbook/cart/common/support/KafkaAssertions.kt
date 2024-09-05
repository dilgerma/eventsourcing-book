package de.eventsourcingbook.cart.common.support

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.stereotype.Component
import java.time.Duration

data class TopicRecord(
    val topic: String,
    val key: String?,
    val payload: Any,
)

@Component
class KafkaAssertions(
    val kafkaConsumerFactory: ConsumerFactory<Any, Any>,
) {
    fun assertRecord(
        topic: String,
        predicate: (event: ConsumerRecord<Any, Any>) -> Boolean,
    ) {
        val consumer = kafkaConsumerFactory.createConsumer()
        consumer.subscribe(listOf(topic))
        consumer.seekToBeginning(listOf())
        val records = consumer.poll(Duration.ofSeconds(5))

        assertThat(
            records,
        ).anyMatch(predicate)

        consumer.close()
    }
}
