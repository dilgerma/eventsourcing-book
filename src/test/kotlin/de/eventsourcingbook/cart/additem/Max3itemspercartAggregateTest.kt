package de.eventsourcingbook.cart.additem

import de.eventsourcingbook.cart.application.DeviceFingerPrintCalculator
import de.eventsourcingbook.cart.common.CommandException
import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Max3itemspercartAggregateTest {
    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @RelaxedMockK
    private lateinit var deviceFingerPrintCalculator: DeviceFingerPrintCalculator

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
        fixture.registerInjectableResource(deviceFingerPrintCalculator)
    }

    @Test
    fun `Max3itemspercartAggregateTest`() {
        // GIVEN
        val events = mutableListOf<Event>()

        events.add(
            RandomData.newInstance<CartCreatedEvent> {
                aggregateId = UUID.fromString("f9602c89-46e3-4202-ac78-efd39339321b")
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("f9602c89-46e3-4202-ac78-efd39339321b")
                description = RandomData.newInstance {}
                image = RandomData.newInstance {}
                price = RandomData.newInstance {}
                itemId = RandomData.newInstance {}
                productId = RandomData.newInstance {}
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("f9602c89-46e3-4202-ac78-efd39339321b")
                description = RandomData.newInstance {}
                image = RandomData.newInstance {}
                price = RandomData.newInstance {}
                itemId = RandomData.newInstance {}
                productId = RandomData.newInstance {}
            },
        )
        events.add(
            RandomData.newInstance<ItemAddedEvent> {
                aggregateId = UUID.fromString("f9602c89-46e3-4202-ac78-efd39339321b")
                description = RandomData.newInstance {}
                image = RandomData.newInstance {}
                price = RandomData.newInstance {}
                itemId = RandomData.newInstance {}
                productId = RandomData.newInstance {}
            },
        )

        // WHEN
        val command =
            AddItemCommand(
                aggregateId = UUID.fromString("f9602c89-46e3-4202-ac78-efd39339321b"),
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

        fixture.given(events).`when`(command).expectException(CommandException::class.java)
    }
}
