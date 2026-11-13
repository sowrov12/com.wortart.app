package com.wortart.app.data.sheets

import com.wortart.app.data.local.entity.VocabEntity
import kotlin.random.Random

/** Rows of a sheet, each row a list of cell texts (a missing cell is an empty text). */
typealias SheetRows = List<List<String>>

/** The word lists as they are in the sheets. Every list is without its header row. */
class SheetData(
    /** The verb list: each row by its header name (Level, Verb, En meaning, ...). */
    val verbs: List<Map<String, String>>,
    val adjectives: SheetRows,
    val adverbs: SheetRows,
    val prepositions: SheetRows,
    val conjunctions: SheetRows,
    val modalVerbs: SheetRows,
)

/**
 * Turns the word sheets into the cards of the app, including everything the flashcard sets need:
 * the sentence with a gap and its wrong options, the Partizip II options, the separable prefix gap,
 * the groups of a word and the opposite word of an adjective.
 *
 * The same rules built the word list that ships inside the app, so a sheet that is unchanged gives
 * the same cards.
 */
object VocabSheetImporter {

    private const val GAP = "_____"
    private const val LETTERS = "A-Za-zÄÖÜäöüß"
    private val TOKEN = Regex("[$LETTERS]+(?:-[$LETTERS]+)*")
    private val LEVELS = setOf("A1", "A2", "B1", "B2")
    private val REFLEXIVE_PRONOUNS = setOf("mich", "dich", "sich", "uns", "euch")
    private val MODAL_VERBS_IN_VERB_LIST = setOf("können", "müssen", "wollen", "dürfen", "sollen", "mögen")
    private val DASHES = setOf("—", "-", "–", "")
    private val PREPOSITION_VERB = Regex(
        "\\b(auf|mit|über|zu|von|nach|an|für|um|in|aus|bei|vor|gegen|durch|unter|ohne)\\s*\\+",
    )
    private val CONTRACTIONS = mapOf(
        "zu" to listOf("zum", "zur"), "in" to listOf("im", "ins"), "an" to listOf("am", "ans"),
        "bei" to listOf("beim"), "von" to listOf("vom"), "auf" to listOf("aufs"),
        "um" to listOf("ums"), "für" to listOf("fürs"), "durch" to listOf("durchs"),
    )

    private fun List<String>.c(index: Int): String = getOrNull(index)?.trim().orEmpty()

    private fun capLike(word: String, reference: String): String =
        if (reference.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() } else word

    private fun firstMeaning(text: String): String = text.substringBefore(';').trim()

    private fun random(key: String): Random = Random(key.hashCode())

    private class Ids {
        private val used = HashMap<String, Int>()

        /** A stable id from the word, so favorites survive when the sheet changes. */
        fun next(prefix: String, word: String): String {
            val slug = word.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]+"), "_").trim('_').ifEmpty { "x" }
            val base = "${prefix}_$slug"
            val count = (used[base] ?: 0) + 1
            used[base] = count
            return if (count == 1) base else "${base}_$count"
        }
    }

    private class Gap(val text: String, val answer: String, val wrong: List<String>)

    fun build(data: SheetData): List<VocabEntity> {
        val ids = Ids()
        val out = ArrayList<VocabEntity>()

        for (row in data.verbs) {
            val verb = row["Verb"].orEmpty().trim()
            if (verb in MODAL_VERBS_IN_VERB_LIST) continue
            verbEntity(ids, "vb", row, modal = false)?.let { out.add(it) }
        }
        for (cells in data.modalVerbs) {
            val row = mapOf(
                "Level" to cells.c(0), "Verb" to cells.c(1), "En meaning" to cells.c(2),
                "Bn meaning" to cells.c(3), "Beispiel" to cells.c(4), "En Beispiel" to cells.c(5),
                "Bn Beispiel" to cells.c(6), "verb conjugation" to cells.c(7), "Präteritum" to cells.c(9),
                "Partizip II" to cells.c(10), "Hilfsverben(Sein/haben)" to "haben",
            )
            verbEntity(ids, "md", row, modal = true)?.let { out.add(it) }
        }
        out += adjectives(ids, data.adjectives)
        out += adverbs(ids, data.adverbs)
        out += prepositions(ids, data.prepositions)
        out += conjunctions(ids, data.conjunctions)
        return out
    }

    // ------------------------------------------------------------------ verbs

    private fun imperatives(du: String): List<String> {
        val stem = when {
            du.endsWith("est") -> du.dropLast(3)
            du.endsWith("sst") || du.endsWith("ßt") || du.endsWith("zt") || du.endsWith("xt") -> du.dropLast(1)
            du.endsWith("st") -> du.dropLast(2)
            du.endsWith("t") -> du.dropLast(1)
            else -> du
        }
        return listOf(stem, stem.replace('ä', 'a').replace('ö', 'o').replace('ü', 'u')).distinct()
    }

    private fun verbEntity(ids: Ids, idPrefix: String, row: Map<String, String>, modal: Boolean): VocabEntity? {
        val rawVerb = row["Verb"].orEmpty().trim().replace(Regex("\\s*\\(.*?\\)\\*?"), "").trim()
        val level = row["Level"].orEmpty().trim().uppercase()
        if (rawVerb.isEmpty() || level !in LEVELS) return null

        val reflexive = rawVerb.startsWith("sich ")
        val base = if (reflexive) rawVerb.substring(5) else rawVerb
        val parts = base.split('|')
        val prefix = if (parts.size > 1 && parts[0].isNotEmpty()) parts[0] else null
        val infinitive = base.replace("|", "").split(' ')[0]
        val word = rawVerb.replace("|", "")
        val stemInfinitive = if (prefix != null) infinitive.drop(prefix.length) else infinitive

        var conjugation = row["verb conjugation"].orEmpty().split(',')
            .map { it.trim() }.filter { it.isNotEmpty() }
            .map { it.split(Regex("\\s+"))[0] }
        val imperative = if (conjugation.size > 1) imperatives(conjugation[1]) else emptyList()
        conjugation = conjugation + conjugation.filter { it.endsWith("t") && it.length > 2 }.map { it.dropLast(1) + "et" }

        val preterite = row["Präteritum"].orEmpty().replace(Regex("\\s*\\(.*?\\)"), "").trim()
            .split(' ')[0].trim(',')
        val participle = row["Partizip II"].orEmpty().trim()

        val families = linkedMapOf(
            "cj" to conjugation.distinct(),
            "cjj" to (if (prefix != null) conjugation.map { prefix + it }.distinct() else emptyList()),
            "pr" to (listOf(preterite) + (if (prefix != null) listOf(prefix + preterite) else emptyList())),
            "pz" to listOf(participle),
            "inf" to (listOf(infinitive) + (if (prefix != null) listOf(prefix + "zu" + stemInfinitive) else emptyList())),
            "imp" to imperative,
        ).mapValues { entry -> entry.value.filter { it.isNotEmpty() } }

        val lookup = LinkedHashMap<String, String>()
        for ((family, forms) in families) {
            for (form in forms) lookup.putIfAbsent(form.lowercase(), family)
        }

        val example = row["Beispiel"].orEmpty().trim()
        var found: MatchResult? = null
        for (match in TOKEN.findAll(example)) {
            val lower = match.value.lowercase()
            if (lower in lookup && lower !in REFLEXIVE_PRONOUNS) {
                found = match
                break
            }
        }

        var blankText: String? = null
        var blankAnswer: String? = null
        var blankWrong: List<String> = emptyList()
        if (found != null) {
            val answer = found.value
            val family = lookup.getValue(answer.lowercase())
            val pool = families[family].orEmpty().filter { !it.equals(answer, ignoreCase = true) }.toMutableList()
            if (pool.map { it.lowercase() }.toSet().size < 2) {
                for (other in listOf("cj", "pr", "pz", "inf")) {
                    for (form in families[other].orEmpty()) {
                        if (!form.equals(answer, ignoreCase = true) && pool.none { it.equals(form, ignoreCase = true) }) {
                            pool.add(form)
                        }
                    }
                }
            }
            val wrong = pool.distinct().shuffled(random("${idPrefix}_$word" + "b")).take(2).map { capLike(it, answer) }
            if (wrong.size == 2) {
                blankText = example.substring(0, found.range.first) + GAP + example.substring(found.range.last + 1)
                blankAnswer = answer
                blankWrong = wrong
            }
        }

        // Wrong Partizip II options.
        val candidates = ArrayList<String>()
        if (participle.endsWith("t")) candidates.add(participle.dropLast(1) + "en")
        else if (participle.endsWith("en")) candidates.add(participle.dropLast(2) + "t")
        val pp = prefix.orEmpty()
        if (participle.startsWith(pp + "ge") && participle.length > pp.length + 3) {
            candidates.add(pp + participle.drop(pp.length + 2))
        } else if (!participle.startsWith(pp + "ge")) {
            candidates.add(pp + "ge" + participle.drop(pp.length))
        }
        candidates.add(preterite)
        candidates.add(infinitive)
        val participleWrong = candidates
            .filter { it.isNotEmpty() && !it.equals(participle, ignoreCase = true) }
            .distinct().take(2)

        // The separable prefix as a gap.
        var prefixGapText: String? = null
        var prefixGapTitle: String? = null
        if (prefix != null) {
            val tokens = TOKEN.findAll(example).toList()
            var hit: IntRange? = null
            for (match in tokens.asReversed()) {
                if (match.value.equals(prefix, ignoreCase = true)) {
                    hit = match.range
                    break
                }
            }
            if (hit == null) {
                for (match in tokens) {
                    val lower = match.value.lowercase()
                    if (lower in lookup && lower.startsWith(prefix.lowercase()) && lower.length > prefix.length) {
                        hit = match.range.first until (match.range.first + prefix.length)
                        break
                    }
                }
            }
            if (hit != null) {
                prefixGapText = example.substring(0, hit.first) + GAP + example.substring(hit.last + 1)
                prefixGapTitle = (if (reflexive) "sich " else "") + GAP + base.split("|", limit = 2)[1]
            }
        }

        val auxiliary = row["Hilfsverben(Sein/haben)"].orEmpty().trim().ifEmpty { "haben" }
        val irregular = row["regelmäßig/unregelmäßig"].orEmpty().trim() == "unregelmäßig"
        val primary = when {
            modal -> "Modal Verbs"
            irregular -> "Irregular Verbs"
            else -> "Regular Verbs"
        }
        val labels = mutableListOf(primary)
        if (prefix != null) {
            labels.add("Separable Verbs")
        } else if (row["Separable/Inseparable"].orEmpty().trim() == "Inseparable") {
            labels.add("Inseparable Verbs")
        }
        if (reflexive || row["Reflexive (yes/no)"].orEmpty().trim().equals("yes", ignoreCase = true)) {
            labels.add("Reflexive Verbs")
        }
        if (auxiliary == "sein") labels.add("Sein Verbs")
        val cases = row["Cases"].orEmpty()
        if (cases.trim() == "Dativ") labels.add("Dativ Verbs")
        if (PREPOSITION_VERB.containsMatchIn(cases)) labels.add("Prepositional Verbs")

        return VocabEntity(
            id = ids.next(idPrefix, word),
            word = word,
            category = when {
                modal -> "modal_verb"
                irregular -> "irregular_verb"
                else -> "regular_verb"
            },
            level = level,
            topic = primary,
            translationEn = row["En meaning"].orEmpty().trim(),
            translationBn = row["Bn meaning"].orEmpty().trim(),
            exampleDe = example,
            exampleEn = row["En Beispiel"].orEmpty().trim(),
            exampleBn = row["Bn Beispiel"].orEmpty().trim(),
            auxiliary = if (auxiliary == "sein") "sein" else "haben",
            praeteritum = preterite.ifEmpty { null },
            partizip2 = participle.ifEmpty { null },
            prefix = prefix,
            tags = "," + labels.joinToString(",") + ",",
            blankDe = blankText,
            blankAnswer = blankAnswer,
            blankWrong = if (blankWrong.size == 2) blankWrong.joinToString("|") else null,
            pz2Wrong = if (participleWrong.size == 2) participleWrong.joinToString("|") else null,
            prefixBlankDe = prefixGapText,
            prefixBlankTitle = prefixGapTitle,
        )
    }

    // ------------------------------------------------------------------ gaps for other words

    private fun findPhrase(example: String, phrase: String): MatchResult? {
        val clean = phrase.replace(Regex("\\s*\\(.*?\\)"), "").trim()
        if (clean.isEmpty()) return null
        val words = clean.split(' ')
        val last = words.last()
        val alternatives = (CONTRACTIONS[last.lowercase()].orEmpty() + last).joinToString("|") { Regex.escape(it) }
        val head = words.dropLast(1).joinToString("") { Regex.escape(it) + " " }
        val pattern = Regex("(?<![$LETTERS])" + head + "(?:" + alternatives + ")(?![$LETTERS])", RegexOption.IGNORE_CASE)
        return pattern.find(example)
    }

    private fun simpleGap(id: String, example: String, alternatives: List<String>, pool: List<String>): Gap? {
        for (alternative in alternatives) {
            val match = findPhrase(example, alternative) ?: continue
            val answer = match.value
            val wrong = pool.filter { !it.equals(alternative, ignoreCase = true) }
                .distinct().shuffled(random(id + "w")).take(2).map { capLike(it, answer) }
            if (wrong.size == 2) {
                return Gap(
                    example.substring(0, match.range.first) + GAP + example.substring(match.range.last + 1),
                    answer,
                    wrong,
                )
            }
        }
        return null
    }

    /** A gap for a word that comes in two parts, such as "entweder … oder". */
    private fun twoPartGap(id: String, example: String, first: String, second: String, others: List<String>): Gap? {
        val m1 = findPhrase(example, first) ?: return null
        val rest = example.substring(m1.range.last + 1)
        val m2 = Regex("(?<![$LETTERS])" + Regex.escape(second) + "(?![$LETTERS])", RegexOption.IGNORE_CASE)
            .find(rest) ?: return null
        val secondStart = m1.range.last + 1 + m2.range.first
        val secondEnd = m1.range.last + 1 + m2.range.last + 1
        val text = example.substring(0, m1.range.first) + GAP +
            example.substring(m1.range.last + 1, secondStart) + GAP + example.substring(secondEnd)
        val wrong = others.distinct().shuffled(random(id + "w")).take(2).map { capLike(it, m1.value) }
        return Gap(text, m1.value + " … " + m2.value, wrong)
    }

    private fun entity(
        id: String, word: String, category: String, level: String, topic: String, tags: List<String>,
        en: String, bn: String, example: String, exampleEn: String, exampleBn: String, gap: Gap?,
    ): VocabEntity = VocabEntity(
        id = id,
        word = word,
        category = category,
        level = level,
        topic = topic,
        translationEn = en,
        translationBn = bn,
        exampleDe = example,
        exampleEn = exampleEn,
        exampleBn = exampleBn,
        tags = "," + tags.joinToString(",") + ",",
        blankDe = gap?.text,
        blankAnswer = gap?.answer,
        blankWrong = if (gap != null && gap.wrong.size == 2) gap.wrong.joinToString("|") else null,
    )

    // ------------------------------------------------------------------ adjectives

    private fun adjectives(ids: Ids, rows: SheetRows): List<VocabEntity> {
        val list = rows.filter { it.c(1).isNotEmpty() && it.c(0).uppercase() in LEVELS }
        val words = list.map { it.c(1) }.toSet()
        val out = ArrayList<VocabEntity>()

        fun label(word: String, en: String, bn: String) = "$word — ${firstMeaning(en)} / ${firstMeaning(bn)}"

        fun card(
            lvl: String, w: String, en: String, bn: String, ex: String, exEn: String, exBn: String,
            antonymWord: String, antonymEn: String, antonymBn: String,
        ) {
            val antonym = antonymWord.takeIf { it !in DASHES }
            val id = ids.next("aj", w)
            val byLevel = list.filter { it.c(0).uppercase() == lvl }
            var pool = byLevel.filter {
                it.c(1) != w && it.c(1) != antonym && it.c(7) != w && it.c(7) != antonym && it.c(7) != "—"
            }
            if (pool.isEmpty()) pool = list.filter { it.c(1) != w && it.c(1) != antonym }
            val wrongRow = pool.randomOrNull(random(id + "a"))

            var gap: Gap? = null
            val match = TOKEN.findAll(ex).firstOrNull {
                val lower = it.value.lowercase()
                lower.startsWith(w.lowercase()) && lower.length - w.length <= 3
            }
            if (match != null) {
                val ending = match.value.drop(w.length)
                val wrong = byLevel.map { it.c(1) }.filter { it != w }.distinct()
                    .shuffled(random(id + "w")).take(2).map { capLike(it + ending, match.value) }
                gap = Gap(
                    ex.substring(0, match.range.first) + GAP + ex.substring(match.range.last + 1),
                    match.value,
                    wrong,
                )
            }
            out.add(
                entity(id, w, "adjective", lvl, "Adjectives", listOf("Adjectives"), en, bn, ex, exEn, exBn, gap)
                    .copy(
                        antonym = antonym,
                        antonymMeaning = if (antonym != null) "${firstMeaning(antonymEn)} / ${firstMeaning(antonymBn)}" else null,
                        antonymWrong = if (antonym != null && wrongRow != null) label(wrongRow.c(1), wrongRow.c(2), wrongRow.c(3)) else null,
                    ),
            )
        }

        for (r in list) {
            card(r.c(0).uppercase(), r.c(1), r.c(2), r.c(3), r.c(4), r.c(5), r.c(6), r.c(7), r.c(8), r.c(9))
        }
        // The opposite words that are not an adjective row of their own become cards too.
        val seen = HashSet<String>()
        for (r in list) {
            val opposite = r.c(7)
            if (opposite in DASHES || opposite in words || !seen.add(opposite)) continue
            card(r.c(0).uppercase(), opposite, r.c(8), r.c(9), r.c(10), r.c(11), r.c(12), r.c(1), r.c(2), r.c(3))
        }
        return out
    }

    // ------------------------------------------------------------------ adverbs

    private fun adverbs(ids: Ids, rows: SheetRows): List<VocabEntity> {
        val list = rows.filter { it.c(1).isNotEmpty() && it.c(0).uppercase() in LEVELS }
        return list.map { r ->
            val level = r.c(0).uppercase()
            val id = ids.next("av", r.c(1))
            val pool = list.filter { it.c(0).uppercase() == level }.map { it.c(1) }
            val gap = simpleGap(id, r.c(4), listOf(r.c(1)), pool)
            entity(id, r.c(1), "adverb", level, "Adverbs", listOf("Adverbs"), r.c(2), r.c(3), r.c(4), r.c(5), r.c(6), gap)
        }
    }

    // ------------------------------------------------------------------ prepositions

    private fun prepositionBase(word: String): String = word.replace(Regex("\\s*\\(.*\\)"), "").trim()

    private fun prepositions(ids: Ids, rows: SheetRows): List<VocabEntity> {
        val list = rows.filter { it.c(2).isNotEmpty() && it.c(0).uppercase() in LEVELS }
        val simple = list.map { prepositionBase(it.c(2)) }.filter { '…' !in it && ' ' !in it }
        return list.map { r ->
            val level = r.c(0).uppercase()
            val type = r.c(1).ifEmpty { "Prepositions" }
            val base = prepositionBase(r.c(2))
            val id = ids.next("pp", r.c(2))
            val example = r.c(5)
            val gap: Gap? = if ('…' in base) {
                val parts = base.split('…').map { it.trim() }
                val others = list.map { prepositionBase(it.c(2)) }.filter { '…' in it && it != base }
                if (parts.size == 2) {
                    val built = twoPartGap(id, example, parts[0], parts[1], others)
                    if (built != null && built.wrong.size < 2) {
                        val filler = simple.filter { !it.equals(base, ignoreCase = true) }.distinct()
                            .shuffled(random(id + "x")).take(2 - built.wrong.size).map { capLike(it, built.answer) + " …" }
                        Gap(built.text, built.answer, built.wrong + filler)
                    } else {
                        built
                    }
                } else {
                    null
                }
            } else {
                simpleGap(id, example, listOf(base), simple.filter { !it.equals(base, ignoreCase = true) })
            }
            entity(
                id, r.c(2), "preposition", level, type, listOf("Prepositions", type),
                r.c(3), r.c(4), example, r.c(6), r.c(7), gap,
            )
        }
    }

    // ------------------------------------------------------------------ conjunctions and question words

    private fun conjunctions(ids: Ids, rows: SheetRows): List<VocabEntity> {
        val list = rows.filter { it.c(2).isNotEmpty() && it.c(0).uppercase() in LEVELS }
        val singles = list.filter { it.c(1) != "Zweiteilige Konjunktion" && it.c(1) != "Fragewort" && ' ' !in it.c(2) }
            .map { it.c(2) }
        val questionWords = list.filter { it.c(1) == "Fragewort" && '/' !in it.c(2) }.map { it.c(2) }
        val pairs = list.filter { it.c(1) == "Zweiteilige Konjunktion" }.map { it.c(2) }

        return list.map { r ->
            val level = r.c(0).uppercase()
            val type = r.c(1)
            val word = r.c(2)
            val example = r.c(5)
            val id = ids.next("cj", word)
            val label = when (type) {
                "Koordinierende Konjunktion" -> "Coordinating Conjunctions"
                "Subordinierende Konjunktion" -> "Subordinating Conjunctions"
                "Zweiteilige Konjunktion" -> "Two-part Conjunctions"
                "Fragewort" -> "Question Words"
                else -> "Conjunctions"
            }
            val gap: Gap? = if (type == "Zweiteilige Konjunktion") {
                val parts = word.split('…').map { it.trim() }
                if (parts.size == 2) twoPartGap(id, example, parts[0], parts[1], pairs.filter { it != word }) else null
            } else {
                val alternatives = word.split('/').map { it.replace(Regex("\\s*\\(.*?\\)"), "").trim() }
                val pool = (if (type == "Fragewort") questionWords else singles)
                    .filter { candidate -> alternatives.none { it.equals(candidate, ignoreCase = true) } }
                simpleGap(id, example, alternatives, pool)
            }
            val tags = if (type == "Fragewort") listOf(label) else listOf(label, "Conjunctions")
            entity(
                id, word, if (type == "Fragewort") "question_word" else "conjunction", level, label, tags,
                r.c(3), r.c(4), example, r.c(6), r.c(7), gap,
            )
        }
    }
}
