package siilinkari.vm

import org.junit.Test
import siilinkari.ast.FunctionDefinition
import siilinkari.objects.Value
import siilinkari.objects.value
import siilinkari.parser.parseExpression
import siilinkari.types.Type
import siilinkari.types.TypeCheckException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluatorTest {

    val evaluator = Evaluator()

    private val square = FunctionDefinition("square", listOf("x" to Type.Int), Type.Int, parseExpression("x * x"))

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

        assertExpressionEvaluation("x", 123.value)
    }

    @Test
    fun arithmetic() {
        assertExpressionEvaluation("1 + 2 * 3 + 4 / 2", 9.value)
    }

    @Test
    fun ifExpressions() {
        evaluator.bind("x", true.value)
        evaluator.bind("y", 42.value)
        evaluator.bind("r", 0.value)

        evaluateStatement("if (x) r = 123; else r = y;")
        assertExpressionEvaluation("r", 123.value)

        evaluateStatement("x = false;")

        evaluateStatement("if (x) r = 123; else r = y;")
        assertExpressionEvaluation("r", 42.value)
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
        assertExpressionEvaluation("r", 0.value)

        evaluateStatement("if (true) r = 2;")
        assertExpressionEvaluation("r", 2.value)
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

        assertExpressionEvaluation("x", 0.value)
        assertExpressionEvaluation("a", 5.value)
        assertExpressionEvaluation("b", 15.value)
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
    fun directCalls() {
        evaluator.bindFunction(square)
        assertExpressionEvaluation("square(4)", 16.value)
    }

    @Test
    fun functionCallsThroughLocalVariable() {
        evaluator.bindFunction(square)
        evaluator.bind("result", 0.value)

        evaluateStatement("""
            if (true) {
                var sq = square;
                result = sq(5);
            }
        """)

        assertExpressionEvaluation("result", 25.value)
    }

    @Test
    fun functionCallsThroughExpression() {
        evaluator.bindFunction(square)
        assertExpressionEvaluation("(square)(6)", 36.value)
    }

    @Test
    fun expressionFunctions() {
        evaluator.bindFunction(FunctionDefinition("sub", listOf("x" to Type.Int, "y" to Type.Int), Type.Int, parseExpression("x - y")))

        assertExpressionEvaluation("sub(7, 4)", 3.value)
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

    @Test
    fun relationalOperators() {
        assertExpressionEvaluation("1 == 1", true.value)
        assertExpressionEvaluation("1 != 1", false.value)
        assertExpressionEvaluation("1 < 1", false.value)
        assertExpressionEvaluation("1 <= 1", true.value)
        assertExpressionEvaluation("1 > 1", false.value)
        assertExpressionEvaluation("1 >= 1", true.value)

        assertExpressionEvaluation("1 == 2", false.value)
        assertExpressionEvaluation("1 != 2", true.value)
        assertExpressionEvaluation("1 < 2", true.value)
        assertExpressionEvaluation("1 <= 2", true.value)
        assertExpressionEvaluation("1 > 2", false.value)
        assertExpressionEvaluation("1 >= 2", false.value)
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
