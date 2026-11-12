package com.wortart.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** False when the learner turned Bengali off: no Bengali text is shown anywhere in the app. */
val LocalShowBengali = compositionLocalOf { true }

private val BENGALI_CHAR = Regex("[\u0980-\u09FF]")
private val LATIN_LETTER = Regex("[A-Za-z]")
private val BENGALI_TAIL = Regex("\\s*[/·|]\\s*[\u0980-\u09FF].*$")

/**
 * Removes the Bengali part of a text: "to learn / শেখা" becomes "to learn", a line that is only
 * Bengali disappears.
 */
fun stripBengali(text: String): String =
    text.lines()
        .mapNotNull { line ->
            val cut = line.replace(BENGALI_TAIL, "")
            when {
                !BENGALI_CHAR.containsMatchIn(cut) -> cut
                LATIN_LETTER.containsMatchIn(cut) -> cut
                else -> null
            }
        }
        .joinToString("\n")
        .trim()

/** This text without its Bengali part when Bengali is switched off. */
@Composable
fun String.noBn(): String = if (LocalShowBengali.current) this else stripBengali(this)
