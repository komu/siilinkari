package siilinkari.optimizer

import org.junit.Ignore
import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.parser.parseExpression
import siilinkari.types.Type
import siilinkari.types.typeCheck
import kotlin.test.assertEquals

class ASTOptimizerTest {

    val env = GlobalStaticEnvironment()

    @Test
    fun evaluateConstantExpressions() {
        assertOptimized("1+1", "[Lit 2]")
        assertOptimized("3+4*5", "[Lit 23]")
        assertOptimized("\"foo\" + 4 + true", "[Lit \"foo4true\"]")
        assertOptimized("!true", "[Lit false]")
        assertOptimized("!false", "[Lit true]")
        assertOptimized("4 == 4", "[Lit true]")
        assertOptimized("4 != 4", "[Lit false]")
        assertOptimized("4 != 4 == false", "[Lit true]")
    }

    @Test
    fun constantIf() {
        env.bind("foo", Type.Function(emptyList(), Type.Unit))
        env.bind("bar", Type.Function(emptyList(), Type.Unit))

        assertOptimized("if (true) foo()", "[Call [Ref foo] []]")
        assertOptimized("if (true) foo() else bar()", "[Call [Ref foo] []]")
        assertOptimized("if (false) foo()", "[ExpressionList []]")
        assertOptimized("if (false) foo() else bar()", "[Call [Ref bar] []]")
        assertOptimized("if (1 == 2) foo() else bar()", "[Call [Ref bar] []]")
    }

    @Test
    fun whileFalse() {
        env.bind("foo", Type.Function(emptyList(), Type.Unit))

        assertOptimized("while (false) foo()", "[ExpressionList []]")
        assertOptimized("while (1 == 2) foo()", "[ExpressionList []]")
    }

    @Test
    fun not() {
        env.bind("x", Type.Boolean)

        assertOptimized("!x", "[Not [Ref x]]")
        assertOptimized("!!x", "[Ref x]")
        assertOptimized("!!!x", "[Not [Ref x]]")
        assertOptimized("!!!!x", "[Ref x]")
        assertOptimized("!!!!!x", "[Not [Ref x]]")
    }

    @Test
    fun propagateConstantVariables() {
        env.bind("foo", Type.Function(listOf(Type.String), Type.Unit))
        assertOptimized("""if (true) { val s = "hello"; foo(s + ", world!") }""",
                """[ExpressionList [[Var [Local 0 (s)] [Lit "hello"]], [Call [Ref foo] [[Lit "hello, world!"]]]]]""")
    }

    @Test
    @Ignore("detecting effectively constant variables is not implemented")
    fun propagateEffectivelyConstantVariables() {
        env.bind("foo", Type.Function(listOf(Type.String), Type.Unit))
        assertOptimized("""if (true) { var s = "hello"; foo(s + ", world!") }""",
                """[ExpressionList [Var [Local 0 (s)] [Lit "hello"]], [Call [Ref foo] [[Lit "hello, world!"]]]]]""")
    }

    @Test
    @Ignore("removing unused variables is not implemented")
    fun removeUnusedVariables() {
        assertOptimized("if (true) { val s = 0 }", "[ExpressionList []]")
    }

    private fun assertOptimized(code: String, expectedAST: String) {
        val statement = parseExpression(code).typeCheck(env).optimize()

        assertEquals(expectedAST, statement.toString(), code)
    }
}
