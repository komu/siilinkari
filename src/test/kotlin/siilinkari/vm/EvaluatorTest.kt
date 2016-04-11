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
        evaluate("var x = 42;")

        assertExpressionEvaluation("x", 42.value)
    }

    @Test
    fun assignments() {
        evaluator.bind("x", 42.value)

        evaluate("x = 123;")

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

        evaluate("if (x) r = 123; else r = y;")
        assertExpressionEvaluation("r", 123.value)

        evaluate("x = false;")

        evaluate("if (x) r = 123; else r = y;")
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

        evaluate("if (false) r = 1;")
        assertExpressionEvaluation("r", 0.value)

        evaluate("if (true) r = 2;")
        assertExpressionEvaluation("r", 2.value)
    }

    @Test
    fun whileLoop() {
        evaluator.bind("x", 5.value)
        evaluator.bind("a", 0.value)
        evaluator.bind("b", 0.value)

        evaluate("""
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
        assertTypeCheckFails("1 + \"foo\"")
        assertTypeCheckFails("!1")
    }

    @Test
    fun directCalls() {
        defineSquareFunction()
        assertExpressionEvaluation("square(4)", 16.value)
    }

    @Test
    fun functionCallsThroughLocalVariable() {
        defineSquareFunction()
        evaluator.bind("result", 0.value)

        evaluate("""
            if (true) {
                var sq = square;
                result = sq(5);
            }
        """)

        assertExpressionEvaluation("result", 25.value)
    }

    @Test
    fun functionCallsThroughExpression() {
        defineSquareFunction()
        assertExpressionEvaluation("(square)(6)", 36.value)
    }

    @Test
    fun expressionFunctions() {
        evaluate("fun sub(x: Int, y: Int): Int = x - y")

        assertExpressionEvaluation("sub(7, 4)", 3.value)
    }

    @Test
    fun evaluationFailsForUnboundVariables() {
        assertTypeCheckFails("x")
        assertTypeCheckFails("x = 4;")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertTypeCheckFails("{ var x = 4; var x = 4; }")
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

    private fun assertTypeCheckFails(s: String) {
        assertFailsWith<TypeCheckException> {
            evaluate(s)
        }
    }

    private fun assertExpressionEvaluation(code: String, expectedValue: Value) {
        assertEquals(expectedValue, evaluate(code))
    }

    private fun defineSquareFunction() {
        evaluator.evaluate("fun square(x: Int): Int = x * x")
    }

    private fun evaluate(code: String) =
        evaluator.evaluate(code)
}
