package com.wortart.app.domain.word

import com.wortart.app.data.local.entity.VocabEntity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PresentTenseTest {

    private fun verb(
        word: String,
        category: String = "regular_verb",
        prefix: String? = null,
        stammform: String? = null,
    ) = VocabEntity(
        id = word,
        word = word,
        category = category,
        level = "A1",
        topic = "General",
        translationEn = "en",
        translationBn = "bn",
        exampleDe = "de",
        exampleEn = "en",
        exampleBn = "bn",
        auxiliary = "haben",
        prefix = prefix,
        stammform = stammform,
    )

    /** The six forms of every verb in assets/vocab_seed.json, in the order ich, du, er, wir, ihr, sie. */
    private val expected = mapOf(
        "machen" to "mache machst macht machen macht machen",
        "lernen" to "lerne lernst lernt lernen lernt lernen",
        "kochen" to "koche kochst kocht kochen kocht kochen",
        "besuchen" to "besuche besuchst besucht besuchen besucht besuchen",
        "bezahlen" to "bezahle bezahlst bezahlt bezahlen bezahlt bezahlen",
        "erklären" to "erkläre erklärst erklärt erklären erklärt erklären",
        "verbessern" to "verbessere verbesserst verbessert verbessern verbessert verbessern",
        "entwickeln" to "entwickle entwickelst entwickelt entwickeln entwickelt entwickeln",
        "gehen" to "gehe gehst geht gehen geht gehen",
        "kommen" to "komme kommst kommt kommen kommt kommen",
        "essen" to "esse isst isst essen esst essen",
        "trinken" to "trinke trinkst trinkt trinken trinkt trinken",
        "sehen" to "sehe siehst sieht sehen seht sehen",
        "schlafen" to "schlafe schläfst schläft schlafen schlaft schlafen",
        "fahren" to "fahre fährst fährt fahren fahrt fahren",
        "nehmen" to "nehme nimmst nimmt nehmen nehmt nehmen",
        "sprechen" to "spreche sprichst spricht sprechen sprecht sprechen",
        "beginnen" to "beginne beginnst beginnt beginnen beginnt beginnen",
        "verstehen" to "verstehe verstehst versteht verstehen versteht verstehen",
        "vermeiden" to "vermeide vermeidest vermeidet vermeiden vermeidet vermeiden",
        "einkaufen" to "kaufe kaufst kauft kaufen kauft kaufen",
        "aufstehen" to "stehe stehst steht stehen steht stehen",
        "anrufen" to "rufe rufst ruft rufen ruft rufen",
        "aufräumen" to "räume räumst räumt räumen räumt räumen",
        "mitkommen" to "komme kommst kommt kommen kommt kommen",
        "abfahren" to "fahre fährst fährt fahren fahrt fahren",
        "ankommen" to "komme kommst kommt kommen kommt kommen",
        "anmelden" to "melde meldest meldet melden meldet melden",
        "ausfüllen" to "fülle füllst füllt füllen füllt füllen",
        "vorbereiten" to "bereite bereitest bereitet bereiten bereitet bereiten",
    )

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    /** The verbs of the real seed file, so the test notices when the data changes. */
    private fun seedVerbs(): List<VocabEntity> {
        val json = File("src/main/assets/vocab_seed.json").readText(Charsets.UTF_8)
        return Json.parseToJsonElement(json).jsonArray
            .map { it.jsonObject }
            .filter { it.text("category")!!.endsWith("_verb") }
            .map { obj ->
                verb(
                    word = obj.text("word")!!,
                    category = obj.text("category")!!,
                    prefix = obj.text("prefix"),
                    stammform = obj.text("stammform"),
                )
            }
    }

    @Test
    fun knownVerbsInTheSeedFileHaveTheCorrectPresentTense() {
        // The seed file now holds thousands of verbs; check the ones this test knows.
        val verbs = seedVerbs().filter { it.word in expected.keys }
        var checked = 0

        verbs.forEach { card ->
            val forms = PresentTense.forms(card) ?: return@forEach
            val tail = card.prefix?.let { " … $it" }.orEmpty()
            val want = expected.getValue(card.word).split(" ").map { it + tail }

            assertEquals(card.word, want, forms)
            checked++
        }
        assertTrue("no known verb was checked", checked > 0)
    }

    @Test
    fun separableVerbsKeepTheirPrefixAtTheEnd() {
        val forms = PresentTense.forms(verb("einkaufen", "separable_verb", prefix = "ein"))!!

        assertEquals(PresentTense.PRONOUNS.size, forms.size)
        assertEquals("kaufe … ein", forms.first())
        assertEquals("kaufen … ein", forms[3])
    }

    @Test
    fun theErFormOfAnIrregularVerbDecidesTheDuForm() {
        val forms = PresentTense.forms(verb("fahren", "irregular_verb", stammform = "er fährt"))!!

        assertEquals("fahre", forms[0])
        assertEquals("fährst", forms[1])
        assertEquals("fährt", forms[2])
    }

    @Test
    fun anIrregularVerbWithoutItsErFormGivesNoTable() {
        assertNull(PresentTense.forms(verb("laufen", "irregular_verb")))
    }

    @Test
    fun anUnexpectedStammformGivesNoTable() {
        assertNull(PresentTense.forms(verb("gehen", "irregular_verb", stammform = "geht")))
        assertNull(PresentTense.forms(verb("gehen", "irregular_verb", stammform = "er")))
        assertNull(PresentTense.forms(verb("können", "irregular_verb", stammform = "er kann")))
    }

    @Test
    fun wordsThatAreNotVerbsHaveNoTable() {
        assertNull(PresentTense.forms(verb("schnell", category = "adjective")))
        assertNull(PresentTense.forms(verb("oft", category = "adverb")))
    }

    @Test
    fun aVerbWithAnUnusualInfinitiveGivesNoTable() {
        assertNull(PresentTense.forms(verb("sein")))
        assertNotNull(PresentTense.forms(verb("lernen")))
    }

    @Test
    fun verbsOnDOrTGetAnExtraE() {
        val forms = PresentTense.forms(verb("arbeiten"))!!

        assertEquals(listOf("arbeite", "arbeitest", "arbeitet", "arbeiten", "arbeitet", "arbeiten"), forms)
    }
}
