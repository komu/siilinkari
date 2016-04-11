package siilinkari.translator

import siilinkari.objects.Value
import java.util.Objects.hash

/**
 * Intermediate representation that is quite close to executed opcodes,
 * but leaves things like addresses and labels still abstract.
 */
sealed class IR {
    override fun toString() = javaClass.simpleName

    object Not : IR()
    object Add : IR()
    object Subtract : IR()
    object Multiply : IR()
    object Divide : IR()
    object Equal : IR()
    object LessThan : IR()
    object LessThanOrEqual : IR()
    object ConcatString : IR()
    object Pop : IR()
    object Dup : IR()
    object Call : IR()
    object Ret : IR()
    object Quit : IR()

    class Enter(val frameSize: Int) : IR()
    class Leave(val paramCount: Int) : IR()

    class Push(val value: Value) : IR() {
        override fun toString() = "Push ${value.repr()}"
        override fun equals(other: Any?) = other is Push && value == other.value
        override fun hashCode() = value.hashCode()
    }

    class LoadLocal(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadLocal $index ; $name"
        override fun equals(other: Any?) = other is LoadLocal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class LoadGlobal(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadGlobal $index ; $name"
        override fun equals(other: Any?) = other is LoadGlobal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class LoadArgument(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadArgument $index ; $name"
        override fun equals(other: Any?) = other is LoadArgument && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class StoreLocal(val index: Int, val name: String) : IR() {
        override fun toString() = "StoreLocal $index ; $name"
        override fun equals(other: Any?) = other is StoreLocal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }

    class StoreGlobal(val index: Int, val name: String) : IR() {
        override fun toString() = "StoreGlobal $index ; $name"
        override fun equals(other: Any?) = other is StoreGlobal && index == other.index && name == other.name
        override fun hashCode() = hash(index, name)
    }
}
