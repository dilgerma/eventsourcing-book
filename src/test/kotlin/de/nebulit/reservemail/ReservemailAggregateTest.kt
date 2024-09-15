package de.nebulit.reservemail

import de.nebulit.common.Event
import de.nebulit.common.support.RandomData
import de.nebulit.domain.ReserveEmailAggregate
import de.nebulit.domain.commands.reservemail.ReserveEMailCommand
import de.nebulit.events.AccountRegisteredEvent
import de.nebulit.events.EMailReservedEvent
import java.util.UUID
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**  */
class ReservemailAggregateTest {
  private lateinit var fixture: FixtureConfiguration<ReserveEmailAggregate>

  @BeforeEach
  fun setUp() {
    fixture = AggregateTestFixture(ReserveEmailAggregate::class.java)
  }

  @Test
  fun `Reservemail Aggregate Test`() {
    // GIVEN
    val events = mutableListOf<Event>()

    // WHEN
    val command =
        ReserveEMailCommand(
            aggregateId = UUID.fromString("2459cb7c-1276-475e-9fef-815e6c511383"),
            email = RandomData.newInstance {},
        )

    // THEN
    val expectedEvents = mutableListOf<Event>()

    expectedEvents.addAll(
        listOf(
            RandomData.newInstance<AccountRegisteredEvent> {
              this.aggregateId = command.aggregateId
              this.email = command.email
            },
            RandomData.newInstance<EMailReservedEvent> {
              this.aggregateId = command.aggregateId
              this.email = command.email
            },
        ),
    )

    fixture
        .given(events)
        .`when`(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(*expectedEvents.toTypedArray())
  }
}
