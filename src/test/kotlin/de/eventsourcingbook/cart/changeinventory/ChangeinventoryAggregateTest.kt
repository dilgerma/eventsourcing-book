package de.eventsourcingbook.cart.changeinventory

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.InventoryAggregate
import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import de.eventsourcingbook.cart.events.InventoryChangedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChangeinventoryAggregateTest {

    private lateinit var fixture: FixtureConfiguration<InventoryAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(InventoryAggregate::class.java)
    }

    @Test
    fun `ChangeinventoryAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        // WHEN
        val command =
            ChangeInventoryCommand(
                inventory = RandomData.newInstance {},
                productId = RandomData.newInstance {}
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<InventoryChangedEvent> {
                this.inventory = command.inventory
                this.productId = command.productId
            }
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
