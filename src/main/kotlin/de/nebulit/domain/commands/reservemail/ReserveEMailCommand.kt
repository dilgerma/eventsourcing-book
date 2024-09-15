package de.nebulit.domain.commands.reservemail

import de.nebulit.common.Command
import java.util.UUID
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ReserveEMailCommand(
    @TargetAggregateIdentifier var aggregateId: UUID,
    var email: String,
) : Command
