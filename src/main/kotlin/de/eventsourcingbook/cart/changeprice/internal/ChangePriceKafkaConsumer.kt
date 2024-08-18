package de.eventsourcingbook.cart.changeprice.internal

import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import de.eventsourcingbook.cart.domain.commands.changeprice.ChangePriceCommand
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ChangePriceKafkaConsumer(var commandGateway: CommandGateway) {

  @KafkaListener(topics = ["price_changes"])
  fun handle(event: ExternalPriceChangedEvent) {
    commandGateway.send<ChangeInventoryCommand>(
        ChangePriceCommand(event.productId, event.price.toDouble(), event.oldPrice.toDouble()))
  }
}
