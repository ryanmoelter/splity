package co.moelten.splity

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.test.TestCaseOrder

@Suppress("unused")
object KotestProjectConfig : AbstractProjectConfig() {
  override val testCaseOrder = TestCaseOrder.Random
  override val isolationMode = IsolationMode.InstancePerLeaf
}
