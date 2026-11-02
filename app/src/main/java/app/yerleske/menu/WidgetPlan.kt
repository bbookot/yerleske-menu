package app.yerleske.menu

import java.time.*

/** Diyanet resmi tatil listeleri: icerik=158 (2026), 159 (2027).
 * Yarım gün tatillerinde öğle servisi 13:00 sonrası sürdüğünden iki servis de kapalı kabul edilir.
 */
object ServiceCalendar {
    private val fixed = setOf(MonthDay.of(1,1), MonthDay.of(4,23), MonthDay.of(5,1), MonthDay.of(5,19), MonthDay.of(7,15), MonthDay.of(8,30), MonthDay.of(10,28), MonthDay.of(10,29))
    private val religious = listOf(
        "2026-03-19" to "2026-03-22", "2026-05-26" to "2026-05-30",
        "2027-03-08" to "2027-03-11", "2027-05-15" to "2027-05-19"
    ).flatMap { (start,end) -> generateSequence(LocalDate.parse(start)) { it.plusDays(1) }.takeWhile { it <= LocalDate.parse(end) }.toList() }.toSet()
    fun reason(date: LocalDate): String? = when {
        MonthDay.from(date) in fixed || date in religious -> "Resmî tatil · yemek hizmeti yok"
        date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Hafta sonu · yemek hizmeti yok"
        else -> null
    }
    fun nextOpen(date: LocalDate): LocalDate = generateSequence(date) { it.plusDays(1) }.first { reason(it) == null }
    fun calendarWarning(date: LocalDate): String? = if (date.year !in 2026..2027) "Bu yılın dinî tatil takvimi için uygulama güncellemesi gerekli" else null
}
data class WidgetPlan(val base: LocalDate, val date: LocalDate, val lunch: Boolean, val dinner: Boolean, val notice: String?, val offset: Int)
object WidgetPlanner {
    val lunchEnd: LocalTime = LocalTime.of(14,30)
    val dinnerEnd: LocalTime = LocalTime.of(19,15)
    fun plan(now: ZonedDateTime, offset: Int = 0): WidgetPlan {
        val local = now.withZoneSameInstant(turkey)
        val today = local.toLocalDate()
        val closed = ServiceCalendar.reason(today)
        val base = ServiceCalendar.nextOpen(if (closed != null || local.toLocalTime() >= dinnerEnd) today.plusDays(1) else today)
        val safeOffset = offset.coerceIn(0,3)
        val date = base.plusDays(safeOffset.toLong())
        val reason = ServiceCalendar.reason(date)
        val notice = reason ?: if (safeOffset == 0 && closed != null) "Bugün: $closed" else null
        val lunch = reason == null && (date != today || local.toLocalTime() < lunchEnd)
        return WidgetPlan(base, date, lunch, reason == null, notice, safeOffset)
    }
    fun weekMonths(start: LocalDate): List<YearMonth> = (0L..6L).map { YearMonth.from(start.plusDays(it)) }.distinct()
    fun nextTransition(now: ZonedDateTime): ZonedDateTime {
        val local = now.withZoneSameInstant(turkey)
        return listOf(local.toLocalDate().atTime(lunchEnd).atZone(turkey), local.toLocalDate().atTime(dinnerEnd).atZone(turkey), local.toLocalDate().plusDays(1).atStartOfDay(turkey)).first { it > local }
    }
}
