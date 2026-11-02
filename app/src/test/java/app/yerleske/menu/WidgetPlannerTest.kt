package app.yerleske.menu

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class WidgetPlannerTest {
    private fun plan(date: String, time: String, offset: Int = 0) = WidgetPlanner.plan(LocalDateTime.parse("${date}T$time").atZone(turkey), offset)
    @Test fun beforeLunchEndBothMealsAreShown() {
        for (time in listOf("00:00", "11:30", "14:29:59")) {
            val p = plan("2026-11-02", time)
            assertEquals(LocalDate.of(2026,11,2),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
        }
    }
    @Test fun exactlyLunchEndOnlyDinnerIsShown() {
        for (time in listOf("14:30", "17:00", "19:14:59")) {
            val p = plan("2026-11-02", time)
            assertFalse(p.lunch); assertTrue(p.dinner); assertEquals(LocalDate.of(2026,11,2),p.date)
        }
    }
    @Test fun exactlyDinnerEndNextDayBothMealsAreShown() {
        for (time in listOf("19:15", "23:59:59")) {
            val p = plan("2026-11-02", time)
            assertEquals(LocalDate.of(2026,11,3),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
        }
    }
    @Test fun fridayEveningAndWeekendSkipToMonday() {
        for ((date,time) in listOf("2026-11-06" to "19:15", "2026-11-07" to "12:00", "2026-11-08" to "20:00")) {
            val p = plan(date,time)
            assertEquals(LocalDate.of(2026,11,9),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
        }
        assertNotNull(plan("2026-11-07","12:00").notice)
    }
    @Test fun nationalHolidayAndHalfDayAreClosed() {
        assertNotNull(ServiceCalendar.reason(LocalDate.of(2026,10,28)))
        assertNotNull(ServiceCalendar.reason(LocalDate.of(2026,10,29)))
        assertEquals(LocalDate.of(2026,10,30),plan("2026-10-28","12:00").date)
    }
    @Test fun religiousHolidaySkipsEntireBreakIncludingWeekend() {
        assertEquals(LocalDate.of(2026,3,23),plan("2026-03-19","11:00").date)
        assertEquals(LocalDate.of(2026,6,1),plan("2026-05-26","12:00").date)
        assertEquals(LocalDate.of(2027,3,12),plan("2027-03-09","12:00").date)
        assertEquals(LocalDate.of(2027,5,20),plan("2027-05-17","12:00").date)
    }
    @Test fun navigationShowsBothMealsAndStopsAtThreeCalendarDays() {
        val p = plan("2026-11-02","15:00",3)
        assertEquals(LocalDate.of(2026,11,5),p.date); assertTrue(p.lunch); assertTrue(p.dinner)
        assertEquals(p,plan("2026-11-02","15:00",99))
        assertEquals(0,plan("2026-11-02","15:00",-4).offset)
    }
    @Test fun manuallyVisitedWeekendOrHolidayNeverShowsService() {
        val weekend = plan("2026-11-05","10:00",2)
        assertFalse(weekend.lunch); assertFalse(weekend.dinner); assertNotNull(weekend.notice)
        val holiday = plan("2026-10-26","10:00",3)
        assertFalse(holiday.lunch); assertFalse(holiday.dinner)
    }
    @Test fun sevenDayWindowFetchesBothMonthsAndBothYears() {
        assertEquals(listOf(YearMonth.of(2026,11),YearMonth.of(2026,12)),WidgetPlanner.weekMonths(LocalDate.of(2026,11,28)))
        assertEquals(listOf(YearMonth.of(2026,12),YearMonth.of(2027,1)),WidgetPlanner.weekMonths(LocalDate.of(2026,12,28)))
        assertEquals(listOf(YearMonth.of(2026,11)),WidgetPlanner.weekMonths(LocalDate.of(2026,11,2)))
    }
    @Test fun yearBoundarySkipsNewYearAndWeekend() {
        assertEquals(LocalDate.of(2027,1,4),plan("2026-12-31","19:15").date)
    }
    @Test fun deviceTimezoneDoesNotChangeTurkeyServiceTime() {
        val instant = Instant.parse("2026-11-02T11:30:00Z")
        val p = WidgetPlanner.plan(instant.atZone(ZoneId.of("America/New_York")))
        assertFalse(p.lunch); assertTrue(p.dinner)
    }
    @Test fun nextTransitionIsStrictlyInFuture() {
        val now = LocalDateTime.parse("2026-11-02T14:30").atZone(turkey)
        assertEquals(LocalTime.of(19,15),WidgetPlanner.nextTransition(now).toLocalTime())
        assertEquals(LocalDate.of(2026,11,3),WidgetPlanner.nextTransition(now.withHour(19).withMinute(15)).toLocalDate())
    }
}
