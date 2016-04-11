package siilinkari.types

import org.junit.Test
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.env.StaticEnvironment
import siilinkari.parser.parseExpression
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TypeCheckerTest {

    var env: StaticEnvironment = GlobalStaticEnvironment()

    @Test
    fun literalTypes() {
        assertType(Type.String, "\"foo\"")
        assertType(Type.Int, "123")
        assertType(Type.Boolean, "true")
    }

    @Test
    fun boundVariableTypes() {
        env.bind("s", Type.String)
        env.bind("b", Type.Boolean)

        assertType(Type.String, "s")
        assertType(Type.Boolean, "b")
    }

    @Test
    fun not() {
        assertType(Type.Boolean, "!true")
        assertTypeCheckFails("!1")
    }

    @Test
    fun equalityComparison() {
        assertType(Type.Boolean, "true == false")
        assertType(Type.Boolean, "1 == 1")
        assertType(Type.Boolean, "\"foo\" == \"bar\"")

        assertType(Type.Boolean, "true != false")
        assertType(Type.Boolean, "1 != 1")
        assertType(Type.Boolean, "\"foo\" != \"bar\"")

        assertTypeCheckFails("true == 1")
        assertTypeCheckFails("true != 1")
    }

    @Test
    fun numericOperators() {
        assertType(Type.Int, "1 + 1")
        assertType(Type.Int, "1 - 1")

        assertTypeCheckFails("1 + true")
        assertTypeCheckFails("true + 1")
        assertTypeCheckFails("true + true")
        assertTypeCheckFails("1 + \"foo\"")
        assertTypeCheckFails("true + \"foo\"")

        assertTypeCheckFails("1 - true")
        assertTypeCheckFails("true - 1")
        assertTypeCheckFails("true - true")
        assertTypeCheckFails("\"foo\" - \"bar\"")
    }

    @Test
    fun ifWithoutElseProducesUnit() {
        assertType(Type.Unit, "if (true) 42")
    }

    @Test
    fun ifWithIncompatibleTypesProducesUnit() {
        assertType(Type.Unit, "if (true) 42 else false")
    }

    @Test
    fun ifWithCompatibleTypesReturnsTheCommonType() {
        assertType(Type.Int, "if (true) 42 else 31")
        assertType(Type.String, "if (true) \"foo\" else \"bar\"")
    }

    @Test
    fun plusWithStringLiteral() {
        assertType(Type.String, "\"foo\" + \"bar\"")
        assertType(Type.String, "\"foo\" + 42")
        assertType(Type.String, "\"foo\" + true")
    }

    @Test
    fun variableCanBeReboundInNestedEnvironment() {
        env.bind("x", Type.Boolean)

        typeCheck("if (x) { var x = 42 }")
        typeCheck("while (x) { var x = 42 }")
    }

    @Test
    fun variableIsVisibleInNestedEnvironment() {
        typeCheck("""
            if (true) {
                var x = 4;
                if (true) {
                    var y = x
                }
            }
            """)
    }

    @Test
    fun variablesDefinedByNestedEnvironmentAreNotVisibleOutside() {
        assertTypeCheckFails("""
            if (true) {
                if (true) {
                    var x = 4
                };
                var y = x
            }
            """)
    }

    @Test
    fun unboundVariables() {
        assertTypeCheckFails("x")
        assertTypeCheckFails("x = 4")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertTypeCheckFails("{ var x = 4; var x = 4 }")
    }

    @Test
    fun unboundVariableType() {
        assertTypeCheckFails("s")
    }

    @Test
    fun assigningToParameters() {
        env = GlobalStaticEnvironment().newScope(listOf("foo" to Type.Int))
        assertTypeCheckFails("foo = 42")
    }

    @Test
    fun assignmentToImmutableVariables() {
        assertTypeCheckFails("""
            if (true) {
                val x = 4;
                x = 2
            }
        """)
    }

    @Test
    fun relationalOperatorsAreNotSupportedForUnit() {
        env.bind("foo", Type.Unit)

        assertTypeCheckFails("foo == foo")
        assertTypeCheckFails("foo != foo")
        assertTypeCheckFails("foo < foo")
        assertTypeCheckFails("foo > foo")
        assertTypeCheckFails("foo <= foo")
        assertTypeCheckFails("foo >= foo")
    }

    @Test
    fun relationalOperatorsAreNotSupportedForFunctions() {
        env.bind("foo", Type.Function(listOf(Type.String), Type.Int))

        assertTypeCheckFails("foo == foo")
        assertTypeCheckFails("foo != foo")
        assertTypeCheckFails("foo < foo")
        assertTypeCheckFails("foo > foo")
        assertTypeCheckFails("foo <= foo")
        assertTypeCheckFails("foo >= foo")
    }

    private fun assertTypeCheckFails(code: String) {
        assertFailsWith<TypeCheckException> {
            typeCheck(code)
        }
    }

    private fun assertType(expectedType: Type, code: String) {
        assertEquals(expectedType, typeCheck(code).type)
    }

    private fun typeCheck(code: String) =
        parseExpression(code).typeCheck(env)
}
