package de.eventsourcingbook.cart.domain.commands.removeitem

import de.eventsourcingbook.cart.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class RemoveItemCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var itemId: UUID
) : Command
