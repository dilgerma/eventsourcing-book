package de.nebulit.domain

import de.nebulit.common.CommandException
import de.nebulit.common.NoArg
import de.nebulit.domain.commands.registeraccount.RegisterAccountCommand
import de.nebulit.domain.commands.reservemail.ReserveEMailCommand
import de.nebulit.events.AccountRegisteredEvent
import de.nebulit.events.EMailReservedEvent
import java.util.UUID
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class ReserveEmailAggregate {
  @AggregateIdentifier lateinit var email: String

  var reserved: Boolean = false

  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  @CommandHandler
  fun handle(command: ReserveEMailCommand) {
    if (reserved) {
      throw CommandException("email already registered")
    }
    AggregateLifecycle.createNew(
        AccountAggregate::class.java,
        { AccountAggregate(RegisterAccountCommand(command.aggregateId, command.email)) },
    )
    AggregateLifecycle.apply(EMailReservedEvent(command.aggregateId, command.email))
  }

  @EventSourcingHandler
  fun handle(event: EMailReservedEvent) {
    reserved = true
    this.email = event.email
  }
}

@NoArg
@Aggregate
class AccountAggregate {
  @AggregateIdentifier lateinit var aggregateId: UUID

  constructor(command: RegisterAccountCommand) {
    AggregateLifecycle.apply(AccountRegisteredEvent(command.aggregateId, command.email))
  }

  @EventSourcingHandler
  fun handle(event: AccountRegisteredEvent) {
    this.aggregateId = event.aggregateId
  }
}
