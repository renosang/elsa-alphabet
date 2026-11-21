import SwiftUI

struct GamesHubView: View {
    @EnvironmentObject private var model: AppModel
    var body: some View {
        ZStack {
            SnowTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    Text("Trò chơi phép thuật").font(.system(size: 34, weight: .bold, design: .rounded))
                    gameLink("Chọn từ đúng", "textformat.abc", .pink) { WordGameView(lesson: model.currentLesson) }
                    gameLink("Đếm khối màu", "square.stack.3d.up.fill", .blue) { LegoCountView() }
                    NavigationLink { ColoringStudioView() } label: { gameCard("Sáng tạo & Tô màu", "paintbrush.pointed.fill", .purple) }.buttonStyle(.plain)
                }.padding()
            }
        }.navigationTitle("Kỹ năng")
    }

    private func gameLink<D: View>(_ title: String, _ icon: String, _ color: Color, @ViewBuilder destination: () -> D) -> some View {
        NavigationLink(destination: destination) { gameCard(title, icon, color) }.buttonStyle(.plain)
    }
    private func gameCard(_ title: String, _ icon: String, _ color: Color) -> some View {
        SnowCard { HStack { Image(systemName: icon).font(.system(size: 42)).foregroundStyle(color); Text(title).font(.system(size: 25, weight: .bold, design: .rounded)); Spacer(); Image(systemName: "chevron.right") } }
    }
}

struct WordGameView: View {
    @StateObject private var audio = AudioGuide()
    @State private var choices: [String] = []
    @State private var wrong = Set<String>()
    @State private var correct = false
    @State private var attempts = 0
    @State private var streak = 0
    let lesson: Lesson

    var body: some View {
        ZStack {
            SnowTheme.background.ignoresSafeArea()
            VStack(spacing: 22) {
                Text("Tìm từ có chữ \(lesson.upper)").font(.system(size: 30, weight: .bold, design: .rounded))
                Text(lesson.emoji).font(.system(size: 100)).scaleEffect(correct ? 1.18 : 1)
                ForEach(choices, id: \.self) { choice in
                    Button { choose(choice) } label: { Text(choice).frame(maxWidth: .infinity) }
                        .buttonStyle(KidButtonStyle(color: choice == lesson.word && correct ? .green : wrong.contains(choice) ? SnowTheme.amber.opacity(0.7) : .white))
                        .modifier(ShakeEffect(shakes: wrong.contains(choice) ? 2 : 0))
                }
                if attempts >= 2 && !correct { Text("Gợi ý: đáp án đúng đang lấp lánh ✨").foregroundStyle(SnowTheme.violet).bold() }
                if streak >= 3 { Text("Đúng liền 3 câu luôn! 🎉").font(.title2.bold()) }
            }.padding()
        }.onAppear { makeChoices(); audio.play("world_words") }
    }

    private func makeChoices() {
        var pool = Lesson.all.filter { $0.id != lesson.id }.shuffled().prefix(3).map(\.word)
        pool.append(lesson.word); choices = pool.shuffled()
    }
    private func choose(_ value: String) {
        if value == lesson.word {
            correct = true; streak += 1; wrong.removeAll()
            audio.play(streak >= 3 ? "praise_streak3" : "praise_correct\(Int.random(in: 0...2))")
        } else {
            attempts += 1; streak = 0; wrong.insert(value)
            audio.play("praise_wrong\(Int.random(in: 0...2))")
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { wrong.remove(value) }
        }
    }
}

struct ShakeEffect: GeometryEffect {
    var shakes: CGFloat
    var animatableData: CGFloat { get { shakes } set { shakes = newValue } }
    func effectValue(size: CGSize) -> ProjectionTransform { ProjectionTransform(CGAffineTransform(translationX: 8 * sin(shakes * .pi * 2), y: 0)) }
}

struct LegoCountView: View {
    @State private var count = Int.random(in: 4...9)
    @State private var answer: Int?
    var body: some View {
        ZStack { SnowTheme.background.ignoresSafeArea(); VStack(spacing: 24) {
            Text("Có bao nhiêu khối màu xanh?").font(.system(size: 29, weight: .bold, design: .rounded))
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(68)), count: 3)) { ForEach(0..<count, id: \.self) { _ in RoundedRectangle(cornerRadius: 12).fill(.blue.gradient).frame(width: 62, height: 45).overlay(Circle().fill(.blue.opacity(0.7)).frame(width: 22, height: 10)) } }
            HStack { ForEach([count - 1, count, count + 1, count + 2].shuffled(), id: \.self) { n in Button("\(n)") { answer = n }.buttonStyle(KidButtonStyle(color: answer == n ? (n == count ? .green : SnowTheme.amber) : .white)) } }
        }.padding() }
    }
}

