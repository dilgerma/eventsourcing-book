package de.eventsourcingbook.cart.domain.commands.archiveitem

import org.axonframework.modelling.command.TargetAggregateIdentifier
import de.eventsourcingbook.cart.common.Command
import java.util.UUID;


data class ArchiveItemCommand(
    @TargetAggregateIdentifier override var aggregateId:UUID,
	var productId:UUID
): Command
