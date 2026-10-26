package com.wortart.app.domain.rules

import com.wortart.app.data.local.entity.VocabEntity

/** An example sentence in German with English and Bengali translations. */
data class RuleExample(val de: String, val en: String, val bn: String)

/** One piece of content inside an expandable rule section. */
sealed interface RuleBlock {
    data class Paragraph(val text: String) : RuleBlock

    data class BulletList(val items: List<String>) : RuleBlock

    /** A table; every row has exactly one cell per header. */
    data class Table(
        val title: String?,
        val headers: List<String>,
        val rows: List<List<String>>,
    ) : RuleBlock

    data class ExampleList(val title: String?, val examples: List<RuleExample>) : RuleBlock
}

/** An expandable section of the Grammar Rules screen. */
data class RuleSection(
    val id: String,
    val title: String,
    /** German name of the topic, shown under the title. */
    val subtitle: String,
    val blocks: List<RuleBlock>,
)

object RuleSectionIds {
    const val SEPARABLE = "separable"
    const val INSEPARABLE = "inseparable"
    const val CONJUGATION = "conjugation"
    const val IRREGULAR = "irregular"
    const val ADJECTIVES = "adjectives"
}

/**
 * All grammar rule sections in display order.
 *
 * The rules themselves are fixed text. The two vocabulary tables (irregular verbs and
 * adjectives) are built from the learner's own word list, so they always match the
 * flashcards.
 */
fun grammarRuleSections(
    irregularVerbs: List<VocabEntity>,
    adjectives: List<VocabEntity>,
): List<RuleSection> = listOf(
    separableSection(),
    inseparableSection(),
    conjugationSection(),
    irregularSection(irregularVerbs),
    adjectiveSection(adjectives),
)

private fun separableSection() = RuleSection(
    id = RuleSectionIds.SEPARABLE,
    title = "Separable verbs",
    subtitle = "Trennbare Verben",
    blocks = listOf(
        RuleBlock.Paragraph(
            "Many German verbs have a prefix that splits off in a main clause and moves to " +
                "the end of the sentence. The verb itself stays in second position.",
        ),
        RuleBlock.Table(
            title = "Common separable prefixes",
            headers = listOf("Prefix", "Example", "Meaning"),
            rows = listOf(
                listOf("ab", "abfahren", "to depart"),
                listOf("an", "anrufen", "to call"),
                listOf("auf", "aufstehen", "to get up"),
                listOf("aus", "ausfüllen", "to fill out"),
                listOf("ein", "einkaufen", "to shop"),
                listOf("mit", "mitkommen", "to come along"),
                listOf("nach", "nachdenken", "to think about"),
                listOf("vor", "vorbereiten", "to prepare"),
                listOf("zu", "zumachen", "to close"),
            ),
        ),
        RuleBlock.Table(
            title = "Present tense: einkaufen (to shop)",
            headers = listOf("Pronoun", "Form"),
            rows = listOf(
                listOf("ich", "kaufe … ein"),
                listOf("du", "kaufst … ein"),
                listOf("er/sie/es", "kauft … ein"),
                listOf("wir", "kaufen … ein"),
                listOf("ihr", "kauft … ein"),
                listOf("sie/Sie", "kaufen … ein"),
            ),
        ),
        RuleBlock.BulletList(
            listOf(
                "Main clause: the prefix goes to the end. Ich kaufe heute ein.",
                "With a modal verb: the verb stays whole. Ich muss einkaufen.",
                "Subordinate clause: the verb stays whole at the end. …, dass er heute einkauft.",
                "Partizip II: -ge- goes between prefix and stem. einkaufen → eingekauft, " +
                    "aufstehen → aufgestanden.",
                "The prefix carries the stress: EIN-kaufen, AUF-stehen.",
            ),
        ),
        RuleBlock.ExampleList(
            title = "Examples",
            examples = listOf(
                RuleExample(
                    de = "Ich kaufe heute im Supermarkt ein.",
                    en = "I shop at the supermarket today.",
                    bn = "আমি আজ সুপারমার্কেটে কেনাকাটা করি।",
                ),
                RuleExample(
                    de = "Ich muss morgen einkaufen.",
                    en = "I have to shop tomorrow.",
                    bn = "আমাকে আগামীকাল বাজার করতে হবে।",
                ),
                RuleExample(
                    de = "Ich habe gestern eingekauft.",
                    en = "I shopped yesterday.",
                    bn = "আমি গতকাল বাজার করেছি।",
                ),
            ),
        ),
    ),
)

private fun inseparableSection() = RuleSection(
    id = RuleSectionIds.INSEPARABLE,
    title = "Inseparable prefixes",
    subtitle = "Untrennbare Verben",
    blocks = listOf(
        RuleBlock.Paragraph(
            "Verbs with these prefixes never split. The prefix is not stressed, and the " +
                "Partizip II has no ge-.",
        ),
        RuleBlock.Table(
            title = "Common inseparable prefixes",
            headers = listOf("Prefix", "Example", "Partizip II"),
            rows = listOf(
                listOf("be-", "besuchen", "besucht"),
                listOf("emp-", "empfehlen", "empfohlen"),
                listOf("ent-", "entwickeln", "entwickelt"),
                listOf("er-", "erklären", "erklärt"),
                listOf("ge-", "gehören", "gehört"),
                listOf("miss-", "missverstehen", "missverstanden"),
                listOf("ver-", "verbessern", "verbessert"),
                listOf("zer-", "zerstören", "zerstört"),
            ),
        ),
        RuleBlock.BulletList(
            listOf(
                "Prefix and verb always stay together. Ich verstehe das nicht.",
                "Partizip II without ge-: besuchen → besucht, erklären → erklärt.",
                "Careful: über-, unter-, um-, durch- and wider- can be separable or inseparable " +
                    "depending on the verb. Learn each verb together with its Partizip II.",
            ),
        ),
        RuleBlock.ExampleList(
            title = "Examples",
            examples = listOf(
                RuleExample(
                    de = "Wir besuchen morgen einen Freund.",
                    en = "We are visiting a friend tomorrow.",
                    bn = "আমরা আগামীকাল একজন বন্ধুর সাথে দেখা করতে যাব।",
                ),
                RuleExample(
                    de = "Ich habe die Frage verstanden.",
                    en = "I understood the question.",
                    bn = "আমি প্রশ্নটি বুঝেছি।",
                ),
                RuleExample(
                    de = "Er erklärt die Regel.",
                    en = "He explains the rule.",
                    bn = "সে নিয়মটি ব্যাখ্যা করে।",
                ),
            ),
        ),
    ),
)

private fun conjugationSection() = RuleSection(
    id = RuleSectionIds.CONJUGATION,
    title = "Present tense & Perfekt",
    subtitle = "Konjugation",
    blocks = listOf(
        RuleBlock.Paragraph(
            "Regular verbs add the endings -e, -st, -t, -en, -t, -en to the stem. Some " +
                "irregular verbs change their vowel in the du and er/sie/es forms.",
        ),
        RuleBlock.Table(
            title = "Present tense (Präsens)",
            headers = listOf("Pronoun", "machen", "fahren", "essen", "sein", "haben"),
            rows = listOf(
                listOf("ich", "mache", "fahre", "esse", "bin", "habe"),
                listOf("du", "machst", "fährst", "isst", "bist", "hast"),
                listOf("er/sie/es", "macht", "fährt", "isst", "ist", "hat"),
                listOf("wir", "machen", "fahren", "essen", "sind", "haben"),
                listOf("ihr", "macht", "fahrt", "esst", "seid", "habt"),
                listOf("sie/Sie", "machen", "fahren", "essen", "sind", "haben"),
            ),
        ),
        RuleBlock.Table(
            title = "Perfekt: haben or sein?",
            headers = listOf("Infinitiv", "Perfekt"),
            rows = listOf(
                listOf("machen", "hat gemacht"),
                listOf("lernen", "hat gelernt"),
                listOf("essen", "hat gegessen"),
                listOf("gehen", "ist gegangen"),
                listOf("fahren", "ist gefahren"),
                listOf("aufstehen", "ist aufgestanden"),
            ),
        ),
        RuleBlock.BulletList(
            listOf(
                "Use sein with verbs of movement and change of state: gehen, kommen, fahren, " +
                    "aufstehen.",
                "Use haben with most other verbs.",
                "Regular Präteritum: stem + -te. machen → machte, lernen → lernte.",
                "Regular Partizip II: ge- + stem + -t. gemacht, gelernt.",
            ),
        ),
        RuleBlock.ExampleList(
            title = "Examples",
            examples = listOf(
                RuleExample(
                    de = "Er fährt jeden Tag mit dem Bus.",
                    en = "He takes the bus every day.",
                    bn = "সে প্রতিদিন বাসে যায়।",
                ),
                RuleExample(
                    de = "Ich bin nach Hause gegangen.",
                    en = "I went home.",
                    bn = "আমি বাড়ি গিয়েছি।",
                ),
                RuleExample(
                    de = "Ich habe Pizza gegessen.",
                    en = "I ate pizza.",
                    bn = "আমি পিৎজা খেয়েছি।",
                ),
            ),
        ),
    ),
)

private fun irregularSection(verbs: List<VocabEntity>): RuleSection {
    val blocks = mutableListOf<RuleBlock>(
        RuleBlock.Paragraph(
            "Irregular (strong) verbs change their stem vowel and often more. Learn the three " +
                "principal forms together with haben or sein.",
        ),
    )

    if (verbs.isNotEmpty()) {
        blocks += RuleBlock.Table(
            title = "Irregular verbs in your vocabulary",
            headers = listOf("Infinitiv", "Präteritum", "Partizip II", "Perfekt with"),
            rows = verbs.sortedBy { it.word.lowercase() }.map {
                listOf(it.word, it.praeteritum ?: "–", it.partizip2 ?: "–", it.auxiliary ?: "–")
            },
        )
    }

    blocks += RuleBlock.BulletList(
        listOf(
            "Many irregular Partizip II forms end in -en: gegangen, gesehen, genommen.",
            "Mixed verbs change the vowel but keep the -t ending: bringen – brachte – " +
                "gebracht, denken – dachte – gedacht.",
            "The flashcard of each verb shows whether it uses haben or sein.",
        ),
    )
    blocks += RuleBlock.ExampleList(
        title = "Examples",
        examples = listOf(
            RuleExample(
                de = "Gestern ging ich früh schlafen.",
                en = "Yesterday I went to sleep early.",
                bn = "গতকাল আমি তাড়াতাড়ি ঘুমাতে গিয়েছিলাম।",
            ),
            RuleExample(
                de = "Wir sind nach Berlin gefahren.",
                en = "We drove to Berlin.",
                bn = "আমরা বার্লিনে গিয়েছিলাম।",
            ),
        ),
    )

    return RuleSection(
        id = RuleSectionIds.IRREGULAR,
        title = "Irregular verbs",
        subtitle = "Unregelmäßige Verben",
        blocks = blocks,
    )
}

private fun adjectiveSection(adjectives: List<VocabEntity>): RuleSection {
    val blocks = mutableListOf<RuleBlock>(
        RuleBlock.Paragraph(
            "Comparative: adjective + -er. Superlative: am + adjective + -sten. Use “als” to " +
                "compare two things and “so … wie” to say they are equal.",
        ),
        RuleBlock.Table(
            title = "Regular pattern",
            headers = listOf("Adjective", "Komparativ", "Superlativ"),
            rows = listOf(
                listOf("schnell", "schneller", "am schnellsten"),
                listOf("klein", "kleiner", "am kleinsten"),
                listOf("wichtig", "wichtiger", "am wichtigsten"),
            ),
        ),
        RuleBlock.Table(
            title = "Umlaut and irregular forms",
            headers = listOf("Adjective", "Komparativ", "Superlativ"),
            rows = listOf(
                listOf("groß", "größer", "am größten"),
                listOf("alt", "älter", "am ältesten"),
                listOf("jung", "jünger", "am jüngsten"),
                listOf("hoch", "höher", "am höchsten"),
                listOf("gut", "besser", "am besten"),
                listOf("viel", "mehr", "am meisten"),
                listOf("gern", "lieber", "am liebsten"),
            ),
        ),
    )

    if (adjectives.isNotEmpty()) {
        blocks += RuleBlock.Table(
            title = "Adjectives in your vocabulary",
            headers = listOf("Adjective", "Komparativ", "Superlativ"),
            rows = adjectives.sortedBy { it.word.lowercase() }.map {
                listOf(it.word, it.komparativ ?: "–", it.superlativ ?: "–")
            },
        )
    }

    blocks += RuleBlock.BulletList(
        listOf(
            "Adjectives ending in -d, -t, -s, -sch or -z usually add -est in the superlative: " +
                "alt → am ältesten, kurz → am kürzesten.",
            "Adjectives ending in -el or -er drop the e in the comparative: teuer → teurer, " +
                "dunkel → dunkler.",
            "Before a noun the endings change: ein schnellerer Zug, der schnellste Zug.",
        ),
    )
    blocks += RuleBlock.ExampleList(
        title = "Examples",
        examples = listOf(
            RuleExample(
                de = "Der Zug ist schneller als das Auto.",
                en = "The train is faster than the car.",
                bn = "ট্রেনটি গাড়ির চেয়ে দ্রুত।",
            ),
            RuleExample(
                de = "Dieser Zug ist am schnellsten.",
                en = "This train is the fastest.",
                bn = "এই ট্রেনটি সবচেয়ে দ্রুত।",
            ),
            RuleExample(
                de = "Sie ist so groß wie ihr Bruder.",
                en = "She is as tall as her brother.",
                bn = "সে তার ভাইয়ের মতোই লম্বা।",
            ),
        ),
    )

    return RuleSection(
        id = RuleSectionIds.ADJECTIVES,
        title = "Comparative & superlative",
        subtitle = "Steigerung der Adjektive",
        blocks = blocks,
    )
}
