package siilinkari.translator

import siilinkari.objects.Value

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
    object Call : IR()

    class Push(val value: Value) : IR() {
        override fun toString() = "Push ${value.repr()}"
    }

    class LoadLocal(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadLocal $index ; $name"
    }

    class LoadGlobal(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadGlobal $index ; $name"
    }

    class LoadArgument(val index: Int, val name: String) : IR() {
        override fun toString() = "LoadArgument $index ; $name"
    }

    class StoreLocal(val index: Int, val name: String) : IR() {
        override fun toString() = "StoreLocal $index ; $name"
    }

    class StoreGlobal(val index: Int, val name: String) : IR() {
        override fun toString() = "StoreGlobal $index ; $name"
    }
}
