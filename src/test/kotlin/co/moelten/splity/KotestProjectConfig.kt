package co.moelten.splity

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.test.TestCaseOrder

object KotestProjectConfig : AbstractProjectConfig() {
//  override val assertionMode = AssertionMode.Error
  override val testCaseOrder = TestCaseOrder.Random
  override val isolationMode = IsolationMode.InstancePerLeaf
}
