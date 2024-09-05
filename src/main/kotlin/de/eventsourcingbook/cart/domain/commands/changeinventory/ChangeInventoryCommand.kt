package de.eventsourcingbook.cart.domain.commands.changeinventory

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class ChangeInventoryCommand(
    @TargetAggregateIdentifier var productId: UUID,
    var inventory: Int,
)
