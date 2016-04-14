package siilinkari.optimizer

import org.junit.Assert.assertEquals
import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.parser.parseExpression
import siilinkari.types.typeCheck

class EvaluateConstantsTest {

    @Test
    fun simpleConstantEvaluation() {
        assertConstantEvaluation("1+1", "[Lit 2]")
        assertConstantEvaluation("6/2", "[Lit 3]")
        assertConstantEvaluation("\"foo\" + \"bar\"", "[Lit \"foobar\"]")
        assertConstantEvaluation("\"foo\" + 42", "[Lit \"foo42\"]")
    }

    @Test
    fun divisionByZeroWillNotBeThrownAtCompileTime() {
        assertConstantEvaluation("1/0", "[Divide [Lit 1] [Lit 0]]")
        assertConstantEvaluation("(1+1)/(1-1)", "[Divide [Lit 2] [Lit 0]]")
    }

    private fun assertConstantEvaluation(code: String, expected: String) {
        val result = parseExpression(code).typeCheck(GlobalStaticEnvironment()).evaluateConstantExpressions()
        assertEquals(expected, result.toString())
    }
}
