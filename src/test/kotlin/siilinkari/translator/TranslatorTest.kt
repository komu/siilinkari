package siilinkari.translator

import org.junit.Test
import siilinkari.vm.Evaluator
import java.util.*
import kotlin.test.fail

class TranslatorTest {

    val evaluator = Evaluator()

    @Test
    fun simpleTranslation() {
        assertTranslation("""
            {
                var x = 4 + 1;
                while (x != 0)
                    x = x - 1
            }
            """,
            """
            0 stack[fp+2] = 5
            1 stack[fp+1] = stack[fp+2] ; store local x
            2 jump 3
            3 stack[fp+2] = stack[fp+1] ; load local x
            4 stack[fp+3] = 0
            5 stack[fp+2] = stack[fp+2] == stack[fp+3]
            6 stack[fp+2] = !stack[fp+2]
            7 jump-if-false stack[fp+2] 14
            8 jump 9
            9 stack[fp+2] = stack[fp+1] ; load local x
            10 stack[fp+3] = 1
            11 stack[fp+2] = stack[fp+2] - stack[fp+3]
            12 stack[fp+1] = stack[fp+2] ; store local x
            13 jump 3
            14 stack[fp+2] = Unit
            15 ret value=stack[fp+2], address=stack[fp+0]
            """)
    }

    @Test
    fun readMeExample() {
        assertTranslation("""
            {
                var x = 4;
                var s = "";
                if (x == 2 + 2) { var t = "It"; s = t + " worked!" }
            }
            """,
            """
            0 stack[fp+4] = 4
            1 stack[fp+1] = stack[fp+4] ; store local x
            2 stack[fp+4] = ""
            3 stack[fp+2] = stack[fp+4] ; store local s
            4 stack[fp+4] = stack[fp+1] ; load local x
            5 stack[fp+5] = 4
            6 stack[fp+4] = stack[fp+4] == stack[fp+5]
            7 jump-if-false stack[fp+4] 16
            8 jump 9
            9 stack[fp+4] = "It"
            10 stack[fp+5] = stack[fp+4] ; dup
            11 stack[fp+3] = stack[fp+5] ; store local t
            12 stack[fp+5] = " worked!"
            13 stack[fp+4] = stack[fp+4] ++ stack[fp+5]
            14 stack[fp+2] = stack[fp+4] ; store local s
            15 jump 16
            16 stack[fp+4] = Unit
            17 ret value=stack[fp+4], address=stack[fp+0]
            """)
    }

    private fun assertTranslation(source: String, expectedInstructionsAsString: String) {
        val instructions = evaluator.dump(source).lines().map { it.trim() }
        val expectedInstructions = expectedInstructionsAsString.trimIndent().lines().map { it.trim() }

        if (instructions != expectedInstructions) {

            val errors = ArrayList<String>()
            if (instructions.size != expectedInstructions.size)
                errors += "amount of instructions didn't match: expected ${expectedInstructions.size}, but got ${instructions.size}"

            instructions.zip(expectedInstructions).forEachIndexed { i, pair ->
                val (instruction, expected) = pair

                if (instruction != expected)
                    errors += "$i: expected '$expected', but got '$instruction'"
            }

            if (errors.isEmpty())
                errors += "expected: $expectedInstructions, but got $instructions"

            fail("translation of:\n\n${source.replaceIndent("  ")}\n\nproduced unexpected results:\n${errors.joinToString("\n") { "  - $it" }}")
        }
    }
}
