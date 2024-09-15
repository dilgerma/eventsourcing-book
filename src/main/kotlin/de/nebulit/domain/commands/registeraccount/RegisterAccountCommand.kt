package de.nebulit.domain.commands.registeraccount

import de.nebulit.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class RegisterAccountCommand(
    @TargetAggregateIdentifier var aggregateId: UUID,
    var email: String,
) : Command
