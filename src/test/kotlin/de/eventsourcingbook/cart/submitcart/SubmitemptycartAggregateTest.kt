package de.eventsourcingbook.cart.submitcart

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.submitcart.SubmitCartCommand
import java.util.UUID
import org.axonframework.modelling.command.AggregateNotFoundException
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**  */
class SubmitemptycartAggregateTest {

    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `submitemptycart aggregate test`() {
        // GIVEN
        val events = mutableListOf<Event>()

        // WHEN
        val command =
            SubmitCartCommand(
                aggregateId = UUID.fromString("81dc9829-f6ff-4a80-a8c2-b70e021c0330"),
            )

        // THEN
        val expectedEvents = mutableListOf<Event>()

        fixture
            .given(events)
            .`when`(command)
            .expectException(AggregateNotFoundException::class.java)
    }
}
