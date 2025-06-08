package de.nebulit.dcb.customer;

import de.nebulit.dcb.events.CustomerRegistered;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterCustomerCommandHandlerTest {


        private AxonTestFixture fixture;

        @BeforeEach
        void beforeEach() {
            fixture = AxonTestFixture.with(CustomerApplication.configurer());
        }

    @Test
    void handleCustomerRegistration() {

        fixture.given()
                .noPriorActivity()
                .when()
                .command(new RegisterCustomer("1", "Martin", "martin@nebulit.de"))
                .then()
                .events(new CustomerRegistered("1", "Martin", "martin@nebulit.de"));
    }

    @Test
    void handleDuplicateEmailRegistration() {

        fixture.given()
                .event(new CustomerRegistered("2", "Martin D.", "martin@nebulit.de"))
                .when()
                .command(new RegisterCustomer("1", "Martin", "martin@nebulit.de"))
                .then()
                .exception(IllegalStateException.class);
    }
}