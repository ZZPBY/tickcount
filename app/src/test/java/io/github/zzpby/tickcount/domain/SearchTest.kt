package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * What the search box looks at.
 *
 * The scope matters as much as the query: a box set to "date" must not match a
 * countdown merely because its name happens to contain the same digits, which is
 * the whole reason the scope exists.
 */
class SearchTest {

    private val event = CountdownEvent(
        id = "e",
        date = LocalDate.of(2026, 10, 16),
        title = "项目评审",
        time = LocalTime.of(9, 30),
        tags = listOf("工作", "重要"),
    )

    private fun text(scope: SearchScope) = searchText(
        event = event,
        scope = scope,
        dateText = "2026年10月16日 星期五",
        timeText = "09:30",
    )

    // ------------------------------------------------------------------ scopes

    @Test
    fun `the name scope looks at the title`() {
        assertTrue(matchesQuery(text(SearchScope.NAME), "评审"))
    }

    @Test
    fun `the name scope looks at the tags too`() {
        assertTrue(matchesQuery(text(SearchScope.NAME), "工作"))
        assertTrue(matchesQuery(text(SearchScope.NAME), "重要"))
    }

    @Test
    fun `the date scope looks at the formatted date`() {
        assertTrue(matchesQuery(text(SearchScope.DATE), "10月"))
        assertTrue(matchesQuery(text(SearchScope.DATE), "星期五"))
        assertTrue(matchesQuery(text(SearchScope.DATE), "2026"))
    }

    @Test
    fun `the time scope looks at the time`() {
        assertTrue(matchesQuery(text(SearchScope.TIME), "09:30"))
        assertTrue(matchesQuery(text(SearchScope.TIME), "9:3"))
    }

    @Test
    fun `a scope does not answer for another scope's text`() {
        // "16" is in the date and nowhere else, and the name scope must not find it.
        assertFalse(matchesQuery(text(SearchScope.NAME), "16"))
        assertFalse(matchesQuery(text(SearchScope.DATE), "评审"))
        assertFalse(matchesQuery(text(SearchScope.TIME), "评审"))
    }

    // ----------------------------------------------------------------- queries

    @Test
    fun `a blank query matches everything`() {
        assertTrue(matchesQuery(text(SearchScope.NAME), ""))
        assertTrue(matchesQuery(text(SearchScope.NAME), "   "))
    }

    @Test
    fun `matching ignores case`() {
        assertTrue(matchesQuery("Birthday", "birth"))
        assertTrue(matchesQuery("birthday", "BIRTH"))
    }

    @Test
    fun `a query is trimmed before matching`() {
        assertTrue(matchesQuery("项目评审", "  评审  "))
    }

    @Test
    fun `a query that is not there does not match`() {
        assertFalse(matchesQuery(text(SearchScope.NAME), "旅行"))
    }

    @Test
    fun `a countdown with no tags still searches by name`() {
        val bare = event.copy(tags = emptyList())

        val haystack = searchText(bare, SearchScope.NAME, dateText = "", timeText = "")

        assertTrue(matchesQuery(haystack, "评审"))
    }
}
