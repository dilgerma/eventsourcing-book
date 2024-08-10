package de.eventsourcingbook.cart.common

import java.util.*

/** Result of a command execution that allows to give Feedback to the client to update. */
data class CommandResult(val identifier: UUID, val aggregateSequence: Long)
