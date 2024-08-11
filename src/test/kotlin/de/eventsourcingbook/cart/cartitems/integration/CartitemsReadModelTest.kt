package de.eventsourcingbook.cart.cartitems.integration

import de.eventsourcingbook.cart.cartitems.CartItemsReadModel
import de.eventsourcingbook.cart.cartitems.CartItemsReadModelQuery
import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CartitemsReadModelTest : BaseIntegrationTest() {

  @Autowired private lateinit var commandGateway: CommandGateway

  @Autowired private lateinit var queryGateway: QueryGateway

  @Test
  fun `CartitemsReadModelTest`() {

    val aggregateId = UUID.randomUUID()

    var addItemCommand = RandomData.newInstance<AddItemCommand> { this.aggregateId = aggregateId }

    var addItemCommandResult = commandGateway.sendAndWait<CommandResult>(addItemCommand)

    awaitUntilAssserted {
      var readModel =
          queryGateway.query(CartItemsReadModelQuery(aggregateId), CartItemsReadModel::class.java)
      // TODO add assertions
      assertThat(readModel.get().data).isNotEmpty
    }
  }
}
