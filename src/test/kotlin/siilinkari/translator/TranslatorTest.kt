package siilinkari.translator

import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.parser.parseStatement
import siilinkari.types.typeCheck
import siilinkari.vm.CodeSegment
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
            3 StoreLocal 0 ; x
            4 Jump 5
            5 LoadLocal 0 ; x
            6 Push 0
            7 Equal
            8 Not
            9 JumpIfFalse 16
            10 Jump 11
            11 LoadLocal 0 ; x
            12 Push 1
            13 Subtract
            14 StoreLocal 0 ; x
            15 Jump 5
            """)
    }

    private fun assertTranslation(source: String, translated: String) {
        assertEquals(translated.trimIndent(), translateStatement(source))
    }

    private fun translateStatement(code: String): String {
        val typed = parseStatement(code).typeCheck(GlobalStaticEnvironment())

        val segment = CodeSegment.Builder()
        typed.translateTo(segment)
        return segment.build().toString()
    }
}
