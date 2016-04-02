package siilinkari.types

import org.junit.Test
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TypeCheckerTest {

    val env = TypeEnvironment()
    val typeChecker = TypeChecker(env)

    @Test
    fun literalTypes() {
        assertExpressionType(Type.String, "\"foo\"")
        assertExpressionType(Type.Int, "123")
        assertExpressionType(Type.Boolean, "true")
    }

    @Test
    fun boundVariableTypes() {
        env.bind("s", Type.String)
        env.bind("b", Type.Boolean)

        assertExpressionType(Type.String, "s")
        assertExpressionType(Type.Boolean, "b")
    }

    @Test
    fun not() {
        assertExpressionType(Type.Boolean, "!true")
        assertExpressionTypeCheckFails("!1")
    }

    @Test
    fun equalityComparison() {
        assertExpressionType(Type.Boolean, "true == false")
        assertExpressionType(Type.Boolean, "1 == 1")
        assertExpressionType(Type.Boolean, "\"foo\" == \"bar\"")

        assertExpressionType(Type.Boolean, "true != false")
        assertExpressionType(Type.Boolean, "1 != 1")
        assertExpressionType(Type.Boolean, "\"foo\" != \"bar\"")

        assertExpressionTypeCheckFails("true == 1")
        assertExpressionTypeCheckFails("true != 1")
    }

    @Test
    fun numericOperators() {
        assertExpressionType(Type.Int, "1 + 1")
        assertExpressionType(Type.Int, "1 - 1")

        assertExpressionTypeCheckFails("1 + true")
        assertExpressionTypeCheckFails("true + 1")
        assertExpressionTypeCheckFails("true + true")
        assertExpressionTypeCheckFails("1 + \"foo\"")
        assertExpressionTypeCheckFails("true + \"foo\"")

        assertExpressionTypeCheckFails("1 - true")
        assertExpressionTypeCheckFails("true - 1")
        assertExpressionTypeCheckFails("true - true")
        assertExpressionTypeCheckFails("\"foo\" - \"bar\"")
    }

    @Test
    fun plusWithStringLiteral() {
        assertExpressionType(Type.String, "\"foo\" + \"bar\"")
        assertExpressionType(Type.String, "\"foo\" + 42")
        assertExpressionType(Type.String, "\"foo\" + true")
    }

    @Test
    fun unboundVariables() {
        assertExpressionTypeCheckFails("x")
        assertStatementTypeCheckFails("x = 4;")
    }

    @Test
    fun evaluationFailsForRebindingVariables() {
        assertStatementTypeCheckFails("{ var x = 4; var x = 4; }")
    }

    @Test
    fun unboundVariableType() {
        assertExpressionTypeCheckFails("s")
    }

    private fun assertExpressionTypeCheckFails(code: String) {
        assertFailsWith<TypeCheckException> {
            typeCheckExpression(code)
        }
    }

    private fun assertStatementTypeCheckFails(code: String) {
        assertFailsWith<TypeCheckException> {
            typeCheckStatement(code)
        }
    }

    private fun assertExpressionType(expectedType: Type, code: String) {
        assertEquals(expectedType, typeCheckExpression(code).type)
    }

    private fun typeCheckExpression(code: String) =
        typeChecker.typeCheck(parseExpression(code))

    private fun typeCheckStatement(code: String) =
        typeChecker.typeCheck(parseStatement(code))

}
