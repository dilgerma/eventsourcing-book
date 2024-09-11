package de.nebulit

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModuleTest {
  @Test
  fun verifyModules() {
    var modules = ApplicationModules.of(SpringApp::class.java)
    modules.verify()
  }
}
