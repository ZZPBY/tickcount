package io.github.zzpby.tickcount.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reading a widget's binding out of whatever the preferences happen to hold.
 *
 * The value that matters is the one an older release left there: the countdown's date as a
 * long, under the key that now holds its id as a string. Asking `SharedPreferences` for a
 * string throws on it, and a widget draw swallows `RuntimeException` — so the widget was
 * simply never drawn again, and only clearing the app's data ever brought it back.
 */
class WidgetBindingTest {

    @Test
    fun `the id a widget was configured with is read back`() {
        assertEquals("5d2f9a", storedWidgetEventId("5d2f9a"))
    }

    @Test
    fun `a date written by an older release is not a binding`() {
        // 1.0.4 to 1.0.6 stored the countdown's epoch day here, as a long.
        assertNull(storedWidgetEventId(20_000L))
        assertNull(storedWidgetEventId(0L))
    }

    @Test
    fun `anything else that is not a string is not a binding`() {
        assertNull(storedWidgetEventId(true))
        assertNull(storedWidgetEventId(1.5f))
        assertNull(storedWidgetEventId(null))
    }

    @Test
    fun `an empty string is not a binding`() {
        assertNull(storedWidgetEventId(""))
    }
}
