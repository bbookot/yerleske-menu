package app.yerleske.menu

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class TimedMealTest {
    private fun plan(date: String,time: String,offset: Int = 0) = WidgetPlanner.plan(LocalDateTime.parse("${date}T$time").atZone(turkey),offset,true)
    @Test fun eveningToNextMorningUsesBothUntilSixThenOnlyLunch() {
        val evening = plan("2026-07-20","20:00")
        assertEquals(LocalDate.parse("2026-07-21"),evening.date)
        assertTrue(evening.lunch); assertTrue(evening.dinner)
        for (time in listOf("00:00","05:59:59")) {
            val p = plan("2026-07-21",time)
            assertEquals(evening.date,p.date); assertTrue(p.lunch); assertTrue(p.dinner)
        }
        for (time in listOf("06:00","11:30","14:29:59")) {
            val p = plan("2026-07-21",time)
            assertEquals(evening.date,p.date); assertTrue(p.lunch); assertFalse(p.dinner)
        }
    }
    @Test fun lunchAndDinnerCutoffsAreExact() {
        for (time in listOf("14:30","17:00","19:14:59")) {
            val p = plan("2026-07-21",time); assertFalse(p.lunch); assertTrue(p.dinner)
            assertEquals(LocalDate.parse("2026-07-21"),p.date)
        }
        val p = plan("2026-07-21","19:15")
        assertEquals(LocalDate.parse("2026-07-22"),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
    }
    @Test fun closedDaysAndFutureBrowsingKeepBothMeals() {
        for ((date,time) in listOf("2026-07-17" to "20:00","2026-07-18" to "06:00","2026-07-15" to "12:00")) {
            val p = plan(date,time); assertNotEquals(LocalDate.parse(date),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
            assertNull(ServiceCalendar.reason(p.date))
        }
        val future = plan("2026-07-21","08:00",1)
        assertTrue(future.lunch); assertTrue(future.dinner)
    }
    @Test fun morningBoundaryIsScheduledAndTimezoneIsTurkey() {
        assertEquals(LocalTime.of(6,0),WidgetPlanner.nextTransition(LocalDateTime.parse("2026-07-21T05:59:59").atZone(turkey)).toLocalTime())
        assertEquals(LocalTime.of(14,30),WidgetPlanner.nextTransition(LocalDateTime.parse("2026-07-21T06:00").atZone(turkey)).toLocalTime())
        val p = WidgetPlanner.plan(Instant.parse("2026-07-21T03:00:00Z").atZone(ZoneId.of("America/New_York")),timedMeals = true)
        assertTrue(p.lunch); assertFalse(p.dinner)
    }
    @Test fun manualStartChoicesAndSavedThemeAreIndependent() {
        assertTrue(StartMeal.LAST.initialEvening(true)); assertFalse(StartMeal.LAST.initialEvening(false))
        assertTrue(StartMeal.DINNER.initialEvening(false)); assertFalse(StartMeal.LUNCH.initialEvening(true))
        assertTrue(AppOptions(theme = 2).isDark(false)); assertFalse(AppOptions(theme = 1).isDark(true))
        assertTrue(AppOptions(theme = 0).isDark(true)); assertFalse(AppOptions(theme = 0).isDark(false))
        assertFalse(AppOptions().timedMeals)
    }
}
