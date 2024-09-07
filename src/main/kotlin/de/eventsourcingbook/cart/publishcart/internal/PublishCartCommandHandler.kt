package de.eventsourcingbook.cart.publishcart.internal

import de.eventsourcingbook.cart.domain.CartAggregate
import de.eventsourcingbook.cart.domain.commands.publishcart.PublishCartCommand
import de.eventsourcingbook.cart.publishcart.internal.contract.ExternalPublishedCart
import de.eventsourcingbook.cart.publishcart.internal.contract.OrderedProduct
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.Repository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PublishCartCommandHandler(
    val kafkaTemplate: KafkaTemplate<String, in ExternalPublishedCart>,
    val repository: Repository<CartAggregate>,
) {
    @Transactional
    @CommandHandler
    fun handle(command: PublishCartCommand) {
        repository.load(command.aggregateId.toString())?.execute {
            kafkaTemplate.send(
                "published_carts",
                ExternalPublishedCart(
                    command.aggregateId,
                    command.totalPrice,
                    command.orderedProducts.map { item ->
                        OrderedProduct(item.productId, item.price)
                    },
                ),
            )
            it.publish()
            if (true) {
                throw IllegalStateException("error processing")
            }
        }
    }
}
