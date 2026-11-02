package app.yerleske.menu

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MenuParserTest {
    @Test fun htmlAndBlankVegetarianLinesAreCleaned() {
        assertEquals(listOf("ÇORBA (130 kcal)", "PİLAV", "AYRAN"), MenuParser.lines("<p>ÇORBA (130 kcal)</p><p>&nbsp;</p><p>PİLAV<br>AYRAN</p>"))
    }
    @Test fun missingMealsRemainEmptyAndInvalidDateIsIgnored() {
        val parsed = MenuParser.parse("""[{"date":"02-11-2026","lunch_menu":"<p>ÇORBA</p>"},{"date":"31-11-2026"}]""")
        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026,11,2), parsed.single().date)
        assertTrue(parsed.single().meals.getValue("dinner_menu").isEmpty())
    }
    @Test fun unpublishedMonthIsValidButNonJsonResponseIsNot() {
        assertTrue(MenuParser.parse("[]").isEmpty())
        assertThrows(Exception::class.java) { MenuParser.parse("<html>Sunucu hatası</html>") }
    }
}
