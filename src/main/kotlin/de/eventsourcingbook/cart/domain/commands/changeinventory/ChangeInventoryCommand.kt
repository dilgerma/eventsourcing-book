package de.eventsourcingbook.cart.domain.commands.changeinventory

import java.util.*
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ChangeInventoryCommand(
    @TargetAggregateIdentifier var productId: UUID,
    var inventory: Int,
)
