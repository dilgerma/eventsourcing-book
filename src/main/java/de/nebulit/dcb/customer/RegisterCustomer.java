package de.nebulit.dcb.customer;

import org.axonframework.modelling.annotation.TargetEntityId;

record RegisterCustomer(String customerId, String name, String email) {
}
