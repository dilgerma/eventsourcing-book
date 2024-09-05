package de.eventsourcingbook.cart.changeprice.internal

import java.math.BigDecimal
import java.util.*

data class ExternalPriceChangedEvent(
    var productId: UUID,
    var price: BigDecimal,
    val oldPrice: BigDecimal,
)
