package leakcanary.internal

import leakcanary.HeapAnalysisSuccess
import leakcanary.HeapValue.IntValue
import leakcanary.HeapValue.LongValue
import leakcanary.HeapValue.ObjectReference
import leakcanary.LeakingInstance
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RetainedSizeTest {

  @get:Rule
  var testFolder = TemporaryFolder()
  private lateinit var hprofFile: File

  @Before
  fun setUp() {
    hprofFile = testFolder.newFile("temp.hprof")
  }

  @Test fun test_a_few_things() {
    hprofFile.dump {
      val parentClass = clazz("Parent", fields = listOf("aa" to LongValue::class))
      val childClass =
        clazz("Child", superClassId = parentClass, fields = listOf("bb" to IntValue::class))

      "GcRoot" clazz {
        staticField["shortestPath"] = "Foo" watchedInstance {
          field["bar"] = "Bar" instance {
            field["a"] = ObjectReference(instance(childClass, listOf(LongValue(1), IntValue(2))))
          }
        }
      }
    }
    val analysis = hprofFile.checkForLeaks<HeapAnalysisSuccess>(computeRetainedHeapSize = true)
    val leak = analysis.retainedInstances[0] as LeakingInstance
    assertThat(leak.retainedHeapSize).isEqualTo(4 + 4 + 4 + 8)
  }

}