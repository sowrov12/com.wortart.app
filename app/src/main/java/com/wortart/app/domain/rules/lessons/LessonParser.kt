package com.wortart.app.domain.rules.lessons

import com.wortart.app.data.local.entity.CefrLevels
import kotlinx.serialization.json.Json

/** Reads grammar_rules.json and checks that the lessons are complete and consistent. */
object LessonParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @throws kotlinx.serialization.SerializationException if the JSON has the wrong shape
     * @throws IllegalArgumentException if a lesson is inconsistent (the message names it)
     */
    fun parse(text: String): LessonsFile {
        val file = json.decodeFromString(LessonsFile.serializer(), text)
        validate(file)
        return file
    }

    fun validate(file: LessonsFile) {
        val ids = file.sections.map { it.id }
        require(ids.size == ids.toSet().size) { "Duplicate lesson ids in grammar_rules.json" }

        file.genderColors.forEach { (article, color) ->
            require(parseHexColor(color) != null) { "genderColors.$article is not a #RRGGBB color: $color" }
        }

        file.sections.forEach { lesson ->
            require(lesson.level in CefrLevels.ALL) { "${lesson.id}: unknown level '${lesson.level}'" }

            lesson.blocks.filterIsInstance<LessonBlock.Table>().forEach { table ->
                require(table.rows.all { it.size == table.headers.size }) {
                    "${lesson.id}: a table row does not have one cell per header"
                }
            }

            lesson.quiz.forEachIndexed { index, quiz ->
                require(quiz.options.size >= 2) { "${lesson.id}: quiz #${index + 1} needs at least two options" }
                require(quiz.answer in quiz.options.indices) {
                    "${lesson.id}: quiz #${index + 1} answer index ${quiz.answer} is out of range"
                }
            }
        }
    }
}
