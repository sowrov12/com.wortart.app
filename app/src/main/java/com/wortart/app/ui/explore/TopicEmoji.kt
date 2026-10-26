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
    else -> "📚"
}
