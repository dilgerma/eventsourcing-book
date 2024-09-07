package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class CartPublicationFailedEvent(
    var aggregateId: UUID,
) : Event
