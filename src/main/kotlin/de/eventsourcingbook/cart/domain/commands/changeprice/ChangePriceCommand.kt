package de.eventsourcingbook.cart.domain.commands.changeprice

import org.axonframework.modelling.command.TargetAggregateIdentifier
import de.eventsourcingbook.cart.common.Command
import java.util.UUID


data class ChangePriceCommand(
	var productId:UUID,
    var newPrice:Double,
	var oldPrice:Double,
)
