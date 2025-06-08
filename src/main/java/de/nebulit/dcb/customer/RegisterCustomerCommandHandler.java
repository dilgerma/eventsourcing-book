package de.nebulit.dcb.customer;

import de.nebulit.dcb.events.CustomerRegistered;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.gateway.EventAppender;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.EventSourcedEntity;
import org.axonframework.eventsourcing.configuration.EventSourcedEntityBuilder;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.modelling.annotation.InjectEntity;
import org.axonframework.modelling.configuration.StatefulCommandHandlingModule;

import java.util.ArrayList;
import java.util.List;

class RegisterCustomerCommandHandler {

    @EventSourcedEntity(tagKey = "email")
    static class State {

        private List<String> emails = new ArrayList<>();

        @EventSourcingHandler
        public void evolve(CustomerRegistered event) {
            this.emails.add(event.email());
        }
    }

    @CommandHandler
    public void handle(
            RegisterCustomer command,
            EventAppender eventAppender,
            @InjectEntity(idProperty = "email") State state) {
        if (state.emails.contains(command.email())) {
            throw new IllegalStateException("Email already registered");
        }
        CustomerRegistered event = new CustomerRegistered(
                command.customerId(),
                command.name(),
                command.email()
        );
        eventAppender.append(event);
    }

    public static void configure(EventSourcingConfigurer configurer) {

        var stateEntity = EventSourcedEntityBuilder
                .annotatedEntity(String.class, RegisterCustomerCommandHandler.State.class);


        StatefulCommandHandlingModule.CommandHandlerPhase commandHandlingModule = StatefulCommandHandlingModule.named("RegisterCustomer")
                .entities()
                .entity(stateEntity)
                .commandHandlers()
                .annotatedCommandHandlingComponent(c -> new RegisterCustomerCommandHandler());

        configurer.registerStatefulCommandHandlingModule(commandHandlingModule);


    }
}
