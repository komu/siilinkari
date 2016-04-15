package siilinkari.parser

import org.junit.Test
import siilinkari.lexer.SyntaxErrorException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ParserTest {

    @Test
    fun variables() {
        assertParseExpression("foo", "[Ref foo]")
    }

    @Test
    fun literals() {
        assertParseExpression("42", "[Lit 42]")
        assertParseExpression("\"foo\"", "[Lit \"foo\"]")
        assertParseExpression("true", "[Lit true]")
    }

    @Test
    fun ifStatements() {
        assertParseExpression("if (x) y else z", "[If [Ref x] [Ref y] [Ref z]]")
        assertParseExpression("if (x) y", "[If [Ref x] [Ref y] []]")
    }

    @Test
    fun whileStatements() {
        assertParseExpression("while (x) y", "[While [Ref x] [Ref y]]")
    }

    @Test
    fun assignment() {
        assertParseExpression("foo = bar", "[Assign foo [Ref bar]]")
    }

    @Test
    fun vars() {
        assertParseExpression("var foo = bar", "[Var foo [Ref bar]]")
    }

    @Test
    fun vals() {
        assertParseExpression("val foo = bar", "[Val foo [Ref bar]]")
    }

    @Test
    fun ifAsAnExpression() {
        assertParseExpression("1 + if (true) 2 else 3", "[Plus [Lit 1] [If [Lit true] [Lit 2] [Lit 3]]]")
        assertParseExpression("if (true) 2 else 3 + 4", "[If [Lit true] [Lit 2] [Plus [Lit 3] [Lit 4]]]")
        assertParseExpression("(if (true) 2 else 3) + 4", "[Plus [If [Lit true] [Lit 2] [Lit 3]] [Lit 4]]")
    }

    @Test
    fun expressionList() {
        assertParseExpression("{}", "[ExpressionList []]")
        assertParseExpression("{ x; y; z }", "[ExpressionList [[Ref x], [Ref y], [Ref z]]]")
    }

    @Test
    fun assignmentToLiteralIsSyntaxError() {
        assertSyntaxError("1 = bar;")
    }

    @Test
    fun binaryOperators() {
        assertParseExpression("1 + 2", "[Plus [Lit 1] [Lit 2]]")
        assertParseExpression("1 - 2", "[Minus [Lit 1] [Lit 2]]")
        assertParseExpression("1 == 2", "[== [Lit 1] [Lit 2]]")
        assertParseExpression("1 != 2", "[!= [Lit 1] [Lit 2]]")
        assertParseExpression("1 < 2", "[< [Lit 1] [Lit 2]]")
        assertParseExpression("1 > 2", "[> [Lit 1] [Lit 2]]")
        assertParseExpression("1 <= 2", "[<= [Lit 1] [Lit 2]]")
        assertParseExpression("1 >= 2", "[>= [Lit 1] [Lit 2]]")
        assertParseExpression("true && false", "[And [Lit true] [Lit false]]")
        assertParseExpression("true || false", "[Or [Lit true] [Lit false]]")
    }

    @Test
    fun not() {
        assertParseExpression("!x", "[Not [Ref x]]")
    }

    @Test
    fun operatorPrecedence() {
        assertParseExpression("a + b == c + d", "[== [Plus [Ref a] [Ref b]] [Plus [Ref c] [Ref d]]]")
        assertParseExpression("a + (b == c) + d", "[Plus [Plus [Ref a] [== [Ref b] [Ref c]]] [Ref d]]")
        assertParseExpression("!x + y", "[Plus [Not [Ref x]] [Ref y]]")
        assertParseExpression("!(x + y)", "[Not [Plus [Ref x] [Ref y]]]")
        assertParseExpression("a + b * c + d", "[Plus [Plus [Ref a] [Multiply [Ref b] [Ref c]]] [Ref d]]")
        assertParseExpression("a == b < c", "[== [Ref a] [< [Ref b] [Ref c]]]")
        assertParseExpression("a == b || c == d && e == f", "[Or [== [Ref a] [Ref b]] [And [== [Ref c] [Ref d]] [== [Ref e] [Ref f]]]]")
    }

    @Test
    fun functionCall() {
        assertParseExpression("foo()", "[Call [Ref foo] []]")
        assertParseExpression("bar(1)", "[Call [Ref bar] [[Lit 1]]]")
        assertParseExpression("baz(1, x)", "[Call [Ref baz] [[Lit 1], [Ref x]]]")
        assertParseExpression("(baz)()", "[Call [Ref baz] []]")
    }

    @Test
    fun functionDefinition() {
        assertParseFunctionDefinition("fun square(x: Int, y: Int): Int = x * x",
            "FunctionDefinition(name=square, args=[(x, Int), (y, Int)], returnType=Int, body=[Multiply [Ref x] [Ref x]])")
    }

    private fun assertSyntaxError(code: String) {
        assertFailsWith<SyntaxErrorException> {
            val stmt = parseExpression(code)
            fail("expected syntax error, but got $stmt")
        }
    }

    private fun assertParseExpression(source: String, expected: String) {
        val expression = parseExpression(source)

        assertEquals(expected, expression.toString(), source)
    }

    private fun assertParseFunctionDefinition(source: String, expected: String) {
        val expression = parseFunctionDefinition(source)

        assertEquals(expected, expression.toString(), source)
    }
}
