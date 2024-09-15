package de.eventsourcingbook.cart.domain.commands.changeprice

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class ChangePriceCommand(
    @TargetAggregateIdentifier
    var productId: UUID,
    var newPrice: Double,
    var oldPrice: Double,
)
