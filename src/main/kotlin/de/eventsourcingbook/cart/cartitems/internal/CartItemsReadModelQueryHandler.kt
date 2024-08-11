package de.eventsourcingbook.cart.cartitems.internal

import de.eventsourcingbook.cart.cartitems.CartItemsReadModel
import de.eventsourcingbook.cart.cartitems.CartItemsReadModelQuery
import de.eventsourcingbook.cart.common.Event
import de.eventsourcingbook.cart.common.QueryHandler
import org.axonframework.eventsourcing.eventstore.EventStore
import org.springframework.stereotype.Component

@Component
class CartItemsReadModelQueryHandler(val eventStore: EventStore) {

  @org.axonframework.queryhandling.QueryHandler
  fun handleQuery(query: CartItemsReadModelQuery): CartItemsReadModel {
    val events =
        eventStore
            .readEvents(query.aggregateId.toString())
            .asStream()
            .filter { it.payload is Event }
            .map { it.payload as Event }
            .toList()

    return CartItemsReadModel().applyEvent(events)
  }
}
