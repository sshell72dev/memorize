import Foundation
import SwiftUI

struct FoundText {
    let title: String
    let author: String?
    let fullText: String
}

class SearchViewModel: ObservableObject {
    @Published var searchQuery: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var savedTexts: [TextModel] = []
    @Published var foundText: FoundText? = nil
    
    private let database: MemorizeDatabase
    private let textService: TextService
    
    init() {
        // Initialize with your API keys - these should come from configuration
        let apiKey = Config.yandexAPIKey
        let folderId = Config.yandexFolderId
        
        self.database = MemorizeDatabase()
        let yandexGPTService = YandexGPTService(apiKey: apiKey, folderId: folderId)
        self.textService = TextService(database: database, yandexGPTService: yandexGPTService)
        
        loadSavedTexts()
    }
    
    func search() {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }
        
        isLoading = true
        error = nil
        foundText = nil
        
        Task {
            do {
                // Parse query to extract title and author
                let (title, author) = parseQuery(query)
                
                // Get text from AI for preview
                guard let fullText = try await textService.getTextByTitle(title: title, author: author) else {
                    await MainActor.run {
                        isLoading = false
                        error = "Не удалось найти текст. Попробуйте другое название или укажите автора."
                    }
                    return
                }
                
                await MainActor.run {
                    isLoading = false
                    foundText = FoundText(title: title, author: author, fullText: fullText)
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    error = "Ошибка: \(error.localizedDescription)"
                }
            }
        }
    }
    
    func approveAndSave() async throws -> String? {
        guard let found = foundText else { return nil }
        return try await textService.loadAndSaveText(title: found.title, author: found.author)
    }
    
    private func parseQuery(_ query: String) -> (title: String, author: String?) {
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Check if query contains author indicators like "автор:", "by", etc.
        if let authorRange = trimmedQuery.range(of: "автор:", options: .caseInsensitive) {
            let afterKeyword = String(trimmedQuery[authorRange.upperBound...]).trimmingCharacters(in: .whitespaces)
            let beforeKeyword = String(trimmedQuery[..<authorRange.lowerBound]).trimmingCharacters(in: .whitespaces)
            
            // Format: "автор: Пушкин Зимнее утро" or "Зимнее утро автор: Пушкин"
            if !afterKeyword.isEmpty {
                if beforeKeyword.isEmpty {
                    // "автор: Пушкин" - everything after is author, but we need title
                    // Try to split: last word might be title
                    let parts = afterKeyword.components(separatedBy: CharacterSet.whitespaces)
                    if parts.count > 1 {
                        let author = parts.dropLast().joined(separator: " ")
                        let title = parts.last!
                        return (title, author)
                    }
                    return (afterKeyword, nil) // Can't determine, use as title
                } else {
                    // "Зимнее утро автор: Пушкин"
                    return (beforeKeyword, afterKeyword)
                }
            }
        }
        
        // Check for "by" keyword (English)
        if let byRange = trimmedQuery.range(of: " by ", options: .caseInsensitive) {
            let beforeBy = String(trimmedQuery[..<byRange.lowerBound]).trimmingCharacters(in: .whitespaces)
            let afterBy = String(trimmedQuery[byRange.upperBound...]).trimmingCharacters(in: .whitespaces)
            if !beforeBy.isEmpty && !afterBy.isEmpty {
                return (beforeBy, afterBy)
            }
        }
        
        // If query is short (1-2 words), assume it's just title
        let parts = trimmedQuery.components(separatedBy: CharacterSet.whitespaces).filter { !$0.isEmpty }
        if parts.count <= 2 {
            return (trimmedQuery, nil)
        }
        
        // For longer queries, try heuristic: last 1-2 words might be author
        // Common pattern: "название произведения автор фамилия"
        if parts.count >= 3 {
            // Try last word as author
            let possibleAuthor = parts.last!
            let possibleTitle = parts.dropLast().joined(separator: " ")
            
            // If last word looks like a name (capitalized, short), it might be author
            if possibleAuthor.count <= 15 && possibleAuthor.first?.isUppercase == true {
                return (possibleTitle, possibleAuthor)
            }
            
            // Try last 2 words as author (e.g., "А.С. Пушкин")
            if parts.count >= 4 {
                let possibleAuthor2 = parts.suffix(2).joined(separator: " ")
                let possibleTitle2 = parts.dropLast(2).joined(separator: " ")
                return (possibleTitle2, possibleAuthor2)
            }
        }
        
        // Default: use entire query as title
        return (trimmedQuery, nil)
    }
    
    private func loadSavedTexts() {
        savedTexts = TextRepository(database: database).getAllTexts()
    }
}

