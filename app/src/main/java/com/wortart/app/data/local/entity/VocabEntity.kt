package com.wortart.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single German vocabulary card.
 *
 * The German word itself is [word]; English and Bengali translations are stored
 * in separate columns. Verb- and adjective-specific columns are null for other
 * word types.
 */
@Entity(
    tableName = "vocab_table",
    indices = [
        Index(value = ["level"]),
        Index(value = ["category"]),
        Index(value = ["topic"]),
    ],
)
data class VocabEntity(
    @PrimaryKey val id: String,
    val word: String,
    /** See [WordCategories]: regular_verb, irregular_verb, separable_verb, adjective, adverb. */
    val category: String,
    /** See [CefrLevels]: A1, A2, B1, B2. */
    val level: String,
    /** Theme used by Explore filters, e.g. Food, Health, Career. */
    val topic: String,
    val translationEn: String,
    val translationBn: String,
    val exampleDe: String,
    val exampleEn: String,
    val exampleBn: String,

    // Verb-specific fields
    /** haben or sein. */
    val auxiliary: String? = null,
    val praeteritum: String? = null,
    val partizip2: String? = null,
    /** Separable prefix, e.g. "ein" for einkaufen. */
    val prefix: String? = null,
    /** Stammform shown in the word detail view, e.g. "er geht". */
    val stammform: String? = null,

    // Adjective-specific fields
    val komparativ: String? = null,
    val superlativ: String? = null,

    /** Used for adjective/adverb "choose the antonym" questions. */
    val antonym: String? = null,

    val isFavorite: Boolean = false,

    // Flashcard set data (generated from the word lists)
    /** Comma-separated group labels, e.g. ",Irregular Verbs,Separable Verbs,". */
    val tags: String? = null,
    /** German example with the answer replaced by a gap, for the "fill the blank" set. */
    val blankDe: String? = null,
    /** The word that belongs into the gap of [blankDe]. */
    val blankAnswer: String? = null,
    /** Two wrong options for the gap, separated by "|". */
    val blankWrong: String? = null,
    /** Two wrong Partizip II options, separated by "|". */
    val pz2Wrong: String? = null,
    /** German example with the separable prefix replaced by a gap. */
    val prefixBlankDe: String? = null,
    /** The word with the separable prefix replaced by a gap, e.g. "_____rufen". */
    val prefixBlankTitle: String? = null,
    /** Short meaning of [antonym], e.g. "bad / খারাপ". */
    val antonymMeaning: String? = null,
    /** A wrong antonym option, already formatted like "klein — small / ছোট". */
    val antonymWrong: String? = null,
)
