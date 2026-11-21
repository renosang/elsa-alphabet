import Foundation

@MainActor
final class AppModel: ObservableObject {
    @Published var completed: Set<String> = [] { didSet { save() } }
    @Published var currentIndex = 0 { didSet { UserDefaults.standard.set(currentIndex, forKey: "currentIndex") } }
    @Published var autoNextSeconds: Double = 4 { didSet { UserDefaults.standard.set(autoNextSeconds, forKey: "autoNextSeconds") } }
    @Published var children: [String] = ["Bé 1"] { didSet { save() } }
    @Published var activeChild = 0

    init() {
        currentIndex = min(UserDefaults.standard.integer(forKey: "currentIndex"), Lesson.all.count - 1)
        let delay = UserDefaults.standard.double(forKey: "autoNextSeconds")
        autoNextSeconds = delay == 0 ? 4 : delay
        if let data = UserDefaults.standard.data(forKey: "completed"), let list = try? JSONDecoder().decode([String].self, from: data) {
            completed = Set(list)
        }
        if let saved = UserDefaults.standard.stringArray(forKey: "children"), !saved.isEmpty { children = saved }
    }

    var currentLesson: Lesson { Lesson.all[currentIndex] }
    var progress: Double { Double(completed.count) / Double(Lesson.all.count) }

    func complete(_ lesson: Lesson) { completed.insert(lesson.id) }
    func moveNext() { currentIndex = (currentIndex + 1) % Lesson.all.count }
    func study(_ lesson: Lesson) { currentIndex = Lesson.all.firstIndex(of: lesson) ?? 0 }

    private func save() {
        UserDefaults.standard.set(try? JSONEncoder().encode(Array(completed)), forKey: "completed")
        UserDefaults.standard.set(children, forKey: "children")
    }
}

