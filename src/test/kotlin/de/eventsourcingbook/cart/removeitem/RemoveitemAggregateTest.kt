package de.eventsourcingbook.cart.removeitem

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.removeitem.RemoveItemCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import de.eventsourcingbook.cart.events.ItemRemovedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RemoveitemAggregateTest {
    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `RemoveitemAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()
        val itemId = UUID.randomUUID()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("4bcfd5e3-51d9-4256-bc16-4d1b71ed46b8")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("4bcfd5e3-51d9-4256-bc16-4d1b71ed46b8")
                this.itemId = itemId
            },
        )

        // WHEN
        val command =
            RemoveItemCommand(
                aggregateId = UUID.fromString("4bcfd5e3-51d9-4256-bc16-4d1b71ed46b8"),
                itemId = itemId,
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<ItemRemovedEvent> {
                this.aggregateId = command.aggregateId
                this.itemId = command.itemId
            },
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
