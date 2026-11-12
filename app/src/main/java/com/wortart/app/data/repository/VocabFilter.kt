package com.wortart.app.data.repository

/**
 * Describes which words to load. Every field is optional;
 * the default instance means "all words".
 *
 * @param query free text matched against German, English and Bengali text.
 * @param level CEFR level (A1, A2, B1, B2) or null for all levels.
 * @param category word type, see WordCategories, or null for all types.
 * @param topic theme such as Food, Health, Career, or null for all topics.
 * @param includeBengali false to keep Bengali text out of the search.
 * @param favoritesOnly true to list only favorite words.
 */
data class VocabFilter(
    val query: String = "",
    val level: String? = null,
    val category: String? = null,
    val topic: String? = null,
    val favoritesOnly: Boolean = false,
    /** False: the search text is not matched against Bengali text. */
    val includeBengali: Boolean = true,
)
