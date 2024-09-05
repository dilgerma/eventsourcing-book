package de.eventsourcingbook.cart.changeinventory.integration

import de.eventsourcingbook.cart.common.support.BaseIntegrationTest
import de.eventsourcingbook.cart.common.support.StreamAssertions
import de.eventsourcingbook.cart.common.support.awaitUntilAssserted
import de.eventsourcingbook.cart.events.InventoryChangedEvent
import org.axonframework.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class ChangeinventoryProcessorTest : BaseIntegrationTest() {
    @Autowired private lateinit var commandGateway: CommandGateway

    @Autowired private lateinit var streamAssertions: StreamAssertions

    @Test
    fun `ChangeinventoryProcessorTest`() {
        val aggregateId = UUID.randomUUID()

        awaitUntilAssserted {
            streamAssertions.assertEvent(aggregateId.toString()) { it is InventoryChangedEvent }
        }
    }
}
