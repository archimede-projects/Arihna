package com.archimedeprojects.arihna.feature.quran

/**
 * Local, offline Tajwid rule matcher for Arihna's Tanzil Uthmani corpus.
 *
 * The rule set is intentionally partial and is surfaced in UI as Beta / main rules.
 * The approach and core rule families are adapted from fcat97/tajweedApi (MIT), but
 * matching here is Uthmani-specific rather than using its IndoPak-only parser directly.
 */
internal enum class TajwidRule {
    QALQALAH,
    IKHFA,
    IQLAB,
    IDGHAM_WITH_GHUNNAH,
    IDGHAM_WITHOUT_GHUNNAH,
    GHUNNAH,
}

internal data class TajwidSpan(
    val start: Int,
    val endExclusive: Int,
    val rule: TajwidRule,
) {
    init {
        require(start >= 0)
        require(endExclusive > start)
    }
}

internal object UthmaniTajwidEngine {
    private const val SUKUN = '\u0652'
    private const val SMALL_HIGH_DOTLESS_HEAD = '\u06E1'
    private const val SHADDA = '\u0651'
    private val tanween = setOf('\u064B', '\u064C', '\u064D')
    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val ikhfaLetters = setOf('ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك')
    private val idghamWithGhunnahLetters = setOf('ي', 'ى', 'و', 'م', 'ن')
    private val idghamWithoutGhunnahLetters = setOf('ر', 'ل')

    private data class Token(
        val start: Int,
        val endExclusive: Int,
        val letter: Char,
        val marks: String,
    )

    fun find(text: String): List<TajwidSpan> {
        if (text.isBlank()) return emptyList()
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return emptyList()
        val spans = mutableListOf<TajwidSpan>()

        tokens.forEachIndexed { index, token ->
            val hasSukun = token.marks.any { it == SUKUN || it == SMALL_HIGH_DOTLESS_HEAD }
            val hasShadda = SHADDA in token.marks
            val hasTanween = token.marks.any(tanween::contains)
            val nunSakin = token.letter == 'ن' && hasSukun

            if (token.letter in qalqalahLetters && hasSukun) {
                spans += token.span(TajwidRule.QALQALAH)
            }
            if ((token.letter == 'ن' || token.letter == 'م') && hasShadda) {
                spans += token.span(TajwidRule.GHUNNAH)
            }

            if (!nunSakin && !hasTanween) return@forEachIndexed
            val next = tokens.getOrNull(index + 1) ?: return@forEachIndexed
            when {
                next.letter == 'ب' -> spans += token.span(TajwidRule.IQLAB)
                next.letter in ikhfaLetters -> spans += token.span(TajwidRule.IKHFA)
                hasWordBoundary(text, token, next) && next.letter in idghamWithGhunnahLetters ->
                    spans += token.span(TajwidRule.IDGHAM_WITH_GHUNNAH)
                hasWordBoundary(text, token, next) && next.letter in idghamWithoutGhunnahLetters ->
                    spans += token.span(TajwidRule.IDGHAM_WITHOUT_GHUNNAH)
            }
        }

        return spans
            .distinctBy { Triple(it.start, it.endExclusive, it.rule) }
            .sortedWith(compareBy<TajwidSpan> { it.start }.thenBy { it.endExclusive }.thenBy { it.rule.ordinal })
            .also { validate(text, it) }
    }

    private fun Token.span(rule: TajwidRule) = TajwidSpan(start, endExclusive, rule)

    private fun hasWordBoundary(text: String, current: Token, next: Token): Boolean =
        text.substring(current.endExclusive, next.start).any { it.isWhitespace() }

    private fun validate(text: String, spans: List<TajwidSpan>) {
        var previousEnd = -1
        spans.forEach { span ->
            require(span.start in text.indices)
            require(span.endExclusive in 1..text.length)
            require(span.start >= previousEnd) { "Overlapping Tajwid spans are not allowed" }
            previousEnd = span.endExclusive
        }
    }

    private fun tokenize(text: String): List<Token> {
        val result = mutableListOf<Token>()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (!isArabicLetter(char)) {
                index++
                continue
            }
            val start = index
            index++
            while (index < text.length && !isArabicLetter(text[index]) && !text[index].isWhitespace()) {
                index++
            }
            result += Token(
                start = start,
                endExclusive = index,
                letter = char,
                marks = text.substring(start + 1, index),
            )
        }
        return result
    }

    private fun isArabicLetter(char: Char): Boolean {
        val block = Character.UnicodeBlock.of(char)
        return Character.isLetter(char) && (
            block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
            )
    }
}
