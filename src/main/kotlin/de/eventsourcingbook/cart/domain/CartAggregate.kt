package de.eventsourcingbook.cart.domain

import de.eventsourcingbook.cart.common.CommandException
import de.eventsourcingbook.cart.domain.commands.additem.AddItemCommand
import de.eventsourcingbook.cart.events.CartCreatedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import java.util.UUID
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class CartAggregate {

  @AggregateIdentifier var aggregateId: UUID? = null

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  fun handle(command: AddItemCommand) {
    if (aggregateId == null) {
      AggregateLifecycle.apply(CartCreatedEvent(aggregateId = command.aggregateId))
    }
    if (cartItems.size >= 3) {
      throw CommandException("can only add 3 items")
    }
    AggregateLifecycle.apply(
        ItemAddedEvent(
            aggregateId = command.aggregateId,
            description = command.description,
            image = command.image,
            price = command.price,
            productId = command.productId,
            itemId = command.itemId))
  }

  @EventSourcingHandler
  fun on(event: CartCreatedEvent) {
    this.aggregateId = event.aggregateId
  }

  val cartItems = mutableListOf<UUID>()

  @EventSourcingHandler
  fun on(event: ItemAddedEvent) {
    this.cartItems.add(event.itemId)
  }
}
