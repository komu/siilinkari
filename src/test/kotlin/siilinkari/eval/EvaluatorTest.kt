package siilinkari.eval

import org.junit.Test
import siilinkari.objects.Value
import siilinkari.objects.value
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
        env.bind("x", 123.value)

        assertExpressionEvaluation("x", 123.value)
    }

    @Test
    fun varStatements() {
        evaluateStatement("var x = 42;")

        assertExpressionEvaluation("x", 42.value)
    }

    @Test
    fun assignments() {
        env.bind("x", 42.value)

        evaluateStatement("x = 123;")

        assertEquals(123.value, env["x"])
    }

    @Test
    fun ifExpressions() {
        env.bind("x", true.value)
        env.bind("y", 42.value)
        env.bind("r", 0.value)

        evaluateStatement("if (x) r = 123; else r = y;")
        assertEquals(123.value, env["r"])

        env["x"] = false.value

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
        env.bind("r", 0.value)

        evaluateStatement("if (false) r = 1;")
        assertEquals(0.value, env["r"])

        evaluateStatement("if (true) r = 2;")
        assertEquals(2.value, env["r"])
    }

    @Test
    fun whileLoop() {
        env.bind("x", 5.value)
        env.bind("a", 0.value)
        env.bind("b", 0.value)

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
        assertExpressionEvaluationFails("1 + \"foo\"")
        assertExpressionEvaluationFails("!1")

    }

    @Test
    fun evaluationFailsForUnboundVariables() {
        assertExpressionEvaluationFails("x")
        assertStatementEvaluationFails("x = 4;")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertStatementEvaluationFails("{ var x = 4; var x = 4; }")
    }

    private fun assertExpressionEvaluationFails(s: String) {
        assertFailsWith<EvaluationException> {
            evaluateExpression(s)
        }
    }

    private fun assertStatementEvaluationFails(s: String) {
        assertFailsWith<EvaluationException> {
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
