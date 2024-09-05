package de.eventsourcingbook.cart.domain.commands.additem

import de.eventsourcingbook.cart.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class AddItemCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var description: String,
    var image: String,
    var price: Double,
    var totalPrice: Double,
    var itemId: UUID,
    var productId: UUID,
) : Command
