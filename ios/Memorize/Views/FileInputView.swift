import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct FileInputView: View {
    @State private var title: String = ""
    @State private var fileContent: String? = nil
    @State private var isLoading: Bool = false
    @State private var error: String? = nil
    
    let fileURL: URL
    let onTextSaved: (String) -> Void
    let onCancel: () -> Void
    let database: MemorizeDatabase
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Загрузка файла")
                .font(.headline)
                .padding()
            
            if fileContent == nil {
                ProgressView()
                Text("Чтение файла...")
                    .onAppear {
                        loadFile(from: fileURL)
                    }
            } else {
                TextField("Название текста", text: $title)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)
                
                Text("Содержимое файла (\(fileContent?.count ?? 0) символов):")
                    .font(.caption)
                    .padding(.horizontal)
                
                ScrollView {
                    Text(fileContent ?? "")
                        .padding()
                }
                .frame(maxHeight: 300)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                )
                .padding(.horizontal)
                
                HStack(spacing: 16) {
                    Button("Отмена") {
                        onCancel()
                    }
                    .buttonStyle(.bordered)
                    
                    Button("Сохранить и начать учить") {
                        saveText()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isLoading || title.isEmpty || fileContent == nil)
                }
                .padding()
                
                if let error = error {
                    Text(error)
                        .foregroundColor(.red)
                        .padding()
                }
            }
            
            Spacer()
        }
        .padding()
    }
    
    private func loadFile(from url: URL) {
        // Start accessing security-scoped resource
        guard url.startAccessingSecurityScopedResource() else {
            error = "Нет доступа к файлу"
            return
        }
        
        defer { url.stopAccessingSecurityScopedResource() }
        
        do {
            let content = try String(contentsOf: url, encoding: .utf8)
            fileContent = content
            title = url.deletingPathExtension().lastPathComponent
        } catch {
            self.error = "Ошибка чтения файла: \(error.localizedDescription)"
        }
    }
    
    private func saveText() {
        guard let content = fileContent, !title.isEmpty else {
            error = "Заполните все поля"
            return
        }
        
        isLoading = true
        error = nil
        
        Task {
            do {
                let apiKey = Config.yandexAPIKey
                let folderId = Config.yandexFolderId
                let yandexGPTService = YandexGPTService(apiKey: apiKey, folderId: folderId)
                let textService = TextService(database: database, yandexGPTService: yandexGPTService)
                
                let textId = try await textService.saveTextDirectly(title: title, fullText: content)
                await MainActor.run {
                    isLoading = false
                    if let textId = textId {
                        onTextSaved(textId)
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

#Preview {
    FileInputView(
        onTextSaved: { _ in },
        onCancel: {},
        database: MemorizeDatabase()
    )
}

