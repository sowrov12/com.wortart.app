package com.wortart.app.data.local.seed

import com.wortart.app.data.local.entity.Auxiliaries
import com.wortart.app.data.local.entity.CefrLevels
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.local.entity.WordCategories
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the bundled vocabulary JSON (assets/vocab_seed.json) into [VocabEntity] rows.
 * Throws [IllegalArgumentException] with the offending item id if the data is invalid,
 * so a bad dataset is caught immediately instead of reaching the database.
 */
object VocabSeedParser {

    fun parse(json: String): List<VocabEntity> {
        val array = JSONArray(json)
        val items = List(array.length()) { index -> parseItem(array.getJSONObject(index), index) }
        val duplicates = items.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate ids in seed data: $duplicates" }
        return items
    }

    private fun parseItem(obj: JSONObject, index: Int): VocabEntity {
        fun required(key: String): String {
            val value = obj.optString(key, "")
            require(value.isNotBlank()) { "Seed item #$index: missing '$key'" }
            return value
        }

        fun optional(key: String): String? =
            if (obj.isNull(key)) null else obj.optString(key).takeIf { it.isNotBlank() }

        val entity = VocabEntity(
            id = required("id"),
            word = required("word"),
            category = required("category"),
            level = required("level"),
            topic = required("topic"),
            translationEn = required("translationEn"),
            translationBn = required("translationBn"),
            exampleDe = required("exampleDe"),
            exampleEn = required("exampleEn"),
            exampleBn = required("exampleBn"),
            auxiliary = optional("auxiliary"),
            praeteritum = optional("praeteritum"),
            partizip2 = optional("partizip2"),
            prefix = optional("prefix"),
            stammform = optional("stammform"),
            komparativ = optional("komparativ"),
            superlativ = optional("superlativ"),
            antonym = optional("antonym"),
        )
        validate(entity)
        return entity
    }

    private fun validate(e: VocabEntity) {
        require(e.category in WordCategories.ALL) { "${e.id}: unknown category '${e.category}'" }
        require(e.level in CefrLevels.ALL) { "${e.id}: unknown level '${e.level}'" }

        if (e.category in WordCategories.VERBS) {
            require(e.auxiliary == Auxiliaries.HABEN || e.auxiliary == Auxiliaries.SEIN) {
                "${e.id}: verb needs auxiliary haben or sein"
            }
        }
        if (e.category == WordCategories.IRREGULAR_VERB) {
            require(e.praeteritum != null && e.partizip2 != null) {
                "${e.id}: irregular verb needs praeteritum and partizip2"
            }
        }
        if (e.category == WordCategories.SEPARABLE_VERB) {
            require(e.prefix != null) { "${e.id}: separable verb needs prefix" }
        }
        if (e.category == WordCategories.ADJECTIVE) {
            require(e.komparativ != null && e.superlativ != null) {
                "${e.id}: adjective needs komparativ and superlativ"
            }
        }
    }
}
