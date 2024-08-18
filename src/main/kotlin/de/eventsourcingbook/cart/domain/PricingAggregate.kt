package de.eventsourcingbook.cart.domain

import de.eventsourcingbook.cart.domain.commands.changeprice.ChangePriceCommand
import de.eventsourcingbook.cart.events.PriceChangedEvent
import java.util.UUID
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class PricingAggregate {

  @AggregateIdentifier lateinit var aggregateId: UUID

  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  @CommandHandler
  fun handle(command: ChangePriceCommand) {
    AggregateLifecycle.apply(
        PriceChangedEvent(command.productId, command.newPrice, command.oldPrice))
  }

  @EventSourcingHandler
  fun on(event: PriceChangedEvent) {
    this.aggregateId = event.productId
  }
}
