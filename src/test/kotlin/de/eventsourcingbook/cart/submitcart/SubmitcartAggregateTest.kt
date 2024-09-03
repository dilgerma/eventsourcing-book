package de.eventsourcingbook.cart.submitcart

import de.eventsourcingbook.cart.common.Command
import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.common.CommandException
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import de.eventsourcingbook.cart.events.ItemAddedEvent;
import de.eventsourcingbook.cart.domain.commands.submitcart.SubmitCartCommand;
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.CartSubmittedEvent
import de.eventsourcingbook.cart.events.OrderedProduct
import org.junit.jupiter.api.Order
import java.util.UUID

/**

 */
class SubmitcartAggregateTest {

    private lateinit var fixture: FixtureConfiguration<CartAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(CartAggregate::class.java)
    }

    @Test
    fun `submitcart aggregate test`() {
        //GIVEN
        val events = mutableListOf<Event>()

        val productId1 = UUID.randomUUID()
        val price1 = 9.99
        val productId2 = UUID.randomUUID()
        val price2 = 10.99
        events.add(RandomData.newInstance<CartCreatedEvent> {
            aggregateId = UUID.fromString("2519f0c5-040b-426c-9565-82634c1ede2f")
        })
        events.add(RandomData.newInstance<ItemAddedEvent> {
            aggregateId = UUID.fromString("2519f0c5-040b-426c-9565-82634c1ede2f")
            this.price = price1
            this.itemId = UUID.randomUUID()
            this.productId = productId1
        })
        events.add(RandomData.newInstance<ItemAddedEvent> {
            aggregateId = UUID.fromString("2519f0c5-040b-426c-9565-82634c1ede2f")
            this.price = price2
            this.itemId = UUID.randomUUID()
            this.productId = productId2
        })

        //WHEN
        val command = SubmitCartCommand(
            aggregateId = UUID.fromString("2519f0c5-040b-426c-9565-82634c1ede2f"),
        )

        //THEN
        val expectedEvents = mutableListOf<Event>()

        expectedEvents.add(RandomData.newInstance<CartSubmittedEvent> {
            this.aggregateId = command.aggregateId
            this.orderedProducts = listOf(OrderedProduct(productId1, price1), OrderedProduct(productId2, price2))
            totalPrice = 20.98
        })


        fixture.given(events)
            .`when`(command)
            .expectSuccessfulHandlerExecution()
            .expectEvents(*expectedEvents.toTypedArray())
    }

    @Test
    fun `submitcart twice is not allowed`() {
        //GIVEN
        val events = mutableListOf<Event>()
        val aggregateId = UUID.randomUUID()

        val productId1 = UUID.randomUUID()
        val price1 = 9.99
        events.add(RandomData.newInstance<CartCreatedEvent> {
            this.aggregateId = aggregateId
        })
        events.add(RandomData.newInstance<ItemAddedEvent> {
            this.aggregateId = aggregateId
            this.price = price1
            this.itemId = UUID.randomUUID()
            this.productId = productId1
        })
        events.add(RandomData.newInstance<CartSubmittedEvent> {
            this.aggregateId = aggregateId
        })

        //WHEN
        val command = SubmitCartCommand(
            aggregateId = aggregateId
        )
        fixture.given(events)
            .`when`(command)
            .expectException(CommandException::class.java)
    }

}
