package siilinkari.vm

import siilinkari.objects.Value
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.translator.translate
import siilinkari.types.TypeChecker
import siilinkari.types.TypeEnvironment
import siilinkari.types.TypedStatement
import siilinkari.types.type

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator() {
    private val environment = Environment()
    private val typeEnvironment = TypeEnvironment()
    private val typeChecker = TypeChecker(typeEnvironment)

    fun bind(name: String, value: Value) {
        typeEnvironment.bind(name, value.type)
        environment.bind(name, value)
    }

    fun evaluateStatement(code: String): Value? {
        val translated = translate(code)

        return evaluateSegment(translated)
    }

    fun dump(code: String): String =
        translate(code).toString()

    private fun translate(code: String): CodeSegment {
        val stmt = parseStatement(code)
        val typedStmt = typeChecker.typeCheck(stmt)

        val translated = if (typedStmt is TypedStatement.Exp) {
            typedStmt.expression.translate()
        } else {
            typedStmt.translate()
        }
        return translated
    }

    fun evaluateExpression(code: String): Value {
        val exp = parseExpression(code)
        val typedExp = typeChecker.typeCheck(exp)
        val translated = typedExp.translate()

        return evaluateSegment(translated)!!
    }

    private fun evaluateSegment(code: CodeSegment): Value? {
        val stack = ValueStack()
        var pc = 0
        val end = code.lastAddress + 1

        while (pc != end) {
            val op = code[pc++]
            when (op) {
                OpCode.Pop ->
                    stack.pop<Value>()
                OpCode.Not ->
                    stack.push(!stack.pop<Value.Bool>())
                OpCode.Add -> {
                    val rhs = stack.pop<Value.Integer>()
                    val lhs = stack.pop<Value.Integer>()
                    stack.push(lhs + rhs)
                }
                OpCode.Subtract -> {
                    val rhs = stack.pop<Value.Integer>()
                    val lhs = stack.pop<Value.Integer>()
                    stack.push(lhs - rhs)
                }
                OpCode.Equal -> {
                    val rhs = stack.pop<Value>()
                    val lhs = stack.pop<Value>()
                    stack.push(Value.Bool(lhs == rhs))
                }
                OpCode.ConcatString -> {
                    val rhs = stack.pop<Value>()
                    val lhs = stack.pop<Value.String>()
                    stack.push(lhs + rhs)
                }
                is OpCode.Push ->
                    stack.push(op.value)
                is OpCode.Load ->
                    stack.push(environment[op.variable])
                is OpCode.Store ->
                    environment[op.variable] = stack.pop<Value>()
                is OpCode.Bind ->
                    environment.bind(op.variable, stack.pop<Value>())
                is OpCode.Jump ->
                    pc = op.label.address
                is OpCode.JumpIfFalse ->
                    if (!stack.pop<Value.Bool>().value)
                        pc = op.label.address
                else ->
                    error("unknown opcode: $op")
            }
        }

        return stack.topOrNull()
    }
}
