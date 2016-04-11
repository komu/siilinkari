package siilinkari.vm

import siilinkari.objects.Value
import java.util.*

/**
 * Encapsulates the state of a single thread of execution.
 *
 * Stack frame layout:
 *
 * ```
 * 0 ...
 * 1 ...
 * 2 ...
 * 3 param2            <- fp - 2 - paramCount
 * 4 param1
 * 5 param0
 * 6 return-address    <- fp - 2
 * 7 old-fp            <- fp - 1
 * 8 local0            <- fp
 * 9 local1
 * 10 stack0
 * 11 stack1
 * 12 stack2
 * 13 ...              <- sp
 * 14 ...
 * ```
 */
class ThreadState {

    private val stack = DataSegment()

    /** Program counter: the next instruction to be executed */
    var pc = 0

    /** Frame pointer */
    var fp = 0

    /** Stack pointer */
    var sp = 0

    /**
     * Accesses data relative to current frame.
     */
    operator fun get(offset: Int): Value =
        stack[fp + offset]

    /**
     * Accesses data relative to current frame.
     */
    operator fun set(offset: Int, value: Value) {
        stack[fp + offset] = value
    }

    /**
     * Returns the value of argument with given index.
     */
    fun loadArgument(index: Int): Value =
        this[- 3 - index]

    /**
     * Pops a value from the stack.
     */
    fun popValue(): Value {
        check(sp != 0)
        return stack[--sp]
    }

    fun dup() {
        val value = popValue()
        push(value)
        push(value)
    }

    inline fun <reified T : Value> pop(): T = popValue() as T

    fun push(value: Value) {
        stack[sp++] = value
    }

    fun popValues(count: Int): List<Value> {
        val values = ArrayList<Value>(count)
        repeat (count) {
            values += popValue()
        }
        return values
    }

    override fun toString() = "  pc = $pc\n  fp = $fp\n  sp = $sp\n  data = $stack"

    inline fun <reified L : Value, reified R : Value> evalBinary(op: (l: L, r: R) -> Value) {
        val rhs = pop<R>()
        val lhs = pop<L>()
        push(op(lhs, rhs))
    }

    fun enterFrame(frameSize: Int) {
        push(Value.Pointer.Data(fp))
        fp = sp
        sp = fp + frameSize
    }

    fun leaveFrame(paramCount: Int) {
        val result = popValue()
        val oldFP = this[-1] as Value.Pointer.Data
        val address = this[-2] as Value.Pointer.Code
        sp = fp - 2 - paramCount
        fp = oldFP.value
        push(result)
        push(address)
    }
}


