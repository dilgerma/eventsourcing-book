package de.nebulit.dcb.customer;

import org.axonframework.configuration.ApplicationConfigurer;
import org.axonframework.eventsourcing.configuration.EventSourcedEntityBuilder;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;

import java.util.logging.Logger;

public class CustomerApplication {

    private static final Logger logger = Logger.getLogger(CustomerApplication.class.getName());

    public static ApplicationConfigurer configurer() {
        EventSourcingConfigurer eventSourcingConfigurer = EventSourcingConfigurer.create();
        RegisterCustomerCommandHandler.configure(eventSourcingConfigurer);
        return eventSourcingConfigurer;
    }
}
