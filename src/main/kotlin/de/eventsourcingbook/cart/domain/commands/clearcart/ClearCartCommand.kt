package de.eventsourcingbook.cart.domain.commands.clearcart

import de.eventsourcingbook.cart.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ClearCartCommand(@TargetAggregateIdentifier override var aggregateId: UUID) : Command
