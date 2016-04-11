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
            0 frame[2] = Constant(5)
            1 frame[1] = frame[2] ; store local x
            2 jump 3
            3 frame[2] = frame[1] ; load local x
            4 frame[3] = Constant(0)
            5 frame[2] = frame[2] == frame[3]
            6 frame[2] = !frame[2]
            7 jump-if-false frame[2] 14
            8 jump 9
            9 frame[2] = frame[1] ; load local x
            10 frame[3] = Constant(1)
            11 frame[2] = frame[2] - frame[3]
            12 frame[1] = frame[2] ; store local x
            13 jump 3
            14 frame[2] = Constant(Unit)
            15 ret value=frame[2], address=frame[0]
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
            0 frame[4] = Constant(4)
            1 frame[1] = frame[4] ; store local x
            2 frame[4] = Constant("")
            3 frame[2] = frame[4] ; store local s
            4 frame[4] = frame[1] ; load local x
            5 frame[5] = Constant(4)
            6 frame[4] = frame[4] == frame[5]
            7 jump-if-false frame[4] 16
            8 jump 9
            9 frame[4] = Constant("It")
            10 frame[5] = frame[4] ; dup
            11 frame[3] = frame[5] ; store local t
            12 frame[5] = Constant(" worked!")
            13 frame[4] = frame[4] ++ frame[5]
            14 frame[2] = frame[4] ; store local s
            15 jump 16
            16 frame[4] = Constant(Unit)
            17 ret value=frame[4], address=frame[0]
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
