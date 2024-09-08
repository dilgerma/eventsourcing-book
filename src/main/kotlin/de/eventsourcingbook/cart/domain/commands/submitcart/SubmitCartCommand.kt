package de.eventsourcingbook.cart.domain.commands.submitcart

import de.eventsourcingbook.cart.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class SubmitCartCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
) : Command
