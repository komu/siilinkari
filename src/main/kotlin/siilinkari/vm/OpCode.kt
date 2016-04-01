package siilinkari.vm

import siilinkari.objects.Value

sealed class OpCode {
    override fun toString() = javaClass.simpleName

    object Not : OpCode()
    object Add : OpCode()
    object Subtract : OpCode()
    object Equal : OpCode()
    object Pop : OpCode()

    class Push(val value: Value) : OpCode() {
        override fun toString() = "Push $value"
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

    class Jump(val label: Label) : OpCode() {
        override fun toString() = "Jump $label"
    }

    class JumpIfFalse(val label: Label) : OpCode() {
        override fun toString() = "JumpIfFalse $label"
    }
}
