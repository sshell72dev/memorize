import SwiftUI
import UniformTypeIdentifiers

@main
struct MemorizeApp: App {
    var body: some Scene {
        WindowGroup {
            AppNavigationView()
        }
    }
}

struct AppNavigationView: View {
    @State private var navigationPath = NavigationPath()
    @State private var selectedTextId: String? = nil
    @State private var showFilePicker = false
    @State private var selectedFileURL: URL? = nil
    
    private let database = MemorizeDatabase()
    
    var body: some View {
        NavigationStack(path: $navigationPath) {
            SearchView(
                onTextSelected: { textId in
                    selectedTextId = textId
                    navigationPath.append("learning")
                },
                onFileSelected: {
                    showFilePicker = true
                },
                onTextInput: {
                    navigationPath.append("textInput")
                }
            )
            .navigationDestination(for: String.self) { destination in
                if destination == "learning" {
                    if let textId = selectedTextId {
                        LearningView(textId: textId) { sessionId in
                            navigationPath.append("statistics")
                        }
                    }
                } else if destination == "textInput" {
                    TextInputView(
                        onTextSaved: { textId in
                            selectedTextId = textId
                            navigationPath.append("learning")
                        },
                        onCancel: {
                            navigationPath.removeLast()
                        },
                        database: database
                    )
                } else if destination == "fileInput" {
                    if let fileURL = selectedFileURL {
                        FileInputView(
                            fileURL: fileURL,
                            onTextSaved: { textId in
                                selectedTextId = textId
                                selectedFileURL = nil
                                navigationPath.append("learning")
                            },
                            onCancel: {
                                selectedFileURL = nil
                                navigationPath.removeLast()
                            },
                            database: database
                        )
                    }
                } else if destination.starts(with: "statistics") {
                    StatisticsView(sessionId: "test", onBack: {
                        navigationPath.removeLast(navigationPath.count)
                    })
                }
            }
            .sheet(isPresented: $showFilePicker) {
                DocumentPicker { url in
                    showFilePicker = false
                    selectedFileURL = url
                    navigationPath.append("fileInput")
                }
            }
        }
    }
}

struct DocumentPicker: UIViewControllerRepresentable {
    let onDocumentPicked: (URL) -> Void
    
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.text, .plainText])
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onDocumentPicked: onDocumentPicked)
    }
    
    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onDocumentPicked: (URL) -> Void
        
        init(onDocumentPicked: @escaping (URL) -> Void) {
            self.onDocumentPicked = onDocumentPicked
        }
        
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            if let url = urls.first {
                onDocumentPicked(url)
            }
        }
    }
}
