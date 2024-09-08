package de.eventsourcingbook.cart.clearcart

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.clearcart.ClearCartCommand
import de.eventsourcingbook.cart.events.CartClearedEvent
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ClearcartAggregateTest {
    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `ClearcartAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("e94adda2-d1fe-4fd6-9f57-9663ba4a831a")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("e94adda2-d1fe-4fd6-9f57-9663ba4a831a")
            },
        )

        // WHEN
        val command =
            ClearCartCommand(aggregateId = UUID.fromString("e94adda2-d1fe-4fd6-9f57-9663ba4a831a"))

        // THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(
            RandomData.newInstance<CartClearedEvent> { this.aggregateId = command.aggregateId },
        )

        fixture
            .given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }
}
