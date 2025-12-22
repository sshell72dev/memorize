import SwiftUI

struct LearningView: View {
    @StateObject private var viewModel: LearningViewModel
    let textId: String
    let onComplete: (String) -> Void
    
    init(textId: String, onComplete: @escaping (String) -> Void) {
        self.textId = textId
        self.onComplete = onComplete
        _viewModel = StateObject(wrappedValue: LearningViewModel(textId: textId, onComplete: onComplete))
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Progress indicator
            Text("Раздел \(viewModel.currentSection + 1)/\(viewModel.totalSections) | " +
                 "Абзац \(viewModel.currentParagraph + 1)/\(viewModel.totalParagraphs) | " +
                 "Фраза \(viewModel.currentPhrase + 1)/\(viewModel.totalPhrases)")
                .font(.caption)
                .padding()
            
            // Current phrase display
            Card {
                VStack {
                    if !viewModel.currentPhraseText.isEmpty {
                        Text(viewModel.currentPhraseText)
                            .font(.title2)
                            .multilineTextAlignment(.center)
                            .padding()
                    }
                    
                    if viewModel.isListening {
                        ProgressView()
                        Text("Слушаю...")
                            .padding(.top)
                    }
                    
                    if let feedback = viewModel.feedback {
                        Text(feedback)
                            .foregroundColor(viewModel.isCorrect ? .green : .red)
                            .padding(.top)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            
            // Control buttons
            HStack(spacing: 16) {
                Button("Не помню") {
                    viewModel.onDontRemember()
                }
                .buttonStyle(.bordered)
                .tint(.red)
                
                if viewModel.canContinue {
                    Button("Далее") {
                        viewModel.continueToNext()
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding()
        }
        .padding()
    }
}

#Preview {
    LearningView(textId: "test", onComplete: { _ in })
}

