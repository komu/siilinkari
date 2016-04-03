package siilinkari.vm

import siilinkari.env.Binding
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.objects.Value
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.translator.translateTo
import siilinkari.types.Type
import siilinkari.types.TypedStatement
import siilinkari.types.type
import siilinkari.types.typeCheck

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator {
    private val environment = GlobalEnvironment()
    private val typeEnvironment = GlobalStaticEnvironment()
    private val stack = ValueStack()

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value) {
        val binding = typeEnvironment.bind(name, value.type)
        environment[binding.index] = value
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

        evaluateSegment(translated)

        return stack.topOrNull() ?: Value.Unit
    }

    /**
     * Evaluates an expression and returns its value.
     */
    fun evaluateExpression(code: String): Value {
        val exp = parseExpression(code)
        val typedExp = exp.typeCheck(typeEnvironment)
        val translated = CodeSegment.Builder()
        typedExp.translateTo(translated)
        translated += OpCode.Ret

        evaluateSegment(translated.build())
        return stack.topOrNull() ?: Value.Unit
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
        val typedStmt = stmt.typeCheck(typeEnvironment)

        val translated = CodeSegment.Builder()
        if (typedStmt is TypedStatement.Exp) {
            typedStmt.expression.translateTo(translated)
        } else {
            typedStmt.translateTo(translated)
        }
        translated += OpCode.Ret
        return translated.build()
    }

    /**
     * Creates a callable function from given expression.
     */
    private fun createFunctionFromExpression(args: List<Pair<String, Type>>, code: String): Value.Function {
        val exp = parseExpression(code)

        // The function body needs to be analyzed in a new scope that contains
        // bindings for all arguments. Create a new scope and at the same time
        // create prepare a function prologue that pops all arguments from the
        // stack into local variables.
        val scope = typeEnvironment.newScope()
        val codeSegment = CodeSegment.Builder()
        for ((name, type) in args) {
            val binding = scope.bind(name, type)
            codeSegment += OpCode.Store(binding)
        }

        val typedExp = exp.typeCheck(scope)

        typedExp.translateTo(codeSegment)
        codeSegment += OpCode.Ret

        val argTypes = args.map { it.second }
        val signature = Type.Function(argTypes, typedExp.type)

        return Value.Function.Compound(signature, codeSegment.build())
    }

    /**
     * Evaluates given code segment.
     */
    private fun evaluateSegment(code: CodeSegment) {
        val frame = Frame(code.frameSize)
        var pc = 0

        while (true) {
            val op = code[pc++]
            when (op) {
                OpCode.Ret ->
                    return
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
                        is Binding.Global -> environment[op.binding.index]
                    })
                is OpCode.Store -> {
                    val value = stack.pop<Value>()
                    when (op.binding) {
                        is Binding.Local -> frame[op.binding.index] = value
                        is Binding.Global -> environment[op.binding.index] = value
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

                    when (func) {
                        is Value.Function.Compound ->
                            evaluateSegment(func.code)
                        is Value.Function.Native ->
                            stack.push(func(stack.popValues(func.argumentCount)))
                    }
                }
                else ->
                    error("unknown opcode: $op")
            }
        }
    }
}
