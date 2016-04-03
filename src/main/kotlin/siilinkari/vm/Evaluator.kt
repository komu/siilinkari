package siilinkari.vm

import siilinkari.env.Binding
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.objects.Value
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.translator.translate
import siilinkari.types.Type
import siilinkari.types.TypeChecker
import siilinkari.types.TypedStatement
import siilinkari.types.type
import java.util.*

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator {
    private val environment = GlobalEnvironment()
    private val typeEnvironment = GlobalStaticEnvironment()
    private val typeChecker = TypeChecker(typeEnvironment)

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value) {
        typeEnvironment.bind(name, value.type)
        environment[name] = value
    }

    /**
     * Binds a global function using given expression as body.
     */
    fun bindFunction(name: String, args: List<Pair<String, Type>>, code: String) {
        bind(name, createFunctionFromExpression(args, code))
    }

    /**
     * Evaluates statement of code. If the statement is an expression
     * statement, returns its value. Otherwise returns [Value.Unit].
     */
    fun evaluateStatement(code: String): Value {
        val translated = translate(code)

        return evaluateSegment(translated)
    }

    /**
     * Evaluates an expression and returns its value.
     */
    fun evaluateExpression(code: String): Value {
        val exp = parseExpression(code)
        val typedExp = typeChecker.typeCheck(exp)
        val translated = typedExp.translate()

        return evaluateSegment(translated)
    }

    /**
     * Translates given code to opcodes and returns string representation of the opcodes.
     */
    fun dump(code: String): String =
        translate(code).toString()

    /**
     * Translates code to opcodes.
     */
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

    /**
     * Creates a callable function from given expression.
     */
    private fun createFunctionFromExpression(args: List<Pair<String, Type>>, code: String): Value.Function {
        val exp = parseExpression(code)

        // The function body needs to be analyzed in a new scope that contains
        // bindings for all arguments.
        val scope = typeEnvironment.newScope()
        val bindings = args.map {
            val (name, type) = it
            scope.bind(name, type) as Binding.Local
        }

        val typedExp = TypeChecker(scope).typeCheck(exp)
        val segment = typedExp.translate()

        return object : Value.Function(Type.Function(args.map { it.second }, typedExp.type)) {
            override fun invoke(args: List<Value>): Value {
                val frame = Frame(segment.frameSize)
                args.forEachIndexed { i, arg ->
                    frame[bindings[i].index] = arg
                }
                return evaluateSegment(segment, frame)
            }
        }
    }

    /**
     * Evaluates given code segment. If the segment leaves a value on the stack
     * (if it was compiled from an expression instead of statement), returns the
     * value. Otherwise returns [Value.Unit].
     */
    private fun evaluateSegment(code: CodeSegment, frame: Frame = Frame(code.frameSize)): Value {
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
                OpCode.Multiply -> {
                    val rhs = stack.pop<Value.Integer>()
                    val lhs = stack.pop<Value.Integer>()
                    stack.push(lhs * rhs)
                }
                OpCode.Divide -> {
                    val rhs = stack.pop<Value.Integer>()
                    val lhs = stack.pop<Value.Integer>()
                    stack.push(lhs / rhs)
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
                    stack.push(when (op.binding) {
                        is Binding.Local -> frame[op.binding.index]
                        is Binding.Global -> environment[op.binding.name]
                    })
                is OpCode.Store -> {
                    val value = stack.pop<Value>()
                    when (op.binding) {
                        is Binding.Local -> frame[op.binding.index] = value
                        is Binding.Global -> environment[op.binding.name] = value
                        else -> error("unknown binding ${op.binding}")
                    }
                }
                is OpCode.Jump ->
                    pc = op.label.address
                is OpCode.JumpIfFalse ->
                    if (!stack.pop<Value.Bool>().value)
                        pc = op.label.address
                is OpCode.Call -> {
                    val func = stack.pop<Value.Function>()
                    val args = ArrayList<Value>()
                    repeat(func.argumentCount) {
                        args += stack.popAny()
                    }

                    stack.push(func(args))
                }
                else ->
                    error("unknown opcode: $op")
            }
        }

        return stack.topOrNull() ?: Value.Unit
    }
}
