package de.eventsourcingbook.cart.domain

import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import de.eventsourcingbook.cart.events.InventoryChangedEvent
import java.util.UUID
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class InventoryAggregate {

  @AggregateIdentifier lateinit var aggregateId: UUID

  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  @CommandHandler
  fun handle(command: ChangeInventoryCommand) {
    AggregateLifecycle.apply(InventoryChangedEvent(command.productId, command.inventory))
  }

  @EventSourcingHandler
  fun on(inventoryChangedEvent: InventoryChangedEvent) {
    this.aggregateId = inventoryChangedEvent.productId
  }
}
