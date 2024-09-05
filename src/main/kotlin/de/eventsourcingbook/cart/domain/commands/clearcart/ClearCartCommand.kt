package de.eventsourcingbook.cart.domain.commands.clearcart

import de.eventsourcingbook.cart.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class ClearCartCommand(@TargetAggregateIdentifier override var aggregateId: UUID) : Command
