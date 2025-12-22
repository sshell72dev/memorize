package com.memorize.learning

object TextComparator {
    private val dontRememberPhrases = listOf(
        "не помню", "непомню", "не знаю", "незнаю",
        "забыл", "забыла", "не помню", "не могу вспомнить"
    )
    
    fun isDontRemember(text: String): Boolean {
        val normalized = text.lowercase().trim()
        return dontRememberPhrases.any { normalized.contains(it) }
    }
    
    fun compareTexts(original: String, userText: String): Boolean {
        val normalizedOriginal = normalizeText(original)
        val normalizedUser = normalizeText(userText)
        
        // Exact match
        if (normalizedOriginal == normalizedUser) return true
        
        // Calculate similarity using Levenshtein distance
        val similarity = calculateSimilarity(normalizedOriginal, normalizedUser)
        
        // Consider correct if similarity is above 85%
        return similarity >= 0.85
    }
    
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[.,!?;:()\\[\\]{}'\"]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0
        
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLength)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[m][n]
    }
}

