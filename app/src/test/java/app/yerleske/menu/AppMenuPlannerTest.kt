package app.yerleske.menu

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AppMenuPlannerTest {
    private fun now(date: String,time: String) = LocalDateTime.parse("${date}T$time").atZone(turkey)
    @Test fun bothSettingsReturnNextServiceDayAndBothMealsAtNinePm() {
        for (timed in listOf(false,true)) {
            val time = now("2026-11-24","21:00")
            val destination = WidgetPlanner.plan(time,timedMeals = timed).date
            assertEquals(LocalDate.parse("2026-11-25"),destination)
            assertEquals(listOf(false,true),AppMenuPlanner.meals(time,destination,timed))
        }
    }
    @Test fun automaticAppAndWidgetAgreeAtAllCutoffsForBothSettings() {
        for (timed in listOf(false,true)) for (time in listOf("00:00","05:59:59","06:00","14:29:59","14:30","19:14:59","19:15","21:00")) {
            val instant = now("2026-11-24",time)
            val widget = WidgetPlanner.plan(instant,timedMeals = timed)
            val expected = buildList { if (widget.lunch) add(false); if (widget.dinner) add(true) }
            assertEquals("timed=$timed time=$time",expected,AppMenuPlanner.meals(instant,widget.date,timed))
        }
    }
    @Test fun weekendHolidayAndYearBoundaryReturnBothOnNextOpenDay() {
        for (timed in listOf(false,true)) for ((date,expected) in listOf("2026-11-27" to "2026-11-30","2026-10-27" to "2026-10-30","2026-12-31" to "2027-01-04")) {
            val time = now(date,"21:00")
            val destination = WidgetPlanner.plan(time,timedMeals = timed).date
            assertEquals(LocalDate.parse(expected),destination)
            assertEquals(listOf(false,true),AppMenuPlanner.meals(time,destination,timed))
        }
    }
    @Test fun explicitManualMealDoesNotChangeAutomaticReturn() {
        val time = now("2026-11-24","21:00")
        val date = WidgetPlanner.plan(time).date
        assertEquals(listOf(true),AppMenuPlanner.meals(time,date,false,true))
        assertEquals(listOf(false),AppMenuPlanner.meals(time,date,false,false))
        assertEquals(listOf(false,true),AppMenuPlanner.meals(time,date,false))
    }
}
