package siilinkari.vm

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class LabelTest {

    @Test
    fun settingLabelAddress() {
        val label = Label()
        label.address = 42
        assertEquals(42, label.address)
    }

    @Test
    fun tryingToResetAddressIsNotAllowed() {
        val label = Label()
        label.address = 42

        assertFails {
            label.address = 43
        }
    }

    @Test
    fun tryingToReadUninitializedAddressIsNotAllowed() {
        val label = Label()

        assertFails {
            label.address
        }
    }

}
