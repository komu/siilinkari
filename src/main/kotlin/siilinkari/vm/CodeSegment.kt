package siilinkari.vm

import siilinkari.env.Binding
import java.util.*

/**
 * A serial of opcodes, addressable with indices 0..lastValidAddress.
 */
class CodeSegment private constructor(private val opCodes: List<OpCode>, val frameSize: Int) {

    /**
     * Returns [OpCode] with given address.
     */
    operator fun get(address: Int): OpCode = opCodes[address]

    /**
     * Returns the last valid address or -1, if there are no opcodes.
     */
    val lastAddress: Int
        get() = opCodes.lastIndex

    override fun toString(): String =
        opCodes.mapIndexed { i, op -> "$i $op" }.joinToString("\n")

    /**
     * Builder for building opcodes.
     */
    class Builder {
        private val opCodes = ArrayList<OpCode>()

        /**
         * Adds a new opcode.
         */
        operator fun plusAssign(op: OpCode) {
            opCodes += op
        }

        /**
         * Adds a new label. This initializes the label to the current address.
         */
        operator fun plusAssign(label: Label) {
            label.address = opCodes.size
        }

        /**
         * Builds the segment.
         */
        fun build(): CodeSegment {
            val maxBindingIndex = opCodes.localBindings().maxBy { it.index  }?.index ?: -1
            assert(opCodes.all { it.isInitialized }) { "uninitialized opcodes"}
            return CodeSegment(opCodes, maxBindingIndex + 1)
        }
    }
}

private fun List<OpCode>.localBindings(): Sequence<Binding.Local> =
    asSequence().map { it.binding }.filterIsInstance<Binding.Local>()
