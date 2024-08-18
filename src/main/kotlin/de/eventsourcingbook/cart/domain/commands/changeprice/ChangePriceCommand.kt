package de.eventsourcingbook.cart.domain.commands.changeprice

import java.util.UUID

data class ChangePriceCommand(
    var productId: UUID,
    var newPrice: Double,
    var oldPrice: Double,
)
