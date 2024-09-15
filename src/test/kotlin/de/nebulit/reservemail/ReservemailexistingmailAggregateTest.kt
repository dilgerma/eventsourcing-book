package de.nebulit.reservemail

import de.nebulit.common.Event
import de.nebulit.common.support.RandomData
import de.nebulit.domain.AccountAggregate
import de.nebulit.domain.commands.reservemail.ReserveEMailCommand
import de.nebulit.events.EMailReservedEvent
import java.util.UUID
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**  */
class ReservemailexistingmailAggregateTest {
  private lateinit var fixture: FixtureConfiguration<AccountAggregate>

  @BeforeEach
  fun setUp() {
    fixture = AggregateTestFixture(AccountAggregate::class.java)
  }

  @Test
  fun `Reservemailexistingmail Aggregate Test`() {
    // GIVEN
    val events = mutableListOf<Event>()

    events.add(
        RandomData.newInstance<EMailReservedEvent> {
          aggregateId = UUID.fromString("7af3132a-9af4-403b-a564-06edf13492bd")
          email = RandomData.newInstance {}
        },
    )

    // WHEN
    val command =
        ReserveEMailCommand(
            aggregateId = UUID.fromString("7af3132a-9af4-403b-a564-06edf13492bd"),
            email = RandomData.newInstance {},
        )

    // THEN
    val expectedEvents = mutableListOf<Event>()

    expectedEvents.add(
        RandomData.newInstance<EMailReservedEvent> {
          this.aggregateId = command.aggregateId
          this.email = command.email
        },
    )

    fixture
        .given(events)
        .`when`(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(*expectedEvents.toTypedArray())
  }
}
