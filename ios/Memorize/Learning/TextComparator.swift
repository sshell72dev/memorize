import Foundation

class TextComparator {
    private static let dontRememberPhrases = [
        "не помню", "непомню", "не знаю", "незнаю",
        "забыл", "забыла", "не могу вспомнить"
    ]
    
    static func isDontRemember(_ text: String) -> Bool {
        let normalized = text.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        return dontRememberPhrases.contains { normalized.contains($0) }
    }
    
    static func compareTexts(original: String, userText: String) -> Bool {
        let normalizedOriginal = normalizeText(original)
        let normalizedUser = normalizeText(userText)
        
        if normalizedOriginal == normalizedUser {
            return true
        }
        
        let similarity = calculateSimilarity(normalizedOriginal, normalizedUser)
        return similarity >= 0.85
    }
    
    private static func normalizeText(_ text: String) -> String {
        return text.lowercased()
            .replacingOccurrences(of: "[.,!?;:()\\[\\]{}'\"]", with: "", options: .regularExpression)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    private static func calculateSimilarity(_ s1: String, _ s2: String) -> Double {
        let maxLength = max(s1.count, s2.count)
        if maxLength == 0 { return 1.0 }
        
        let distance = levenshteinDistance(s1, s2)
        return 1.0 - (Double(distance) / Double(maxLength))
    }
    
    private static func levenshteinDistance(_ s1: String, _ s2: String) -> Int {
        let m = s1.count
        let n = s2.count
        var dp = Array(repeating: Array(repeating: 0, count: n + 1), count: m + 1)
        
        for i in 0...m { dp[i][0] = i }
        for j in 0...n { dp[0][j] = j }
        
        let s1Array = Array(s1)
        let s2Array = Array(s2)
        
        for i in 1...m {
            for j in 1...n {
                let cost = s1Array[i - 1] == s2Array[j - 1] ? 0 : 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[m][n]
    }
}

