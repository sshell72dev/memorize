import Foundation
import SwiftUI

class SearchViewModel: ObservableObject {
    @Published var searchQuery: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var savedTexts: [TextModel] = []
    
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
        
        Task {
            do {
                let textId = try await textService.loadAndSaveText(title: query)
                await MainActor.run {
                    isLoading = false
                    if textId != nil {
                        // Navigation will be handled by parent
                    } else {
                        error = "Не удалось найти текст. Попробуйте другое название."
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    error = "Ошибка: \(error.localizedDescription)"
                }
            }
        }
    }
    
    private func loadSavedTexts() {
        savedTexts = TextRepository(database: database).getAllTexts()
    }
}

