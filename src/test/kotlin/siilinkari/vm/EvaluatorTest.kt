package siilinkari.vm

import org.junit.Test
import siilinkari.objects.Value
import siilinkari.objects.value
import siilinkari.types.TypeCheckException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluatorTest {

    val evaluator = Evaluator()
    val env = evaluator.environment

    @Test
    fun literalEvaluation() {
        assertExpressionEvaluation("42", 42.value)
        assertExpressionEvaluation("true", true.value)
        assertExpressionEvaluation("\"foo\"", "foo".value)
    }

    @Test
    fun variableEvaluation() {
        evaluator.bind("x", 123.value)

        assertExpressionEvaluation("x", 123.value)
    }

    @Test
    fun varStatements() {
        evaluateStatement("var x = 42;")

        assertExpressionEvaluation("x", 42.value)
    }

    @Test
    fun assignments() {
        evaluator.bind("x", 42.value)

        evaluateStatement("x = 123;")

        assertEquals(123.value, env["x"])
    }

    @Test
    fun ifExpressions() {
        evaluator.bind("x", true.value)
        evaluator.bind("y", 42.value)
        evaluator.bind("r", 0.value)

        evaluateStatement("if (x) r = 123; else r = y;")
        assertEquals(123.value, env["r"])

        evaluator.environment["x"] = false.value

        evaluateStatement("if (x) r = 123; else r = y;")
        assertEquals(42.value, env["r"])
    }

    @Test
    fun binaryExpressions() {
        assertExpressionEvaluation("1 + 2", 3.value)
        assertExpressionEvaluation("1 - 2", (-1).value)
        assertExpressionEvaluation("1 == 2", false.value)
        assertExpressionEvaluation("1 == 1", true.value)
        assertExpressionEvaluation("1 != 2", true.value)
        assertExpressionEvaluation("1 != 1", false.value)
    }

    @Test
    fun ifWithoutElse() {
        evaluator.bind("r", 0.value)

        evaluateStatement("if (false) r = 1;")
        assertEquals(0.value, env["r"])

        evaluateStatement("if (true) r = 2;")
        assertEquals(2.value, env["r"])
    }

    @Test
    fun whileLoop() {
        evaluator.bind("x", 5.value)
        evaluator.bind("a", 0.value)
        evaluator.bind("b", 0.value)

        evaluateStatement("""
            while (x != 0) {
                x = x - 1;
                a = a + 1;
                b = a + b;
            }
        """)

        assertEquals(0.value, env["x"])
        assertEquals(5.value, env["a"])
        assertEquals(15.value, env["b"])
    }

    @Test
    fun not() {
        assertExpressionEvaluation("!true", false.value)
        assertExpressionEvaluation("!false", true.value)
    }

    @Test
    fun evaluationFailuresForCoercions() {
        assertExpressionTypeCheckFails("1 + \"foo\"")
        assertExpressionTypeCheckFails("!1")
    }

    @Test
    fun evaluationFailsForUnboundVariables() {
        assertExpressionTypeCheckFails("x")
        assertStatementTypeCheckFails("x = 4;")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertStatementTypeCheckFails("{ var x = 4; var x = 4; }")
    }

    @Test
    fun plusWithStringLiteralOnLeftSideIsStringConcatenation() {
        assertExpressionEvaluation("\"foo \" + \"bar\"", "foo bar".value)
        assertExpressionEvaluation("\"foo \" + 42", "foo 42".value)
        assertExpressionEvaluation("\"foo \" + true", "foo true".value)
    }

    private fun assertExpressionTypeCheckFails(s: String) {
        assertFailsWith<TypeCheckException> {
            evaluateExpression(s)
        }
    }

    private fun assertStatementTypeCheckFails(s: String) {
        assertFailsWith<TypeCheckException> {
            evaluateStatement(s)
        }
    }

    private fun assertExpressionEvaluation(code: String, expectedValue: Value) {
        assertEquals(expectedValue, evaluateExpression(code))
    }

    private fun evaluateExpression(code: String) =
        evaluator.evaluateExpression(code)

    private fun evaluateStatement(code: String) {
        evaluator.evaluateStatement(code)
    }
}
