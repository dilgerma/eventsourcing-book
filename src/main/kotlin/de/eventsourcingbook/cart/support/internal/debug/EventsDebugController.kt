package de.eventsourcingbook.cart.support.internal.debug

import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@ConditionalOnProperty("application.debug.enabled", havingValue = "true", matchIfMissing = false)
@RestController
class EventsDebugController(val eventStorageEngine: EventStorageEngine) {

    @CrossOrigin
    @GetMapping("/internal/debug/events/{aggregateId}")
    fun resolveEvents(@PathVariable("aggregateId") aggregateId: UUID): List<DomainEventMessage<*>> {
        return eventStorageEngine.readEvents(aggregateId.toString()).asSequence().toList()
    }
}
