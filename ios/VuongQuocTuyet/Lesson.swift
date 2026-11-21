import Foundation

struct Lesson: Identifiable, Codable, Hashable {
    let id: String
    let upper: String
    let lower: String
    let word: String
    let emoji: String

    static let all: [Lesson] = [
        .init(id: "a", upper: "A", lower: "a", word: "quả táo", emoji: "🍎"),
        .init(id: "aw", upper: "Ă", lower: "ă", word: "cái khăn", emoji: "🧣"),
        .init(id: "aa", upper: "Â", lower: "â", word: "cái cân", emoji: "⚖️"),
        .init(id: "b", upper: "B", lower: "b", word: "bánh mì", emoji: "🍞"),
        .init(id: "c", upper: "C", lower: "c", word: "con cá", emoji: "🐟"),
        .init(id: "d", upper: "D", lower: "d", word: "quả dừa", emoji: "🥥"),
        .init(id: "dd", upper: "Đ", lower: "đ", word: "đèn bàn", emoji: "💡"),
        .init(id: "e", upper: "E", lower: "e", word: "xe đạp", emoji: "🚲"),
        .init(id: "ee", upper: "Ê", lower: "ê", word: "con dê", emoji: "🐐"),
        .init(id: "g", upper: "G", lower: "g", word: "con gà", emoji: "🐔"),
        .init(id: "h", upper: "H", lower: "h", word: "bông hoa", emoji: "🌼"),
        .init(id: "i", upper: "I", lower: "i", word: "cây kim", emoji: "🪡"),
        .init(id: "k", upper: "K", lower: "k", word: "cái kéo", emoji: "✂️"),
        .init(id: "l", upper: "L", lower: "l", word: "quả lê", emoji: "🍐"),
        .init(id: "m", upper: "M", lower: "m", word: "con mèo", emoji: "🐱"),
        .init(id: "n", upper: "N", lower: "n", word: "cái nón", emoji: "👒"),
        .init(id: "o", upper: "O", lower: "o", word: "con ong", emoji: "🐝"),
        .init(id: "oo", upper: "Ô", lower: "ô", word: "cái ô", emoji: "☂️"),
        .init(id: "ow", upper: "Ơ", lower: "ơ", word: "quả ớt", emoji: "🌶️"),
        .init(id: "p", upper: "P", lower: "p", word: "cục pin", emoji: "🔋"),
        .init(id: "q", upper: "Q", lower: "q", word: "quả quýt", emoji: "🍊"),
        .init(id: "r", upper: "R", lower: "r", word: "cái rổ", emoji: "🧺"),
        .init(id: "s", upper: "S", lower: "s", word: "ngôi sao", emoji: "⭐️"),
        .init(id: "t", upper: "T", lower: "t", word: "quả táo", emoji: "🍎"),
        .init(id: "u", upper: "U", lower: "u", word: "đu đủ", emoji: "🥭"),
        .init(id: "uw", upper: "Ư", lower: "ư", word: "sư tử", emoji: "🦁"),
        .init(id: "v", upper: "V", lower: "v", word: "con voi", emoji: "🐘"),
        .init(id: "x", upper: "X", lower: "x", word: "xe buýt", emoji: "🚌"),
        .init(id: "y", upper: "Y", lower: "y", word: "y tá", emoji: "🧑‍⚕️")
    ]
}

