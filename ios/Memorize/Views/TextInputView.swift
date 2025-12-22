import SwiftUI

struct TextInputView: View {
    @StateObject private var viewModel: TextInputViewModel
    let onTextSaved: (String) -> Void
    let onCancel: () -> Void
    
    init(
        onTextSaved: @escaping (String) -> Void,
        onCancel: @escaping () -> Void,
        database: MemorizeDatabase
    ) {
        let apiKey = Config.yandexAPIKey
        let folderId = Config.yandexFolderId
        let yandexGPTService = YandexGPTService(apiKey: apiKey, folderId: folderId)
        let textService = TextService(database: database, yandexGPTService: yandexGPTService)
        
        _viewModel = StateObject(wrappedValue: TextInputViewModel(
            database: database,
            textService: textService
        ))
        self.onTextSaved = onTextSaved
        self.onCancel = onCancel
    }
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Введите текст вручную")
                .font(.headline)
                .padding()
            
            TextField("Название текста", text: $viewModel.title)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)
            
            TextEditor(text: $viewModel.textContent)
                .frame(minHeight: 200)
                .padding(.horizontal)
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
                    viewModel.saveText { textId in
                        onTextSaved(textId)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isLoading || viewModel.title.isEmpty || viewModel.textContent.isEmpty)
            }
            .padding()
            
            if viewModel.isLoading {
                ProgressView()
            }
            
            if let error = viewModel.error {
                Text(error)
                    .foregroundColor(.red)
                    .padding()
            }
            
            Spacer()
        }
        .padding()
    }
}

#Preview {
    TextInputView(
        onTextSaved: { _ in },
        onCancel: {},
        database: MemorizeDatabase()
    )
}

