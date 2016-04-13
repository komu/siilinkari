package siilinkari.vm

import siilinkari.objects.Value
import java.util.*

/**
 * Encapsulates the state of a single thread of execution.
 */
class ThreadState {

    private val stack = DataSegment()

    /** Program counter: the next instruction to be executed */
    var pc = 0

    /** Frame pointer */
    var fp = 0

    /**
     * Accesses data relative to current frame.
     */
    operator fun get(offset: Int): Value =
        stack[fp, offset]

    /**
     * Accesses data relative to current frame.
     */
    operator fun set(offset: Int, value: Value) {
        stack[fp + offset] = value
    }

    /**
     * Returns the arguments from current stack-frame.
     */
    fun getArgs(count: Int): List<Value> {
        val values = ArrayList<Value>(count)

        for (i in 0..(count-1))
            values += this[i]

        return values
    }

    override fun toString() = "  pc = $pc\n  fp = $fp\n  data = $stack"

    inline fun <reified T : Value, reified L : Value, reified R : Value> evalBinary(op: OpCode.Binary<L, R, T>, f: (L, R) -> T) {
        this[op.target] = f(this[op.lhs] as L, this[op.rhs] as R)
    }

    inline fun <reified L : Value, reified R : Value> evalBinaryBool(op: OpCode.Binary<L, R, Value.Bool>, f: (L, R) -> Boolean) {
        this[op.target] = Value.Bool(f(this[op.lhs] as L, this[op.rhs] as R))
    }
}
