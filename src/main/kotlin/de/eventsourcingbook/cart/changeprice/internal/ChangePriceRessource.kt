package de.eventsourcingbook.cart.changeprice.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.changeprice.ChangePriceCommand
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.concurrent.CompletableFuture

data class ChangePricePayload(var newPrice: Double, var oldPrice: Double, var productId: UUID)

@RestController
class ChangePriceRessource(private var commandGateway: CommandGateway) {

    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/changeprice")
    fun processDebugCommand(
        @RequestParam newPrice: Double,
        @RequestParam oldPrice: Double,
        @RequestParam productId: UUID,
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(ChangePriceCommand(productId, newPrice, oldPrice))
    }

    @CrossOrigin
    @PostMapping("/changeprice/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: ChangePricePayload,
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(
            ChangePriceCommand(
                newPrice = payload.newPrice,
                oldPrice = payload.oldPrice,
                productId = payload.productId,
            ),
        )
    }
}
