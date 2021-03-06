package siilinkari.translator

import siilinkari.objects.Value
import java.util.Objects.hash

/**
 * Intermediate representation that is quite close to executed opcodes,
 * but leaves things like addresses and labels still abstract.
 */
sealed class IR(val stackDelta: Int) {
    override fun toString() = javaClass.simpleName

    object Not : IR(0)
    object Add : IR(-1)
    object Subtract : IR(-1)
    object Multiply : IR(-1)
    object Divide : IR(-1)
    object Equal : IR(-1)
    object LessThan : IR(-1)
    object LessThanOrEqual : IR(-1)
    object ConcatString : IR(-1)
    object Pop : IR(-1)
    object Dup : IR(1)
    class Call(val argumentCount: Int) : IR(-argumentCount)
    object RestoreFrame : IR(0)
    object Ret : IR(-1)
    object PushUnit : IR(1)

    class Push(val value: Value) : IR(1) {
        override fun toString() = "Push ${value.repr()}"
        override fun equals(other: Any?) = other is Push && value == other.value
        override fun hashCode() = value.hashCode()
    }

    interface LocalFrameIR {
        val localFrameOffset: Int
    }

    class LoadLocal(val index: Int, val name: String) : IR(1), LocalFrameIR {
        override fun toString() = "LoadLocal $index ; $name"
        override fun equals(other: Any?) = other is LoadLocal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
        override val localFrameOffset: Int = index
    }

    class LoadGlobal(val index: Int, val name: String) : IR(1) {
        override fun toString() = "LoadGlobal $index ; $name"
        override fun equals(other: Any?) = other is LoadGlobal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class LoadArgument(val index: Int, val name: String) : IR(1) {
        override fun toString() = "LoadArgument $index ; $name"
        override fun equals(other: Any?) = other is LoadArgument && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class StoreLocal(val index: Int, val name: String) : IR(-1), LocalFrameIR {
        override fun toString() = "StoreLocal $index ; $name"
        override fun equals(other: Any?) = other is StoreLocal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
        override val localFrameOffset: Int = index
    }

    class StoreGlobal(val index: Int, val name: String) : IR(-1) {
        override fun toString() = "StoreGlobal $index ; $name"
        override fun equals(other: Any?) = other is StoreGlobal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }
}
