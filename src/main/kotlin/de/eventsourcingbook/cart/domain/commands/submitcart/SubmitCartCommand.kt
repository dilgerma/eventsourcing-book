package de.eventsourcingbook.cart.domain.commands.submitcart

import de.eventsourcingbook.cart.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class SubmitCartCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
) : Command
