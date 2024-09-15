package de.nebulit.domain

import de.nebulit.domain.commands.automation.ExpireTodoCommand
import de.nebulit.domain.commands.command.AddTodoCommand
import de.nebulit.events.TodoAddedEvent
import de.nebulit.events.TodoExpiredEvent
import java.util.UUID
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class TODOAggregate {
  @AggregateIdentifier lateinit var aggregateId: UUID

  var expired: Boolean = false

  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  @CommandHandler
  fun handle(command: AddTodoCommand) {
    AggregateLifecycle.apply(
        TodoAddedEvent(command.aggregateId, command.todo, command.expirationDate))
  }

  @EventSourcingHandler
  fun on(event: TodoAddedEvent) {
    this.aggregateId = event.aggregateId
  }

  @CommandHandler
  fun handle(command: ExpireTodoCommand) {
    AggregateLifecycle.apply(TodoExpiredEvent(command.aggregateId, command.todo))
  }

  @EventSourcingHandler
  fun on(event: TodoExpiredEvent) {
    this.expired = true
  }
}
