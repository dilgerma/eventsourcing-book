package de.nebulit.dcb.events;

import org.axonframework.eventsourcing.annotations.EventTag;

public record CustomerRegistered(
        @EventTag(key = "customerId")
        String customerId,
        String name,
        @EventTag(key = "email")
        String email
) {}
