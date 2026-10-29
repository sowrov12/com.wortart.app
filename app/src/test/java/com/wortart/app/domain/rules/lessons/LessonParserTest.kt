package com.wortart.app.domain.rules.lessons

import com.wortart.app.data.local.entity.CefrLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class LessonParserTest {

    /** The real file that ships in the app. Unit tests run inside the app/ module folder. */
    private val bundled: LessonsFile by lazy {
        LessonParser.parse(File("src/main/assets/grammar_rules.json").readText(Charsets.UTF_8))
    }

    private val bengali = Regex("[\\u0980-\\u09FF]")

    // ---- The file that ships in the app ----

    @Test
    fun theBundledFileParsesAndIsValid() {
        assertEquals(1, bundled.version)
        assertEquals(16, bundled.sections.size)
        assertEquals(bundled.sections.size, bundled.sections.map { it.id }.toSet().size)
    }

    @Test
    fun everyLessonHasTitlesContentAndAQuiz() {
        bundled.sections.forEach { lesson ->
            assertTrue(lesson.id, lesson.title.de.isNotBlank() && lesson.title.en.isNotBlank() && lesson.title.bn.isNotBlank())
            assertTrue(lesson.id, lesson.summary.en.isNotBlank() && lesson.summary.bn.isNotBlank())
            assertTrue("${lesson.id} has no blocks", lesson.blocks.isNotEmpty())
            assertTrue("${lesson.id} has no quiz", lesson.quiz.isNotEmpty())
            assertTrue("${lesson.id}: level ${lesson.level}", lesson.level in CefrLevels.ALL)
        }
    }

    @Test
    fun allBlockTypesAreUsedSomewhere() {
        val kinds = bundled.sections.flatMap { it.blocks }.map { it::class }.toSet()

        assertTrue(LessonBlock.Text::class in kinds)
        assertTrue(LessonBlock.Table::class in kinds)
        assertTrue(LessonBlock.Examples::class in kinds)
        assertTrue(LessonBlock.Tip::class in kinds)
        assertTrue(LessonBlock.Mistake::class in kinds)
    }

    @Test
    fun everyTableRowHasOneCellPerHeader() {
        bundled.sections.forEach { lesson ->
            lesson.blocks.filterIsInstance<LessonBlock.Table>().forEach { table ->
                table.rows.forEach { row -> assertEquals("${lesson.id}: $row", table.headers.size, row.size) }
            }
        }
    }

    @Test
    fun everyQuizAnswerPointsAtAnOptionAndConvertsToAQuestion() {
        bundled.sections.forEach { lesson ->
            lesson.quiz.forEach { quiz ->
                assertTrue("${lesson.id}: ${quiz.q}", quiz.answer in quiz.options.indices)
                val question = quiz.toChoiceQuestion()
                assertNotNull(question)
                assertEquals(quiz.options[quiz.answer], question!!.correctAnswer)
            }
        }
    }

    @Test
    fun exampleMarksAreInsideTheirGermanSentence() {
        bundled.sections.flatMap { it.blocks }
            .filterIsInstance<LessonBlock.Examples>()
            .flatMap { it.items }
            .forEach { example ->
                example.mark?.let { assertTrue("'$it' not in '${example.de}'", it in example.de) }
            }
    }

    @Test
    fun bengaliTextIsReallyBengali() {
        bundled.sections.forEach { lesson ->
            assertTrue(lesson.id, bengali.containsMatchIn(lesson.title.bn))
            lesson.blocks.forEach { block ->
                when (block) {
                    is LessonBlock.Text -> assertTrue(lesson.id, bengali.containsMatchIn(block.bn))
                    is LessonBlock.Tip -> assertTrue(lesson.id, bengali.containsMatchIn(block.bn))
                    is LessonBlock.Mistake -> assertTrue(lesson.id, bengali.containsMatchIn(block.bn))
                    is LessonBlock.Examples -> block.items.forEach {
                        assertTrue("${lesson.id}: ${it.bn}", bengali.containsMatchIn(it.bn))
                    }
                    is LessonBlock.Table -> Unit
                }
            }
        }
    }

    @Test
    fun genderColorsAreValidForAllThreeArticles() {
        assertEquals(setOf("der", "die", "das"), bundled.genderColors.keys)
        bundled.genderColors.values.forEach { assertNotNull(parseHexColor(it)) }
    }

    @Test
    fun everyLessonIconHasItsOwnEmoji() {
        bundled.sections.forEach { lesson ->
            assertTrue("No emoji for '${lesson.icon}'", lessonEmoji(lesson.icon) != "📚")
        }
    }

    // ---- Checks on broken files ----

    private fun fileJson(blocks: String = """{"type":"text","en":"e","bn":"b"}""", quiz: String = "[]", level: String = "A1") = """
        {"version":1,"genderColors":{"der":"#4C8DFF"},"sections":[
          {"id":"x","level":"$level","icon":"menu_book",
           "title":{"de":"d","en":"e","bn":"b"},"summary":{"en":"e","bn":"b"},
           "blocks":[$blocks],"quiz":$quiz}
        ]}
    """.trimIndent()

    @Test
    fun aMinimalFileParses() {
        val file = LessonParser.parse(fileJson())

        assertEquals("x", file.sections.single().id)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val withExtra = fileJson().replace("\"version\":1,", "\"version\":1,\"note\":\"hello\",")

        assertEquals(1, LessonParser.parse(withExtra).sections.size)
    }

    @Test
    fun aQuizAnswerOutsideTheOptionsIsRejected() {
        val quiz = """[{"q":"q ___","options":["a","b"],"answer":2,"why":{"en":"e","bn":"b"}}]"""

        expectFailure { LessonParser.parse(fileJson(quiz = quiz)) }
    }

    @Test
    fun aTableRowWithTheWrongWidthIsRejected() {
        val table = """{"type":"table","headers":["a","b"],"rows":[["1","2"],["3"]]}"""

        expectFailure { LessonParser.parse(fileJson(blocks = table)) }
    }

    @Test
    fun anUnknownLevelIsRejected() {
        expectFailure { LessonParser.parse(fileJson(level = "C2")) }
    }

    @Test
    fun anUnknownBlockTypeIsRejected() {
        expectFailure { LessonParser.parse(fileJson(blocks = """{"type":"video","en":"e","bn":"b"}""")) }
    }

    @Test
    fun aMissingRequiredFieldIsRejected() {
        expectFailure { LessonParser.parse(fileJson(blocks = """{"type":"tip","en":"only english"}""")) }
    }

    @Test
    fun aBadGenderColorIsRejected() {
        expectFailure { LessonParser.parse(fileJson().replace("#4C8DFF", "blue")) }
    }

    @Test
    fun duplicateLessonIdsAreRejected() {
        val lesson = """{"id":"x","level":"A1","icon":"menu_book","title":{"de":"d","en":"e","bn":"b"},
            "summary":{"en":"e","bn":"b"},"blocks":[]}"""
        val json = """{"version":1,"genderColors":{},"sections":[$lesson,$lesson]}"""

        expectFailure { LessonParser.parse(json) }
    }

    @Test
    fun aQuizIsOptional() {
        val noQuiz = fileJson().replace(",\"quiz\":[]", "")

        assertTrue(LessonParser.parse(noQuiz).sections.single().quiz.isEmpty())
    }

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected the file to be rejected")
        } catch (expected: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException too.
            assertFalse(expected.message.isNullOrBlank())
        }
    }

    @Test
    fun nothingIsReturnedForAMissingMark() {
        val example = LessonExample(de = "Ich sehe den Mann.", en = "e", bn = "b")

        assertNull(example.mark)
    }
}
