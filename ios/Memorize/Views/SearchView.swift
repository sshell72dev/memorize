import SwiftUI

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    let onTextSelected: (String) -> Void
    let onFileSelected: () -> Void
    let onTextInput: () -> Void
    @State private var showPreview = false
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Поиск текста")
                .font(.headline)
                .padding()
            
            TextField(
                "Введите название и автора (например: \"Зимнее утро Пушкин\" или \"автор: Пушкин Зимнее утро\")",
                text: $viewModel.searchQuery
            )
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.horizontal)
            .onSubmit {
                viewModel.search()
            }
            
            Button(action: {
                viewModel.search()
            }) {
                if viewModel.isLoading {
                    HStack {
                        ProgressView()
                        Text("Загрузка...")
                    }
                } else {
                    Text("Найти и начать учить")
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(viewModel.isLoading || viewModel.searchQuery.isEmpty)
            
            if let error = viewModel.error {
                Text(error)
                    .foregroundColor(.red)
                    .padding()
            }
            
            Divider()
                .padding(.vertical)
            
            Text("Или загрузите свой текст")
                .font(.subheadline)
            
            HStack(spacing: 16) {
                Button("📁 Загрузить файл") {
                    onFileSelected()
                }
                .buttonStyle(.bordered)
                
                Button("✏️ Ввести текст") {
                    onTextInput()
                }
                .buttonStyle(.bordered)
            }
            
            if !viewModel.savedTexts.isEmpty {
                Text("Сохраненные тексты:")
                    .font(.headline)
                    .padding(.top)
                
                List(viewModel.savedTexts) { text in
                    Button(action: {
                        onTextSelected(text.id)
                    }) {
                        Text(text.title)
                    }
                }
            }
            
            Spacer()
        }
        .padding()
        .sheet(isPresented: $showPreview) {
            if let foundText = viewModel.foundText {
                NavigationStack {
                    TextPreviewView(
                        title: foundText.title,
                        author: foundText.author,
                        fullText: foundText.fullText,
                        onApprove: {
                            Task {
                                do {
                                    if let textId = try await viewModel.approveAndSave() {
                                        await MainActor.run {
                                            showPreview = false
                                            onTextSelected(textId)
                                        }
                                    }
                                } catch {
                                    await MainActor.run {
                                        viewModel.error = "Ошибка сохранения: \(error.localizedDescription)"
                                    }
                                }
                            }
                        },
                        onEdit: {
                            showPreview = false
                        },
                        onRetry: {
                            showPreview = false
                            viewModel.search()
                        }
                    )
                }
            }
        }
        .onChange(of: viewModel.foundText) { oldValue, newValue in
            if newValue != nil {
                showPreview = true
            }
        }
    }
}

#Preview {
    SearchView(onTextSelected: { _ in })
}

