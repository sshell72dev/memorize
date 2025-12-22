import Foundation
import Alamofire

struct YandexGPTRequest: Codable {
    let modelUri: String
    let completionOptions: CompletionOptions
    let messages: [Message]
}

struct CompletionOptions: Codable {
    let stream: Bool
    let temperature: Double
    let maxTokens: String
    
    init(stream: Bool = false, temperature: Double = 0.6, maxTokens: String = "2000") {
        self.stream = stream
        self.temperature = temperature
        self.maxTokens = maxTokens
    }
}

struct Message: Codable {
    let role: String
    let text: String
}

struct YandexGPTResponse: Codable {
    let result: Result
}

struct Result: Codable {
    let alternatives: [Alternative]
}

struct Alternative: Codable {
    let message: Message
    let status: String
}

struct ParsedTextStructure: Codable {
    let sections: [SectionStructure]
}

struct SectionStructure: Codable {
    let paragraphs: [ParagraphStructure]
}

struct ParagraphStructure: Codable {
    let phrases: [String]
}

class YandexGPTService {
    private let apiKey: String
    private let folderId: String
    private let baseURL = "https://llm.api.cloud.yandex.net/"
    
    init(apiKey: String, folderId: String) {
        self.apiKey = apiKey
        self.folderId = folderId
    }
    
    func getTextByTitle(title: String) async throws -> String? {
        let prompt = "Найди и верни полный текст произведения или стихотворения с названием: \(title). Верни только текст без дополнительных комментариев."
        
        let request = YandexGPTRequest(
            modelUri: "gpt://\(folderId)/yandexgpt/latest",
            completionOptions: CompletionOptions(),
            messages: [Message(role: "user", text: prompt)]
        )
        
        let url = baseURL + "foundationModels/v1/completion"
        
        return try await withCheckedThrowingContinuation { continuation in
            AF.request(
                url,
                method: .post,
                parameters: request,
                encoder: JSONParameterEncoder.default,
                headers: [
                    "Authorization": "Api-Key \(apiKey)",
                    "x-folder-id": folderId
                ]
            )
            .validate()
            .responseDecodable(of: YandexGPTResponse.self) { response in
                switch response.result {
                case .success(let gptResponse):
                    let text = gptResponse.result.alternatives.first?.message.text
                    continuation.resume(returning: text)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func parseText(_ text: String) async throws -> ParsedTextStructure? {
        let prompt = """
            Разбей следующий текст на:
            1. Разделы (логические части)
            2. Абзацы/четверостишия внутри разделов
            3. Фразы/предложения внутри абзацев
            
            Верни в формате JSON:
            {
              "sections": [
                {
                  "paragraphs": [
                    {
                      "phrases": ["фраза 1", "фраза 2"]
                    }
                  ]
                }
              ]
            }
            
            Текст:
            \(text)
        """
        
        let request = YandexGPTRequest(
            modelUri: "gpt://\(folderId)/yandexgpt/latest",
            completionOptions: CompletionOptions(),
            messages: [Message(role: "user", text: prompt)]
        )
        
        let url = baseURL + "foundationModels/v1/completion"
        
        return try await withCheckedThrowingContinuation { continuation in
            AF.request(
                url,
                method: .post,
                parameters: request,
                encoder: JSONParameterEncoder.default,
                headers: [
                    "Authorization": "Api-Key \(apiKey)",
                    "x-folder-id": folderId
                ]
            )
            .validate()
            .responseDecodable(of: YandexGPTResponse.self) { response in
                switch response.result {
                case .success(let gptResponse):
                    if let jsonString = gptResponse.result.alternatives.first?.message.text {
                        let parsed = TextParser.parseJson(jsonString)
                        continuation.resume(returning: parsed)
                    } else {
                        continuation.resume(returning: nil)
                    }
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}

