package de.eventsourcingbook.cart.domain.commands.archiveitem

import de.eventsourcingbook.cart.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ArchiveItemCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var productId: UUID
) : Command
