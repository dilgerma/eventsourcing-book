package de.eventsourcingbook.cart.inventories.integration

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import de.eventsourcingbook.cart.inventories.InventoriesReadModel
import de.eventsourcingbook.cart.inventories.InventoriesReadModelQuery
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class InventoriesReadModelTest : BaseIntegrationTest() {

    @Autowired private lateinit var commandGateway: CommandGateway

    @Autowired private lateinit var queryGateway: QueryGateway

    @Test
    fun `InventoriesReadModelTest`() {
        val aggregateId = UUID.randomUUID()

        var changeInventoryCommand =
            RandomData.newInstance<ChangeInventoryCommand> { this.productId = aggregateId }

        var changeInventoryCommandResult =
            commandGateway.sendAndWait<CommandResult>(changeInventoryCommand)

        awaitUntilAssserted {
            var readModel =
                queryGateway.query(
                    InventoriesReadModelQuery(aggregateId),
                    InventoriesReadModel::class.java,
                )
            // TODO add assertions
            assertThat(readModel.get()).isNotNull
        }
    }
}
