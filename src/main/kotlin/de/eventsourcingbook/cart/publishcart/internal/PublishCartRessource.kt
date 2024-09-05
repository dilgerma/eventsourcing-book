package de.eventsourcingbook.cart.publishcart.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.publishcart.PublishCartCommand
import de.eventsourcingbook.cart.events.OrderedProduct
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.collections.List

data class PublishCartPayload(
    var aggregateId: UUID,
    var orderedProducts: List<OrderedProduct>,
    var totalPrice: Double,
)

@RestController
class PublishCartRessource(
    private var commandGateway: CommandGateway,
) {
    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/publishcart")
    fun processDebugCommand(
        @RequestParam aggregateId: UUID,
        @RequestParam orderedProducts: List<OrderedProduct>,
        @RequestParam totalPrice: Double,
    ): CompletableFuture<CommandResult> =
        commandGateway.send(
            PublishCartCommand(
                aggregateId,
                orderedProducts,
                totalPrice,
            ),
        )

    @CrossOrigin
    @PostMapping("/publishcart/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: PublishCartPayload,
    ): CompletableFuture<CommandResult> =
        commandGateway.send(
            PublishCartCommand(
                aggregateId = payload.aggregateId,
                orderedProducts = payload.orderedProducts,
                totalPrice = payload.totalPrice,
            ),
        )
}
