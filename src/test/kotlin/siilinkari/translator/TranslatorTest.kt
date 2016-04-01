package siilinkari.translator

import org.junit.Test
import siilinkari.parser.parseStatement
import siilinkari.types.TypeChecker
import siilinkari.types.TypeEnvironment
import kotlin.test.assertEquals

class TranslatorTest {

    @Test
    fun simpleTranslation() {
        assertTranslation("""
            {
                var x = 4 + 1;
                while (x != 0) {
                    x = x - 1;
                }
            }
            """,
            """
            0 Push 4
            1 Push 1
            2 Add
            3 Bind x
            4 Load x
            5 Push 0
            6 Equal
            7 Not
            8 JumpIfFalse [Label 14]
            9 Load x
            10 Push 1
            11 Subtract
            12 Store x
            13 Jump [Label 4]
            """)
    }

    private fun assertTranslation(source: String, translated: String) {
        assertEquals(translated.trimIndent(), translateStatement(source))
    }

    private fun translateStatement(code: String): String {
        val typed = TypeChecker(TypeEnvironment()).typeCheck(parseStatement(code))

        return typed.translate().toString()
    }
}
