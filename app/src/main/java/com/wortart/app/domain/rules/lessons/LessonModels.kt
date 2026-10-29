package com.wortart.app.domain.rules.lessons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Models of assets/grammar_rules.json (bilingual grammar lessons).
// Property names must stay the same as the keys in the JSON file.

/** English and Bengali text. */
@Serializable
data class Bi(val en: String, val bn: String)

/** German, English and Bengali text. */
@Serializable
data class Trio(val de: String, val en: String, val bn: String)

/** One piece of a lesson. The JSON key "type" tells which kind it is. */
@Serializable
sealed interface LessonBlock {

    @Serializable
    @SerialName("text")
    data class Text(val en: String, val bn: String, val title: String? = null) : LessonBlock

    @Serializable
    @SerialName("table")
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val caption: Bi? = null,
    ) : LessonBlock

    @Serializable
    @SerialName("examples")
    data class Examples(val items: List<LessonExample>) : LessonBlock

    @Serializable
    @SerialName("tip")
    data class Tip(val en: String, val bn: String) : LessonBlock

    @Serializable
    @SerialName("mistake")
    data class Mistake(val wrong: String, val right: String, val en: String, val bn: String) : LessonBlock
}

/** An example sentence. [mark] is the part of [de] to highlight. */
@Serializable
data class LessonExample(val de: String, val en: String, val bn: String, val mark: String? = null)

/** A one-question quiz. [answer] is the index of the correct entry in [options]. */
@Serializable
data class QuizItem(val q: String, val options: List<String>, val answer: Int, val why: Bi)

@Serializable
data class Lesson(
    val id: String,
    /** CEFR level: A1, A2, B1 or B2. */
    val level: String,
    /** Name of a Material icon, see [lessonEmoji]. */
    val icon: String,
    val title: Trio,
    val summary: Bi,
    val blocks: List<LessonBlock>,
    val quiz: List<QuizItem> = emptyList(),
)

@Serializable
data class LessonsFile(
    val version: Int,
    /** Colors for der / die / das as "#RRGGBB". */
    val genderColors: Map<String, String>,
    val sections: List<Lesson>,
)
