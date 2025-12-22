import Foundation

class TextParser {
    static func parseJson(_ jsonString: String) -> ParsedTextStructure? {
        // Clean JSON from markdown code blocks if present
        let cleanedJson = jsonString
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        
        guard let data = cleanedJson.data(using: .utf8) else {
            return nil
        }
        
        do {
            let decoder = JSONDecoder()
            let structure = try decoder.decode(ParsedTextStructure.self, from: data)
            return structure
        } catch {
            print("Error parsing JSON: \(error)")
            return nil
        }
    }
}

