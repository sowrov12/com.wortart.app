package com.wortart.app.ui.explore

/** Emoji shown on a topic card. Unknown topics get a book. */
fun topicEmoji(topic: String): String = when (topic) {
    "Food" -> "🍽️"
    "Health" -> "🩺"
    "Career" -> "💼"
    "Travel" -> "✈️"
    "Shopping" -> "🛍️"
    "Education" -> "🎓"
    "Home" -> "🏠"
    "Daily Life" -> "☀️"
    "Movement" -> "🚶"
    "Communication" -> "💬"
    "Time" -> "⏰"
    "Emotions" -> "😊"
    "Place" -> "📍"
    "Connectors" -> "🔗"
    "Regular Verbs" -> "🔁"
    "Irregular Verbs" -> "⚡"
    "Modal Verbs" -> "🔑"
    "Separable Verbs" -> "✂️"
    "Inseparable Verbs" -> "🧷"
    "Reflexive Verbs" -> "🪞"
    "Sein Verbs" -> "🚶"
    "Dativ Verbs" -> "🎁"
    "Prepositional Verbs" -> "📌"
    "Adjectives" -> "🎨"
    "Adverbs" -> "💨"
    "Prepositions", "Akkusativ Prepositions", "Dativ Prepositions", "Genitiv Prepositions",
    "Wechselpräpositionen", "Temporal Prepositions" -> "📍"
    "Conjunctions", "Coordinating Conjunctions", "Subordinating Conjunctions",
    "Two-part Conjunctions" -> "🔗"
    "Question Words" -> "❓"
    else -> "📚"
}
