package de.nebulit.domain

import de.nebulit.common.NoArg
import de.nebulit.domain.commands.AddTodoCommand
import de.nebulit.events.TodoAdded
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate
import java.util.UUID

@NoArg
@Aggregate(snapshotTriggerDefinition = "todoAggregateSnapshotTriggerDefinition")
class TodoListAggregate {
    @AggregateIdentifier lateinit var aggregateId: UUID

    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    @CommandHandler
    fun handle(command: AddTodoCommand) {
        AggregateLifecycle.apply(TodoAdded(command.aggregateId, command.todo))
    }

    @EventSourcingHandler
    fun handle(event: TodoAdded) {
        this.aggregateId = event.aggregateId
    }
}
