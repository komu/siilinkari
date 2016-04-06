package siilinkari.optimizer

import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.types.Type
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

    @Test
    fun constantIf() {
        env.bind("foo", Type.Function(emptyList(), Type.Unit))
        env.bind("bar", Type.Function(emptyList(), Type.Unit))

        assertOptimizedStatement("if (true) { foo(); }", "[Call [Ref foo] []]")
        assertOptimizedStatement("if (true) { foo(); } else { bar(); }", "[Call [Ref foo] []]")
        assertOptimizedStatement("if (false) { foo(); }", "[StatementList []]")
        assertOptimizedStatement("if (false) { foo(); } else { bar(); }", "[Call [Ref bar] []]")
        assertOptimizedStatement("if (1 == 2) { foo(); } else { bar(); }", "[Call [Ref bar] []]")
    }

    @Test
    fun whileFalse() {
        env.bind("foo", Type.Function(emptyList(), Type.Unit))

        assertOptimizedStatement("while (false) { foo(); }", "[StatementList []]")
        assertOptimizedStatement("while (1 == 2) { foo(); }", "[StatementList []]")
    }

    private fun assertOptimizedExpression(code: String, expectedAST: String) {
        val statement = parseExpression(code).typeCheck(env).optimize()

        assertEquals(expectedAST, statement.toString(), code)
    }

    private fun assertOptimizedStatement(code: String, expectedAST: String) {
        val statement = parseStatement(code).typeCheck(env).optimize()

        assertEquals(expectedAST, statement.toString(), code)
    }
}
