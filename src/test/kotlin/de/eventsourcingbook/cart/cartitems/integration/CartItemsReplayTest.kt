package de.eventsourcingbook.cart.cartitems.integration

import com.ninjasquad.springmockk.MockkBean
import de.eventsourcingbook.cart.application.DeviceFingerPrintCalculator
import de.eventsourcingbook.cart.cartitems.CartItemsReadModel
import de.eventsourcingbook.cart.cartitems.CartItemsReadModelQuery
import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.RandomData
import de.eventsourcingbook.cart.common.support.StreamAssertions
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import de.eventsourcingbook.cart.support.ResetStream
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.command.Repository
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/** cart containing the product should react if the price of the product changes */
class CartItemsReplayTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var queryGateway: QueryGateway

    @Autowired
    private lateinit var streamAssertions: StreamAssertions

    @Autowired
    private lateinit var repository: Repository<CartAggregate>

    @Autowired
    private lateinit var resetStream: ResetStream

    @MockkBean
    private lateinit var deviceFingerPrintCalculator: DeviceFingerPrintCalculator

    @Test
    fun `replays events correctly`() {
        val aggregateId = UUID.randomUUID()
        val fingerPrint = "fingerPrint"
        every { deviceFingerPrintCalculator.calculateDeviceFingerPrint() } returns fingerPrint

        commandGateway.sendAndWait<AddItemCommand>(
            RandomData.newInstance<AddItemCommand> {
                this.aggregateId = aggregateId
            },
        )

        awaitUntilAssserted {
            var readModel =
                queryGateway.query(
                    CartItemsReadModelQuery(aggregateId),
                    CartItemsReadModel::class.java,
                )
            assertThat(readModel.get().data).isNotEmpty
        }
        resetStream.reset("cart-items")

        awaitUntilAssserted {
            var readModel =
                queryGateway.query(
                    CartItemsReadModelQuery(aggregateId),
                    CartItemsReadModel::class.java,
                )
            assertThat(readModel.get().data).isNotEmpty
            assertThat(
                readModel
                    .get()
                    .data
                    .first()
                    .fingerPrint,
            ).isEqualTo(fingerPrint)
        }
    }
}
