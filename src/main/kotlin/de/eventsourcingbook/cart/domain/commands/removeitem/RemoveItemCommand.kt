package de.eventsourcingbook.cart.domain.commands.removeitem

import de.eventsourcingbook.cart.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class RemoveItemCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var itemId: UUID,
) : Command
