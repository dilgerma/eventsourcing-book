package de.eventsourcingbook.cart.domain.commands.publishcart

import de.eventsourcingbook.cart.common.Command
import de.eventsourcingbook.cart.events.OrderedProduct
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID
import kotlin.collections.List

data class PublishCartCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var orderedProducts: List<OrderedProduct>,
    var totalPrice: Double,
) : Command
