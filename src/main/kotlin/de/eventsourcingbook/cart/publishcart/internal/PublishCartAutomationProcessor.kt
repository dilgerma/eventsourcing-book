package de.eventsourcingbook.cart.publishcart.internal

import de.eventsourcingbook.cart.common.Processor
import de.eventsourcingbook.cart.domain.commands.publishcart.PublishCartCommand
import de.eventsourcingbook.cart.events.CartSubmittedEvent
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DisallowReplay
import org.axonframework.eventhandling.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@DisallowReplay
@Component
@ProcessingGroup("publish_cart")
class PublishCartAutomationProcessor : Processor {
    var logger = KotlinLogging.logger {}

    @Autowired lateinit var commandGateway: CommandGateway

    @EventHandler
    fun on(event: CartSubmittedEvent) {
        commandGateway.sendAndWait<Any>(
            PublishCartCommand(event.aggregateId, event.orderedProducts, event.totalPrice),
        )
    }
}
