package de.eventsourcingbook.cart

import java.util.UUID

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.test.ApplicationModuleTest

class ModuleTest  {
    @Test
    fun verifyModules() {
        var modules = ApplicationModules.of(SpringApp::class.java)
        modules.verify()
    }
}
