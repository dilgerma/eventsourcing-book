package de.eventsourcingbook.cart.additem

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AdditemAggregateTest {

    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `AdditemAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        // WHEN
        val command =
            AddItemCommand(
                aggregateId = UUID.fromString("41bc3104-8f83-43ef-960b-9931e855e97d"),
                description = RandomData.newInstance {},
                image = RandomData.newInstance {},
                price = RandomData.newInstance {},
                totalPrice = RandomData.newInstance {},
                itemId = RandomData.newInstance {},
                productId = RandomData.newInstance {},
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<CartCreatedEvent> { this.aggregateId = command.aggregateId },
        )

        expectedEvents.add(
            RandomData.newInstance<ItemAddedEvent> {
                this.aggregateId = command.aggregateId
                this.description = command.description
                this.image = command.image
                this.price = command.price
                this.itemId = command.itemId
                this.productId = command.productId
            },
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
