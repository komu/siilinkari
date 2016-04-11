package siilinkari.vm

import siilinkari.objects.Value

sealed class OpCode {
    override fun toString() = javaClass.simpleName

    open fun relocate(baseAddress: Int) = this

    class Not(val target: Int, val source: Int) : OpCode() {
        override fun toString() = "frame[$target] = !frame[$source]"
    }

    abstract class Binary(val name: String, val target: Int, val lhs: Int, val rhs: Int) : OpCode() {
        override fun toString() = "frame[$target] = frame[$lhs] $name frame[$rhs]"

        class Add(t: Int, l: Int, r: Int) : Binary("+", t, l, r)
        class Subtract(t: Int, l: Int, r: Int) : Binary("-", t, l, r)
        class Multiply(t: Int, l: Int, r: Int) : Binary("*", t, l, r)
        class Divide(t: Int, l: Int, r: Int) : Binary("/", t, l, r)
        class Equal(t: Int, l: Int, r: Int) : Binary("==", t, l, r)
        class LessThan(t: Int, l: Int, r: Int) : Binary("<", t, l, r)
        class LessThanOrEqual(t: Int, l: Int, r: Int) : Binary("<=", t, l, r)
        class ConcatString(t: Int, l: Int, r: Int) : Binary("++", t, l, r)
    }

    object Nop : OpCode()

    class Call(val offset: Int, val argumentCount: Int) : OpCode() {
        override fun toString() = "call frame[$offset], $argumentCount"
    }

    class RestoreFrame(val sp: Int) : OpCode() {
        override fun toString() = "restore-frame $sp"
    }

    class Ret(val valuePointer: Int, val returnAddressPointer: Int) : OpCode() {
        override fun toString() = "ret value=frame[$valuePointer], address=frame[$returnAddressPointer]"
    }

    class Copy(val target: Int, val source: Int, val description: String) : OpCode() {
        override fun toString() = "frame[$target] = frame[$source] ; $description"
    }

    class LoadConstant(val target: Int, val value: Value) : OpCode() {
        override fun toString() = "frame[$target] = Constant(${value.repr()})"
    }

    class LoadGlobal(val target: Int, val sourceGlobal: Int, val name: String) : OpCode() {
        override fun toString() = "frame[$target] = global[$sourceGlobal] ; $name"
    }

    class StoreGlobal(val targetGlobal: Int, val source: Int, val name: String) : OpCode() {
        override fun toString() = "global[$targetGlobal] = frame[$source] ; $name"
    }

    class Jump(val address: Int) : OpCode() {
        override fun toString() = "jump $address"
        override fun relocate(baseAddress: Int) = Jump(baseAddress + address)
    }

    class JumpIfFalse(val sp: Int, val address: Int) : OpCode() {
        override fun toString() = "jump-if-false frame[$sp] $address"
        override fun relocate(baseAddress: Int) = JumpIfFalse(sp, baseAddress + address)
    }
}
