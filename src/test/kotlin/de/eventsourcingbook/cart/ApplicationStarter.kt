package de.eventsourcingbook.cart

import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object ApplicationStarter {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.from(SpringApp::main).with(ContainerConfiguration::class.java).run(*args)
    }
}

@TestConfiguration(proxyBeanMethods = false)
internal class ContainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres"))
                .withReuse(true)
                .withExposedPorts(POSTGRES_PORT)
                .withPassword("postgres")
                .withUsername("postgres")
        return postgres
    }

    @Bean
    @ServiceConnection
    fun kafkaContainer(): KafkaContainer {
        // https://github.com/spring-projects/spring-boot/pull/40695
        // TODO switch to official image with spring 3.4.0
        val kafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka")).withExposedPorts(9092, 9093)
        return kafkaContainer
    }

    companion object {
        val POSTGRES_PORT = 5432
    }
}
