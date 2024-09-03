package de.eventsourcingbook.cart.domain.commands.submitcart

import org.axonframework.modelling.command.TargetAggregateIdentifier
import de.eventsourcingbook.cart.common.Command
import de.eventsourcingbook.cart.events.OrderedProduct
import java.util.UUID;
import kotlin.collections.List;


data class SubmitCartCommand(
    @TargetAggregateIdentifier override var aggregateId:UUID,
): Command
