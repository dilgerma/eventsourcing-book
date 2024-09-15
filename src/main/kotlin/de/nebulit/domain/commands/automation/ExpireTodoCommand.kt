package de.nebulit.domain.commands.automation

import de.nebulit.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ExpireTodoCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var todo: String,
) : Command
