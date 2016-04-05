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
    private val globalData = DataSegment()
    private val globalTypeEnvironment = GlobalStaticEnvironment()
    private val globalCode = CodeSegment.Builder()

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value) {
        val binding = globalTypeEnvironment.bind(name, value.type)
        globalData[binding.index] = value
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
        val topFramePointer = codeBuilder.frameSize
        val code = codeBuilder.build()

        val state = ThreadState()
        state.fp = topFramePointer
        state.pc = startAddress

        evalLoop@while (true) {
            val op = code[state.pc++]
            when (op) {
                OpCode.Pop              -> state.popValue()
                OpCode.Not              -> state.push(!state.pop<Value.Bool>())
                OpCode.Add              -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l + r }
                OpCode.Subtract         -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l - r }
                OpCode.Multiply         -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l * r }
                OpCode.Divide           -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l / r }
                OpCode.Equal            -> state.evalBinary<Value, Value> { l, r -> Value.Bool(l == r) }
                OpCode.ConcatString     -> state.evalBinary<Value.String, Value> { l, r -> l + r }
                is OpCode.Push          -> state.push(op.value)
                is OpCode.Jump          -> state.pc = op.label.address
                is OpCode.JumpIfFalse   -> if (!state.pop<Value.Bool>().value) state.pc = op.label.address
                is OpCode.Load          -> state.push(when (op.binding) {
                    is Binding.Local  -> state.data[op.binding.index]
                    is Binding.Global -> globalData[op.binding.index]
                })
                is OpCode.Store         -> when (op.binding) {
                    is Binding.Local  -> state.data[op.binding.index] = state.popValue()
                    is Binding.Global -> globalData[op.binding.index] = state.popValue()
                }
                is OpCode.Enter         -> state.fp += op.frameSize
                is OpCode.Leave         -> state.fp -= op.frameSize
                is OpCode.Call -> {
                    val func = state.pop<Value.Function>()

                    when (func) {
                        is Value.Function.Compound -> {
                            state.data[state.fp++] = Value.Pointer.Data(state.pc)
                            state.pc = func.address
                        }
                        is Value.Function.Native ->
                            state.push(func(state.popValues(func.argumentCount)))
                    }
                }
                OpCode.Ret -> {
                    if (state.fp == topFramePointer)
                        break@evalLoop
                    else
                        state.pc = (state.data[--state.fp] as Value.Pointer.Data).value
                }
                else ->
                    error("unknown opcode: $op")
            }
        }

        return state.topOrNull() ?: Value.Unit
    }

    /**
     * Encapsulates the state of a single thread of execution.
     */
    inner class ThreadState {
        private val stack = ArrayList<Value>()

        val data = DataSegment()

        /** Program counter: the next instruction to be executed */
        var pc = 0

        /** Frame pointer */
        var fp = 0

        operator fun get(offset: Int): Value = data[translateAddress(offset)]

        operator fun set(offset: Int, value: Value) = {
            data[translateAddress(offset)] = value
        }

        private fun translateAddress(offset: Int): Int = fp - offset - 1

        fun popValue(): Value = stack.removeAt(stack.lastIndex)

        inline fun <reified T : Value> pop(): T = popValue() as T

        fun push(value: Value) {
            stack.add(value)
        }

        fun topOrNull(): Value? = stack.lastOrNull()

        fun popValues(count: Int): List<Value> {
            val removed = stack.subList(stack.size - count, stack.size)
            val values = ArrayList<Value>(removed.asReversed())
            removed.clear()
            return values
        }

        override fun toString() = "  pc = $pc\n  fp = $fp\n  stack = ${stack.asReversed().joinToString(", ")}"

        inline fun <reified L : Value, reified R : Value> evalBinary(op: (l: L, r: R) -> Value) {
            val rhs = pop<R>()
            val lhs = pop<L>()
            push(op(lhs, rhs))
        }
    }
}
