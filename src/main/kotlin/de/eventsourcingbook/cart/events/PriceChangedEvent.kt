package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.*

data class PriceChangedEvent(
    var productId: UUID,
    var newPrice: Double,
    var oldPrice: Double,
) : Event
