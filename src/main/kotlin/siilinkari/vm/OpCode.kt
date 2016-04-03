package siilinkari.vm

import siilinkari.env.Binding
import siilinkari.objects.Value

sealed class OpCode {
    override fun toString() = javaClass.simpleName

    open val isInitialized = true
    open val binding: Binding? = null

    object Not : OpCode()
    object Add : OpCode()
    object Subtract : OpCode()
    object Multiply : OpCode()
    object Divide : OpCode()
    object Equal : OpCode()
    object ConcatString : OpCode()
    object Pop : OpCode()
    object Call : OpCode()

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
    }

    class JumpIfFalse(label: Label) : LabeledOpCode(label) {
        override fun toString() = "JumpIfFalse $label"
    }
}
