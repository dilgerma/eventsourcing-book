package de.eventsourcingbook.cart.changeprice

import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.domain.PricingAggregate
import de.eventsourcingbook.cart.common.CommandException
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import de.eventsourcingbook.cart.domain.commands.changeprice.ChangePriceCommand;
import de.eventsourcingbook.cart.events.PriceChangedEvent


class ChangepriceAggregateTest {

    private lateinit var fixture: FixtureConfiguration<PricingAggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(PricingAggregate::class.java)
    }

    @Test
    fun `ChangepriceAggregateTest`() {
      //GIVEN
      val events = mutableListOf<Event>()
      
     
    

      //WHEN
      val command = ChangePriceCommand(
 					newPrice = RandomData.newInstance {  },
	oldPrice = RandomData.newInstance {  },
	productId = RandomData.newInstance {  }
            )

      //THEN
      val expectedEvents = mutableListOf<Event>()
      
               expectedEvents.add(RandomData.newInstance<PriceChangedEvent> { 
               	this.newPrice = command.newPrice
	this.oldPrice = command.oldPrice
	this.productId = command.productId
                })   
                

      fixture.given(events)
        .`when`(command)
        .expectSuccessfulHandlerExecution()
                .expectEvents(*expectedEvents.toTypedArray())
    }

}
