import Foundation
import SwiftUI

class TextInputViewModel: ObservableObject {
    @Published var title: String = ""
    @Published var textContent: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    
    private let database: MemorizeDatabase
    private let textService: TextService
    
    init(database: MemorizeDatabase, textService: TextService) {
        self.database = database
        self.textService = textService
    }
    
    func saveText(completion: @escaping (String) -> Void) {
        let title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let textContent = textContent.trimmingCharacters(in: .whitespacesAndNewlines)
        
        guard !title.isEmpty && !textContent.isEmpty else {
            error = "Заполните все поля"
            return
        }
        
        isLoading = true
        error = nil
        
        Task {
            do {
                let textId = try await textService.saveTextDirectly(title: title, fullText: textContent)
                await MainActor.run {
                    isLoading = false
                    if let textId = textId {
                        completion(textId)
                    } else {
                        error = "Не удалось сохранить текст"
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
}

