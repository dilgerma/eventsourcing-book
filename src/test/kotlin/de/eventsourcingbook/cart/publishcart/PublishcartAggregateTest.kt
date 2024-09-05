package de.eventsourcingbook.cart.publishcart

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.publishcart.PublishCartCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.CartPublishedEvent
import de.eventsourcingbook.cart.events.CartSubmittedEvent
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

/**  */
@ExtendWith(MockKExtension::class)
class PublishcartAggregateTest {
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
    fun `PublishcartAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("5dbfbd52-4e02-49f0-856a-9ce5e2c78265")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("5dbfbd52-4e02-49f0-856a-9ce5e2c78265")
                itemId = UUID.randomUUID()
                productId = UUID.randomUUID()
            },
        )
        events.add(
            RandomData.newInstance<CartSubmittedEvent> {
                aggregateId = UUID.fromString("5dbfbd52-4e02-49f0-856a-9ce5e2c78265")
                orderedProducts = RandomData.newInstance {}
            },
        )

        // WHEN
        val command =
            PublishCartCommand(
                aggregateId = UUID.fromString("5dbfbd52-4e02-49f0-856a-9ce5e2c78265"),
                orderedProducts = RandomData.newInstance {},
                totalPrice = RandomData.newInstance {},
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<CartPublishedEvent> { this.aggregateId = command.aggregateId },
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
