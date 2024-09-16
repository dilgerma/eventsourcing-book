package de.nebulit.domain.commands

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class AddTodoCommand(
    @TargetAggregateIdentifier
    val aggregateId: UUID,
    val todo: String,
)
