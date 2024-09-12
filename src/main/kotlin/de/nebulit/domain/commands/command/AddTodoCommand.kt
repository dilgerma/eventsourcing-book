package de.nebulit.domain.commands.command

import de.nebulit.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class AddTodoCommand(
    @TargetAggregateIdentifier override var aggregateId: UUID,
    var todo: String
) : Command