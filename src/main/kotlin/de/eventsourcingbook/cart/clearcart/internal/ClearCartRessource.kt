package de.eventsourcingbook.cart.clearcart.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.clearcart.ClearCartCommand
import java.util.UUID
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class ClearCartPayload(var aggregateId: UUID)

@RestController
class ClearCartRessource(private var commandGateway: CommandGateway) {

    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/clearcart")
    fun processDebugCommand(@RequestParam aggregateId: UUID): CompletableFuture<CommandResult> {
        return commandGateway.send(ClearCartCommand(aggregateId))
    }

    @CrossOrigin
    @PostMapping("/clearcart/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: ClearCartPayload
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(ClearCartCommand(aggregateId = payload.aggregateId))
    }
}
