package siilinkari.vm

import siilinkari.objects.Value
import java.util.*

/**
 * Runtime stack for values.
 */
class ValueStack {

    private val stack = ArrayList<Value>()

    fun popAny(): Value = stack.removeAt(stack.lastIndex)

    inline fun <reified T : Value> pop(): T = popAny() as T

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

    override fun toString() = stack.toString()
}
