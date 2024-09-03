package de.eventsourcingbook.cart.submitcart.internal

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import de.eventsourcingbook.cart.domain.commands.submitcart.SubmitCartCommand
import de.eventsourcingbook.cart.common.CommandResult

import java.util.UUID;
import kotlin.collections.List;

import java.util.concurrent.CompletableFuture


data class SubmitCartPayload(
    var aggregateId: UUID,
)

@RestController
class SubmitCartRessource(private var commandGateway: CommandGateway) {

    var logger = KotlinLogging.logger {}


    @CrossOrigin
    @PostMapping("/debug/submitcart")
    fun processDebugCommand(@RequestParam aggregateId: UUID): CompletableFuture<CommandResult> {
        return commandGateway.send(SubmitCartCommand(aggregateId))
    }


    @CrossOrigin
    @PostMapping("/submitcart/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: SubmitCartPayload
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(SubmitCartCommand(aggregateId = payload.aggregateId))
    }


}
