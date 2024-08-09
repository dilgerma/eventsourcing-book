package de.eventsourcingbook.cart.common

import java.util.*

interface Command {
    var aggregateId: UUID
}
