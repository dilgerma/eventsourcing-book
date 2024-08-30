package de.eventsourcingbook.cart.archiveitem.integration

import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.common.support.StreamAssertions
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import de.eventsourcingbook.cart.domain.commands.changeprice.ChangePriceCommand
import de.eventsourcingbook.cart.events.ItemArchivedEvent
import java.util.*
import org.axonframework.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * This Test is flaky, meaning it will sometimes fail and sometimes succeed. The reason is, that it
 * relies on an eventually consistent read model. there is a timing issue between a command issued
 * and all data processed in parallel.
 */
/** cart containing the product should react if the price of the product changes */
class ArchiveItemProcessorTest : BaseIntegrationTest() {

  @Autowired private lateinit var commandGateway: CommandGateway

  @Autowired private lateinit var streamAssertions: StreamAssertions

  @Test
  fun `archive item processor test`() {

    val aggregateId = UUID.randomUUID()
    val productId = UUID.randomUUID()
    val itemId = UUID.randomUUID()

    var addItemCommand =
        RandomData.newInstance<AddItemCommand> {
          this.aggregateId = aggregateId
          this.productId = productId
          this.itemId = itemId
        }

    commandGateway.sendAndWait<Any>(addItemCommand)

    // uncomment this line to make the test green. Eventual Consistency in action
    // Thread.sleep(1000)

    var changePriceCommand =
        RandomData.newInstance<ChangePriceCommand> { this.productId = productId }

    commandGateway.sendAndWait<Any>(changePriceCommand)

    awaitUntilAssserted {
      streamAssertions.assertEvent(aggregateId.toString()) {
        it is ItemArchivedEvent && it.aggregateId == aggregateId && it.itemId == itemId
      }
    }
  }
}
