package de.eventsourcingbook.cart.removeitem

import de.eventsourcingbook.cart.common.CommandException
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

class RemoveitemwhichwasalreadyremovedAggregateTest {

    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `RemoveitemwhichwasalreadyremovedAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()
        val itemId = UUID.randomUUID()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("a58f246d-86dc-4145-a609-23fce5215cd5")
            },
        )
        events.add(RandomData.newInstance<ItemAddedEvent> { this.itemId = itemId })
        events.add(
            RandomData.newInstance<ItemRemovedEvent> {
                aggregateId = UUID.fromString("a58f246d-86dc-4145-a609-23fce5215cd5")
                this.itemId = itemId
            },
        )

        // WHEN
        val command =
            RemoveItemCommand(
                aggregateId = UUID.fromString("a58f246d-86dc-4145-a609-23fce5215cd5"),
                itemId = itemId,
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        fixture.given(events).`when`(command).expectException(CommandException::class.java)
    }
}
