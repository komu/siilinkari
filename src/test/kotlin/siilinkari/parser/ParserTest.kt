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
        assertParseStatement("if (x) y; else z;", "[If [Ref x] [Ref y] [Ref z]]")
        assertParseStatement("if (x) y;", "[If [Ref x] [Ref y] []]")
    }

    @Test
    fun whileStatements() {
        assertParseStatement("while (x) y;", "[While [Ref x] [Ref y]]")
    }

    @Test
    fun assignment() {
        assertParseStatement("foo = bar;", "[Assign foo [Ref bar]]")
    }

    @Test
    fun vars() {
        assertParseStatement("var foo = bar;", "[Var foo [Ref bar]]")
    }

    @Test
    fun vals() {
        assertParseStatement("val foo = bar;", "[Val foo [Ref bar]]")
    }

    @Test
    fun statementList() {
        assertParseStatement("{}", "[StatementList []]")
        assertParseStatement("{ x; y; z; }", "[StatementList [[Ref x], [Ref y], [Ref z]]]")
    }

    @Test
    fun assignmentToLiteralIsSyntaxError() {
        assertSyntaxError("1 = bar;")
    }

    @Test
    fun binaryOperators() {
        assertParseExpression("1 + 2", "[Plus [Lit 1] [Lit 2]]")
        assertParseExpression("1 - 2", "[Minus [Lit 1] [Lit 2]]")
        assertParseExpression("1 == 2", "[Equals [Lit 1] [Lit 2]]")
        assertParseExpression("1 != 2", "[NotEquals [Lit 1] [Lit 2]]")
        assertParseExpression("1 < 2", "[LessThan [Lit 1] [Lit 2]]")
        assertParseExpression("1 > 2", "[GreaterThan [Lit 1] [Lit 2]]")
        assertParseExpression("1 <= 2", "[LessThanOrEqual [Lit 1] [Lit 2]]")
        assertParseExpression("1 >= 2", "[GreaterThanOrEqual [Lit 1] [Lit 2]]")
    }

    @Test
    fun not() {
        assertParseExpression("!x", "[Not [Ref x]]")
    }

    @Test
    fun operatorPrecedence() {
        assertParseExpression("a + b == c + d", "[Equals [Plus [Ref a] [Ref b]] [Plus [Ref c] [Ref d]]]")
        assertParseExpression("a + (b == c) + d", "[Plus [Plus [Ref a] [Equals [Ref b] [Ref c]]] [Ref d]]")
        assertParseExpression("!x + y", "[Plus [Not [Ref x]] [Ref y]]")
        assertParseExpression("!(x + y)", "[Not [Plus [Ref x] [Ref y]]]")
        assertParseExpression("a + b * c + d", "[Plus [Plus [Ref a] [Multiply [Ref b] [Ref c]]] [Ref d]]")
        assertParseExpression("a == b < c", "[Equals [Ref a] [LessThan [Ref b] [Ref c]]]")
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
            val stmt = parseStatement(code)
            fail("expected syntax error, but got $stmt")
        }
    }

    private fun assertParseStatement(source: String, expected: String) {
        val statement = parseStatement(source)

        assertEquals(expected, statement.toString(), source)
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
