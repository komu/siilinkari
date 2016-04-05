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
import java.util.*

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator {
    private val data = DataSegment()
    private val globalTypeEnvironment = GlobalStaticEnvironment()
    private val globalCode = CodeSegment.Builder()

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value) {
        val binding = globalTypeEnvironment.bind(name, value.type)
        data[binding.index] = value
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
        val typedExp = exp.typeCheck(globalTypeEnvironment)
        val translated = CodeSegment.Builder()
        typedExp.translateTo(translated)
        translated += OpCode.Ret

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
    private fun translate(code: String): CodeSegment.Builder {
        val stmt = parseStatement(code)
        val typedStmt = stmt.typeCheck(globalTypeEnvironment)

        val translated = CodeSegment.Builder()
        if (typedStmt is TypedStatement.Exp) {
            typedStmt.expression.translateTo(translated)
        } else {
            typedStmt.translateTo(translated)
        }
        translated += OpCode.Ret
        return translated
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
        val scope = globalTypeEnvironment.newScope()
        val codeSegment = CodeSegment.Builder()
        for ((name, type) in args) {
            val binding = scope.bind(name, type)
            codeSegment += OpCode.Store(binding)
        }

        val typedExp = exp.typeCheck(scope)

        typedExp.translateTo(codeSegment)

        val argTypes = args.map { it.second }
        val signature = Type.Function(argTypes, typedExp.type)

        val finalCode = CodeSegment.Builder()
        val frameSize = codeSegment.frameSize
        finalCode += OpCode.Enter(frameSize)
        finalCode.addRelocated(codeSegment)
        finalCode += OpCode.Leave(frameSize)
        finalCode += OpCode.Ret

        val address = globalCode.addRelocated(finalCode)

        return Value.Function.Compound(signature, address)
    }

    /**
     * Evaluates given code segment.
     */
    private fun evaluateSegment(segment: CodeSegment.Builder): Value {
        // Relocate code to be evaluated after the global code into a single segment.
        val codeBuilder = CodeSegment.Builder(globalCode)
        val startAddress = codeBuilder.addRelocated(segment)
        val code = codeBuilder.build()

        val stack = ValueStack()
        var framePointer = codeBuilder.frameSize
        var pc = startAddress
        val pcStack = ArrayList<Int>()

        fun Binding.address(): Int = when (this) {
            is Binding.Local  -> framePointer - index - 1
            is Binding.Global -> index
        }

        evalLoop@while (true) {
            val op = code[pc++]
            when (op) {
                OpCode.Ret ->
                    if (pcStack.isEmpty())
                        break@evalLoop
                    else
                        pc = pcStack.removeAt(pcStack.lastIndex)
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
                    stack.push(data[op.binding.address()])
                is OpCode.Store ->
                    data[op.binding.address()] = stack.pop<Value>()
                is OpCode.Jump ->
                    pc = op.label.address
                is OpCode.JumpIfFalse ->
                    if (!stack.pop<Value.Bool>().value)
                        pc = op.label.address
                is OpCode.Call -> {
                    val func = stack.pop<Value.Function>()

                    when (func) {
                        is Value.Function.Compound -> {
                            pcStack.add(pc)
                            pc = func.address
                        }
                        is Value.Function.Native ->
                            stack.push(func(stack.popValues(func.argumentCount)))
                    }
                }
                is OpCode.Enter ->
                    framePointer += op.frameSize
                is OpCode.Leave ->
                    framePointer -= op.frameSize
                else ->
                    error("unknown opcode: $op")
            }
        }

        return stack.topOrNull() ?: Value.Unit
    }
}
