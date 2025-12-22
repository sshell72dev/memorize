import SwiftUI

struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    let onTextSelected: (String) -> Void
    let onFileSelected: () -> Void
    let onTextInput: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Поиск текста")
                .font(.headline)
                .padding()
            
            TextField(
                "Введите название текста или стихотворения",
                text: $viewModel.searchQuery
            )
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.horizontal)
            
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
    }
}

#Preview {
    SearchView(onTextSelected: { _ in })
}

