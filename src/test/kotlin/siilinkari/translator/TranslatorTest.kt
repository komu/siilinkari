package siilinkari.translator

import org.junit.Test
import siilinkari.vm.Evaluator
import kotlin.test.assertEquals

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
            0 Push 5
            1 StoreLocal 0 ; x
            2 Jump 3
            3 LoadLocal 0 ; x
            4 Push 0
            5 Equal
            6 Not
            7 JumpIfFalse 14
            8 Jump 9
            9 LoadLocal 0 ; x
            10 Push 1
            11 Subtract
            12 StoreLocal 0 ; x
            13 Jump 3
            14 PushUnit
            15 Quit
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
            0 Push 4
            1 StoreLocal 0 ; x
            2 Push ""
            3 StoreLocal 1 ; s
            4 LoadLocal 0 ; x
            5 Push 4
            6 Equal
            7 JumpIfFalse 17
            8 Jump 9
            9 Push "It"
            10 Dup
            11 StoreLocal 2 ; t
            12 Push " worked!"
            13 ConcatString
            14 StoreLocal 1 ; s
            15 PushUnit
            16 Jump 17
            17 PushUnit
            18 Quit
            """)
    }

    private fun assertTranslation(source: String, translated: String) {
        assertEquals(translated.trimIndent(), evaluator.dump(source))
    }
}
