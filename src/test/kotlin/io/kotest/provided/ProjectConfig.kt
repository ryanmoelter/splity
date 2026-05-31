package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.test.TestCaseOrder

// Kotest 6 discovers project config by this exact name (io.kotest.provided.ProjectConfig); it no
// longer scans the classpath for arbitrary AbstractProjectConfig subclasses.
@Suppress("unused")
class ProjectConfig : AbstractProjectConfig() {
  override val testCaseOrder = TestCaseOrder.Random
  override val isolationMode = IsolationMode.InstancePerLeaf
}
