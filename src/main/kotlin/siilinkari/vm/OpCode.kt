package siilinkari.vm

import siilinkari.objects.Value

sealed class OpCode {
    override fun toString() = javaClass.simpleName

    open val isInitialized = true

    object Not : OpCode()
    object Add : OpCode()
    object Subtract : OpCode()
    object Equal : OpCode()
    object Pop : OpCode()

    class Push(val value: Value) : OpCode() {
        override fun toString() = "Push ${value.repr()}"
    }

    class Load(val variable: String) : OpCode() {
        override fun toString() = "Load $variable"
    }

    class Bind(val variable: String) : OpCode() {
        override fun toString() = "Bind $variable"
    }

    class Store(val variable: String) : OpCode() {
        override fun toString() = "Store $variable"
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
