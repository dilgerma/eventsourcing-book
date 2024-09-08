package de.eventsourcingbook.cart.cartwithproducts.integration

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/** Cart should contain the exact product id of the added item */
class CartwithproductsReadModelTest : BaseIntegrationTest() {
    @Autowired private lateinit var commandGateway: CommandGateway

    @Autowired private lateinit var queryGateway: QueryGateway

    @Test
    fun `CartwithproductsReadModelTest`() {
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

        awaitUntilAssserted {
            var readModel =
                queryGateway.query(
                    CartsWithProductsReadModelQuery(productId),
                    CartsWithProductsReadModel::class.java,
                )
            assertThat(readModel.get().data).hasSize(1)
            assertThat(readModel.get().data).first().matches {
                it.aggregateId == aggregateId && it.productId == productId
            }
        }
    }
}
