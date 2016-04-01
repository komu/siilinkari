package siilinkari.vm

class Label {

    var address: Int = -1
        get() {
            check(field != -1) { "address of label is not initialized" }
            return field
        }
        set(address) {
            check(field == -1) { "tried to reinitialize a label"}
            require(address >= 0) { "invalid address $address"}
            field = address
        }

    override fun toString() = "[Label $address]"
}
