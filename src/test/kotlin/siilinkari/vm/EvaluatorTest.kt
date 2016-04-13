package siilinkari.vm

import org.junit.Test
import siilinkari.objects.Value
import siilinkari.objects.value
import siilinkari.runtime.fun1
import siilinkari.runtime.fun2
import siilinkari.types.Type
import siilinkari.types.TypeCheckException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluatorTest {

    val evaluator = Evaluator().apply {
        trace = false
    }

    @Test
    fun literalEvaluation() {
        assertEvaluation("42", 42.value)
        assertEvaluation("true", true.value)
        assertEvaluation("\"foo\"", "foo".value)
    }

    @Test
    fun variableEvaluation() {
        evaluator.bind("x", 123.value)

        assertEvaluation("x", 123.value)
    }

    @Test
    fun varStatements() {
        evaluate("var x = 42")

        assertEvaluation("x", 42.value)
    }

    @Test
    fun assignments() {
        evaluator.bind("x", 42.value)

        evaluate("x = 123")

        assertEvaluation("x", 123.value)
    }

    @Test
    fun arithmetic() {
        assertEvaluation("1 + 2 * 3 + 4 / 2", 9.value)
    }

    @Test
    fun ifExpressions() {
        evaluator.bind("x", true.value)
        evaluator.bind("y", 42.value)
        evaluator.bind("r", 0.value)

        evaluate("if (x) r = 123 else r = y")
        assertEvaluation("r", 123.value)

        evaluate("x = false")

        evaluate("if (x) r = 123 else r = y")
        assertEvaluation("r", 42.value)
    }

    @Test
    fun ifExpressionValues() {
        evaluator.bind("x", true.value)

        assertEvaluation("if (x) 1 else 2", 1.value)
        assertEvaluation("if (!x) 1 else 2", 2.value)
    }

    @Test
    fun binaryExpressions() {
        assertEvaluation("1 + 2", 3.value)
        assertEvaluation("1 - 2", (-1).value)
        assertEvaluation("1 == 2", false.value)
        assertEvaluation("1 == 1", true.value)
        assertEvaluation("1 != 2", true.value)
        assertEvaluation("1 != 1", false.value)
    }

    @Test
    fun ifWithoutElse() {
        evaluator.bind("r", 0.value)

        evaluate("if (false) r = 1")
        assertEvaluation("r", 0.value)

        evaluate("if (true) r = 2")
        assertEvaluation("r", 2.value)
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
                b = a + b
            }
        """)

        assertEvaluation("x", 0.value)
        assertEvaluation("a", 5.value)
        assertEvaluation("b", 15.value)
    }

    @Test
    fun not() {
        evaluate("val x = true")
        assertEvaluation("!x", false.value)
        assertEvaluation("!!x", true.value)
    }

    @Test
    fun evaluationFailuresForCoercions() {
        assertTypeCheckFails("1 + \"foo\"")
        assertTypeCheckFails("!1")
    }

    @Test
    fun directCalls() {
        defineSquareFunction()
        assertEvaluation("square(4)", 16.value)
    }

    @Test
    fun functionCallsThroughLocalVariable() {
        defineSquareFunction()
        evaluator.bind("result", 0.value)

        evaluate("""
            if (true) {
                var sq = square;
                result = sq(5)
            }
        """)

        assertEvaluation("result", 25.value)
    }

    @Test
    fun functionCallsThroughExpression() {
        defineSquareFunction()
        assertEvaluation("(square)(6)", 36.value)
    }

    @Test
    fun expressionFunctions() {
        evaluate("fun sub(x: Int, y: Int): Int = x - y")

        assertEvaluation("sub(7, 4)", 3.value)
    }

    @Test
    fun evaluationFailsForUnboundVariables() {
        assertTypeCheckFails("x")
        assertTypeCheckFails("x = 4")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertTypeCheckFails("{ var x = 4; var x = 4 }")
    }

    @Test
    fun plusWithStringLiteralOnLeftSideIsStringConcatenation() {
        assertEvaluation("\"foo \" + \"bar\"", "foo bar".value)
        assertEvaluation("\"foo \" + 42", "foo 42".value)
        assertEvaluation("\"foo \" + true", "foo true".value)
    }

    @Test
    fun relationalOperators() {
        assertEvaluation("1 == 1", true.value)
        assertEvaluation("1 != 1", false.value)
        assertEvaluation("1 < 1", false.value)
        assertEvaluation("1 <= 1", true.value)
        assertEvaluation("1 > 1", false.value)
        assertEvaluation("1 >= 1", true.value)

        assertEvaluation("1 == 2", false.value)
        assertEvaluation("1 != 2", true.value)
        assertEvaluation("1 < 2", true.value)
        assertEvaluation("1 <= 2", true.value)
        assertEvaluation("1 > 2", false.value)
        assertEvaluation("1 >= 2", false.value)
    }

    @Test
    fun nativeFunctionCallWithSingleParameter() {
        evaluator.bind("inc", fun1<Value.Integer>("inc", Type.Int, Type.Int) { it + 1.value }, mutable = false)

        assertEvaluation("inc(4)", 5.value)
    }

    @Test
    fun nativeFunctionWithMultipleParameters() {
        evaluator.bind("sub", fun2<Value.Integer>("sub", Type.Int, Type.Int) { a, b -> a - b }, mutable = false)

        assertEvaluation("sub(7, 4)", 3.value)
    }

    @Test
    fun nestedIfs() {
        assertEvaluation("""if (false) 1 else if (true) 2 else 3""", 2.value)
    }

    @Test
    fun recursion() {
        evaluate("""
            fun fib(i: Int): Int =
                if (i == 0)
                    0
                else if (i == 1)
                    1
                else
                    fib(i-1) + fib(i-2)
        """)

        assertEvaluation("fib(2)", 1.value)
        assertEvaluation("fib(10)", 55.value)
        assertEvaluation("fib(20)", 6765.value)
    }

    private fun assertTypeCheckFails(s: String) {
        assertFailsWith<TypeCheckException> {
            evaluate(s)
        }
    }

    private fun assertEvaluation(code: String, expectedValue: Value) {
        evaluator.optimize = true
        assertEquals(expectedValue, evaluate(code))
        evaluator.optimize = false
        assertEquals(expectedValue, evaluate(code))
    }

    private fun defineSquareFunction() {
        evaluator.evaluate("fun square(x: Int) = x * x")
    }

    private fun evaluate(code: String): Value =
        evaluator.evaluate(code).value
}
