import SwiftUI

struct TextPreviewView: View {
    let title: String
    let author: String?
    let fullText: String
    let onApprove: () -> Void
    let onEdit: () -> Void
    let onRetry: () -> Void
    
    @State private var showFullText = false
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    Text(title)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    if let author = author, !author.isEmpty {
                        Text("Автор: \(author)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                
                Divider()
                
                // Text preview
                VStack(alignment: .leading, spacing: 12) {
                    Text("Предпросмотр текста:")
                        .font(.headline)
                        .padding(.horizontal)
                    
                    if showFullText {
                        Text(fullText)
                            .font(.body)
                            .lineSpacing(4)
                            .padding()
                    } else {
                        Text(String(fullText.prefix(500)) + (fullText.count > 500 ? "..." : ""))
                            .font(.body)
                            .lineSpacing(4)
                            .padding()
                        
                        if fullText.count > 500 {
                            Button(action: {
                                showFullText = true
                            }) {
                                Text("Показать полностью")
                                    .font(.subheadline)
                            }
                            .padding(.horizontal)
                        }
                    }
                }
                
                Divider()
                
                // Action buttons
                VStack(spacing: 12) {
                    Button(action: onApprove) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Одобрить и начать учить")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    
                    HStack(spacing: 12) {
                        Button(action: onEdit) {
                            HStack {
                                Image(systemName: "pencil")
                                Text("Изменить поиск")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        
                        Button(action: onRetry) {
                            HStack {
                                Image(systemName: "arrow.clockwise")
                                Text("Повторить поиск")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.orange)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle("Предпросмотр")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        TextPreviewView(
            title: "Пример стихотворения",
            author: "А.С. Пушкин",
            fullText: "Это длинный текст для предпросмотра. " + String(repeating: "Он содержит много строк. ", count: 50),
            onApprove: {},
            onEdit: {},
            onRetry: {}
        )
    }
}

