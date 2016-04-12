package siilinkari.vm

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

        val currentAddress: Int
            get() = opCodes.size

        /**
         * Adds instructions of [builder] to this builder, relocating all labels
         * as needed.
         */
        fun addRelocated(builder: Builder): Int = addRelocated(builder.opCodes)

        /**
         * Adds instructions of [segment] to this builder, relocating all labels
         * as needed.
         */
        fun addRelocated(segment: CodeSegment) = addRelocated(segment.opCodes)

        private fun addRelocated(ops: List<OpCode>): Int {
            val address = opCodes.size

            for (op in ops)
                opCodes += op.relocate(address)

            return address
        }

        /**
         * Builds the segment.
         */
        fun build(): CodeSegment {
            return CodeSegment(opCodes)
        }

        override fun toString(): String =
            opCodes.asSequence().mapIndexed { i, op -> "$i $op" }.joinToString("\n")
    }
}
