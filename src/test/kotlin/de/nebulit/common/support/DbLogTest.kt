package de.nebulit.common.support

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties =
        [
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.show-sql=true",
        ])
@Disabled
class DbLogTest : BaseIntegrationTest() {

  @Test
  fun logSqlStatements() {
    // no-op - just logs sql
  }
}
