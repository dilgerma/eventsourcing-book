package de.eventsourcingbook.cart

import jakarta.persistence.EntityManagerFactory
import mu.KotlinLogging
import org.axonframework.commandhandling.CommandBus
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.commandhandling.gateway.DefaultCommandGateway
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.LoggingErrorHandler
import org.axonframework.eventhandling.PropagatingErrorHandler
import org.axonframework.messaging.MessageDispatchInterceptor
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.deadletter.Decisions
import org.axonframework.messaging.deadletter.EnqueuePolicy
import org.axonframework.messaging.interceptors.BeanValidationInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.modulith.Modulith
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
class ValidatorConfig {
    @Bean
    fun localValidatorFactoryBean(): LocalValidatorFactoryBean = LocalValidatorFactoryBean()
}

@Configuration
class ValidationConfig {
    @Bean
    fun beanValidationInterceptor(validatorFactory: LocalValidatorFactoryBean): BeanValidationInterceptor<*> =
        BeanValidationInterceptor<CommandMessage<*>>(validatorFactory)
}

@EnableTransactionManagement
@Configuration
class TransactionManagerConfig {
    @Primary
    @Bean
    fun transactionManager(emf: EntityManagerFactory): PlatformTransactionManager = JpaTransactionManager(emf)
}

@Configuration
class AxonConfig {
    val logger = KotlinLogging.logger {}

    @Autowired
    fun configurationEventHandling(config: EventProcessingConfigurer) {
        config.registerDefaultListenerInvocationErrorHandler { PropagatingErrorHandler.instance() }
        config.registerListenerInvocationErrorHandler("inventories") { LoggingErrorHandler() }
        config.registerDeadLetterPolicy("publish_cart") { config ->
            EnqueuePolicy { letter, cause ->
                var retries: Int = letter.diagnostics().getOrDefault("retries", 0) as Int
                if (retries < 5) {
                    Decisions.requeue<EventMessage<*>>(cause) { dl -> dl.diagnostics().and("retries", retries + 1) }
                }
                Decisions.doNotEnqueue()
            } 
        }
    }

    @Bean
    fun commandGateway(
        commandBus: CommandBus?,
        dispatchInterceptors: List<MessageDispatchInterceptor<CommandMessage<*>>>,
        handlerInterceptor: List<MessageHandlerInterceptor<CommandMessage<*>>>,
    ): CommandGateway {
        handlerInterceptor.forEach { it -> commandBus?.registerHandlerInterceptor(it) }
        return DefaultCommandGateway
            .builder()
            .commandBus(commandBus!!)
            .dispatchInterceptors(*dispatchInterceptors.toTypedArray())
            .build()
    }
}

@Modulith(
    systemName = "System",
    sharedModules = ["de.eventsourcingbook.cart.common", "de.eventsourcingbook.cart.domain"],
    useFullyQualifiedModuleNames = true,
)
@EnableJpaRepositories
@SpringBootApplication
@EnableScheduling
@EntityScan(
    basePackages =
        [
            "de.eventsourcingbook.cart",
            "org.springframework.modulith.events.jpa",
            "org.axonframework.eventhandling.tokenstore",
            "org.axonframework.eventsourcing.eventstore.jpa",
            "org.axonframework.modelling.saga.repository.jpa",
            "org.axonframework.eventhandling.deadletter.jpa",
        ],
)
@EnableKafka
class SpringApp {
    companion object {
        fun main(args: Array<String>) {
            runApplication<SpringApp>(*args)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SpringApp>(*args)
}
