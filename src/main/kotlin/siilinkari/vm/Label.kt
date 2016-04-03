package siilinkari.vm

/**
 * Labels allow referring to an address in source code before the actual
 * address is resolved.
 *
 * Consider translating an if-statement: we'd like to generate code like:
 *
 * ```
 * 0 op1
 * 1 JumpIfFalse 5
 * 2 op2
 * 3 op2
 * 4 op3
 * 5 op-after-if
 * ```
 *
 * However, at the point when we are generating code for [OpCode.JumpIfFalse],
 * we don't know how many opcodes there are going to be between it and `op-after-if`
 * and therefore can't calculate the address directly. Labels solve this problem:
 * we can create an uninitialized label, pass it to opcode needing an address and
 * later add the label to code, which causes the address of the label to be resolved.
 */
class Label {

    private var _address: Int? = null

    /**
     * Address of the label.
     *
     * Initially the address in uninitialized state and can't be queries.
     * Once it is set to valid value, it can't be modified anymore.
     */
    var address: Int
        get() = _address ?: throw IllegalStateException("address of label is not initialized")
        set(address) {
            check(_address == null) { "tried to reinitialize a label"}
            require(address >= 0) { "invalid address $address"}
            _address = address
        }

    override fun toString() = "[Label $_address]"

    /**
     * Returns true if address is initialized.
     */
    val isInitialized: Boolean
        get() = _address != null

    fun relocate(address: Int): Label {
        val label = Label()
        label.address = this.address + address
        return label
    }
}
