package siilinkari.vm

import siilinkari.env.Binding
import java.util.*

/**
 * A serial of opcodes, addressable with indices 0..lastValidAddress.
 */
class CodeSegment private constructor(private val opCodes: List<OpCode>) {

    /**
     * Returns [OpCode] with given address.
     */
    operator fun get(address: Int): OpCode = opCodes[address]

    override fun toString(): String =
        opCodes.asSequence().mapIndexed { i, op -> "$i $op" }.joinToString("\n")

    /**
     * Builder for building opcodes.
     */
    class Builder() {
        private val opCodes = ArrayList<OpCode>()

        constructor(builder: Builder): this() {
            addRelocated(builder)
        }

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
         * Adds instructions of [segment] to this builder, relocating all labels
         * as needed.
         */
        fun addRelocated(segment: CodeSegment.Builder): Int {
            val address = opCodes.size

            for (op in segment.opCodes)
                opCodes += op.relocate(address)

            return address
        }

        /**
         * Calculates the frame size needed by opcodes in this segment.
         */
        val frameSize: Int
            get() = opCodes.localBindings().maxBy { it.index }?.index?.let { it + 1 } ?: 0

        /**
         * Builds the segment.
         */
        fun build(): CodeSegment {
            assert(opCodes.all { it.isInitialized }) { "uninitialized opcodes"}
            return CodeSegment(opCodes)
        }

        override fun toString(): String =
            opCodes.asSequence().mapIndexed { i, op -> "$i $op" }.joinToString("\n")
    }
}

private fun List<OpCode>.localBindings(): Sequence<Binding.Local> =
    asSequence().map { it.binding }.filterIsInstance<Binding.Local>()
