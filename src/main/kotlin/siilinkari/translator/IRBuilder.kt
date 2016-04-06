package siilinkari.translator

import siilinkari.vm.CodeSegment
import siilinkari.vm.Label
import java.util.*

/**
 * Builder for building IR-stream.
 */
class IRBuilder() {
    private val opCodes = ArrayList<IR>()

    /**
     * Adds a new opcode.
     */
    operator fun plusAssign(op: IR) {
        opCodes += op
    }

    /**
     * Adds a new label. This initializes the label to the current address.
     */
    operator fun plusAssign(label: Label) {
        label.address = opCodes.size
    }

    fun buildTo(code: CodeSegment.Builder) {
        for (op in opCodes)
            code += op.translate()
    }
}
