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
import org.axonframework.modelling.command.AggregateInvocationException
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import java.util.UUID

/** cannot republish an already published cart */
@ExtendWith(MockKExtension::class)
class RepublishcartAggregateTest {
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
    fun `RepublishcartAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("722754da-4f24-4db9-8125-be0227e9724c")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("722754da-4f24-4db9-8125-be0227e9724c")
                itemId = UUID.randomUUID()
                productId = UUID.randomUUID()
            },
        )
        events.add(
            RandomData.newInstance<CartSubmittedEvent> {
                aggregateId = UUID.fromString("722754da-4f24-4db9-8125-be0227e9724c")
            },
        )
        events.add(
            RandomData.newInstance<CartPublishedEvent> {
                aggregateId = UUID.fromString("722754da-4f24-4db9-8125-be0227e9724c")
            },
        )

        // WHEN
        val command =
            PublishCartCommand(
                aggregateId = UUID.fromString("722754da-4f24-4db9-8125-be0227e9724c"),
                orderedProducts = RandomData.newInstance {},
                totalPrice = RandomData.newInstance {},
            )

        // THEN
        fixture
            .given(events)
            .`when`(command)
            .expectException(AggregateInvocationException::class.java)
    }
}
