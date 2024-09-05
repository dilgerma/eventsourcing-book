package de.eventsourcingbook.cart.changeinventory.internal

import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryChangedKafkaConsumer(var commandGateway: CommandGateway) {

    @KafkaListener(topics = ["inventories"])
    fun handle(externalInventoryChangedEvent: ExternalInventoryChangedEvent) {
        commandGateway.send<ChangeInventoryCommand>(
            ChangeInventoryCommand(
                externalInventoryChangedEvent.productId,
                externalInventoryChangedEvent.inventory,
            ),
        )
    }
}
