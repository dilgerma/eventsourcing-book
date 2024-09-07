package de.eventsourcingbook.cart.publishcart

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.publishcart.PublishCartCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.CartPublicationFailedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import de.eventsourcingbook.cart.publishcart.internal.PublishCartCommandHandler
import de.eventsourcingbook.cart.publishcart.internal.contract.ExternalPublishedCart
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import java.util.UUID

/** cannot publish unsubmitted cart */
@ExtendWith(MockKExtension::class)
class PublishunsubmittedcartAggregateTest {
    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @RelaxedMockK private lateinit var kafkaTemplate: KafkaTemplate<String, ExternalPublishedCart>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
        fixture.registerAnnotatedCommandHandler(
            PublishCartCommandHandler(kafkaTemplate, fixture.repository),
        )
    }

    @Test
    fun `PublishunsubmittedcartAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("e86dc125-b86b-40fb-9d8c-0cd319761cc5")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("e86dc125-b86b-40fb-9d8c-0cd319761cc5")
                itemId = UUID.randomUUID()
                productId = UUID.randomUUID()
            },
        )

        // WHEN
        val command =
            PublishCartCommand(
                aggregateId = UUID.fromString("e86dc125-b86b-40fb-9d8c-0cd319761cc5"),
                orderedProducts = RandomData.newInstance {},
                totalPrice = RandomData.newInstance {},
            )

        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<CartPublicationFailedEvent> { this.aggregateId = command.aggregateId },
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
