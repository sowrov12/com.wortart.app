package com.wortart.app.ui.explore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TopicEmojiTest {

    @Test
    fun everySeedTopicHasItsOwnEmoji() {
        // Topics used in vocab_seed.json, except "General" which uses the default.
        listOf(
            "Career", "Communication", "Connectors", "Daily Life", "Education", "Emotions",
            "Food", "Health", "Home", "Movement", "Place", "Shopping", "Time", "Travel",
        ).forEach { topic ->
            assertNotEquals("Missing emoji for $topic", "📚", topicEmoji(topic))
        }
    }

    @Test
    fun unknownTopicsFallBackToABook() {
        assertEquals("📚", topicEmoji("General"))
        assertEquals("📚", topicEmoji("Something new"))
    }
}
