package siilinkari.vm

import siilinkari.env.Binding
import siilinkari.objects.Value

sealed class OpCode {
    override fun toString() = javaClass.simpleName

    open val isInitialized = true
    open val binding: Binding? = null
    open fun relocate(address: Int) = this

    object Not : OpCode()
    object Add : OpCode()
    object Subtract : OpCode()
    object Multiply : OpCode()
    object Divide : OpCode()
    object Equal : OpCode()
    object ConcatString : OpCode()
    object Pop : OpCode()
    object Call : OpCode()
    object Ret : OpCode()
    object Quit : OpCode()

    class Push(val value: Value) : OpCode() {
        override fun toString() = "Push ${value.repr()}"
    }

    abstract class BindingOpCode(override val binding: Binding) : OpCode()

    class Load(binding: Binding) : BindingOpCode(binding) {
        override fun toString() = "Load $binding"
    }

    class Store(binding: Binding) : BindingOpCode(binding) {
        override fun toString() = "Store $binding"
    }

    abstract class LabeledOpCode(val label: Label) : OpCode() {
        override val isInitialized: Boolean
            get() = label.isInitialized
    }

    class Jump(label: Label) : LabeledOpCode(label) {
        override fun toString() = "Jump $label"
        override fun relocate(address: Int) = Jump(label.relocate(address))
    }

    class JumpIfFalse(label: Label) : LabeledOpCode(label) {
        override fun toString() = "JumpIfFalse $label"
        override fun relocate(address: Int) = JumpIfFalse(label.relocate(address))
    }

    /**
     * Enter a stack frame.
     *
     * Push current `fp`, set new `fp` to `sp` and calculate new `sp = fp + frameSize`.
     */
    class Enter(val frameSize: Int) : OpCode() {
        override fun toString() = "Enter $frameSize"
    }

    /**
     * Leaves a stack frame.
     *
     * Pop return value, restore `fp`, calculate new `sp` so that the arguments
     * pushed to this function are popped and push result and return address to
     * top of stack.
     */
    class Leave(val paramCount: Int) : OpCode() {
        override fun toString() = "Leave $paramCount"
    }
}
