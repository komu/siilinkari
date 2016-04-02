package siilinkari.vm

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabelTest {

    @Test
    fun settingLabelAddress() {
        val label = Label()

        assertFalse(label.isInitialized)

        label.address = 42

        assertTrue(label.isInitialized)
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
