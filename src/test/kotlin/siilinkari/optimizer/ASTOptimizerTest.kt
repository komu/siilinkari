package siilinkari.optimizer

import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.parser.parseExpression
import siilinkari.types.typeCheck
import kotlin.test.assertEquals

class ASTOptimizerTest {

    val env = GlobalStaticEnvironment()

    @Test
    fun evaluateConstantExpressions() {
        assertOptimizedExpression("1+1", "[Lit 2]")
        assertOptimizedExpression("3+4*5", "[Lit 23]")
        assertOptimizedExpression("\"foo\" + 4 + true", "[Lit \"foo4true\"]")
        assertOptimizedExpression("!true", "[Lit false]")
        assertOptimizedExpression("!false", "[Lit true]")
        assertOptimizedExpression("4 == 4", "[Lit true]")
        assertOptimizedExpression("4 != 4", "[Lit false]")
        assertOptimizedExpression("4 != 4 == false", "[Lit true]")
    }

    private fun assertOptimizedExpression(code: String, expectedAST: String) {
        val statement = parseExpression(code).typeCheck(env).optimize()

        assertEquals(expectedAST, statement.toString(), code)
    }
}
