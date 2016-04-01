package siilinkari.lexer

import org.junit.Test
import kotlin.test.assertEquals

class SourceLocationTest {

    @Test
    fun defaultToStringProvidesBasicInfo() {
        val location = SourceLocation("dummy.sk", 42, 14, "    if (foo) bar() else baz()")

        assertEquals("[dummy.sk:42:14]", location.toString())
    }

    @Test
    fun stringRepresentationProvidesInformationAboutCurrentLine() {
        val location = SourceLocation("dummy.sk", 42, 14, "    if (foo) bar() else baz()")

        assertEquals("""
            |[dummy.sk:42:14]     if (foo) bar() else baz()
            |                              ^
            |
        """.trimMargin(), location.toLongString())
    }
}
