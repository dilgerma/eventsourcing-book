package de.eventsourcingbook.cart.common.support

import java.time.Duration
import org.awaitility.Awaitility
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
abstract class BaseIntegrationTest {

    companion object {
        @org.testcontainers.junit.jupiter.Container
        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres")).withReuse(true)

      @ServiceConnection
        @org.testcontainers.junit.jupiter.Container
        private val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))
            .withExposedPorts(9092)
            .withExposedPorts(9093)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { "test" }
            registry.add("spring.datasource.password") { "test" }
            registry.add("spring.flyway.user") { "test" }
            registry.add("spring.flyway.password") { "test" }
        }
    }
}

fun awaitUntilAssserted(fn: () -> Unit) {
    Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(15)).untilAsserted { fn() }
}
