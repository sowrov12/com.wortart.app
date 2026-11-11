package com.wortart.app.data.local.entity

/** All group labels of a word (its primary topic and its tags). */
fun VocabEntity.groupLabels(): List<String> {
    val fromTags = tags
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    return if (fromTags.isEmpty()) listOf(topic) else fromTags
}

fun VocabEntity.isVerb(): Boolean = category in WordCategories.ALL_VERBS

fun VocabEntity.isSeparable(): Boolean =
    category == WordCategories.SEPARABLE_VERB || WordGroups.SEPARABLE in groupLabels()

/** Words of the same kind give wrong options for each other (all verbs together, and so on). */
fun VocabEntity.kindKey(): String = if (isVerb()) "verb" else category
