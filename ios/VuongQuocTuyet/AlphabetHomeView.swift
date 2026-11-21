import SwiftUI

struct AlphabetHomeView: View {
    @EnvironmentObject private var model: AppModel
    private let columns = [GridItem(.adaptive(minimum: 92), spacing: 12)]

    var body: some View {
        ZStack {
            SnowTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    hero
                    HStack {
                        Text("Bảng chữ cái").font(.system(size: 30, weight: .bold, design: .rounded))
                        Spacer()
                        Text("\(model.completed.count)/29 chữ").foregroundStyle(SnowTheme.teal)
                    }
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(Lesson.all) { lesson in
                            NavigationLink(value: lesson) { letterCard(lesson) }
                                .buttonStyle(.plain)
                                .simultaneousGesture(TapGesture().onEnded { model.study(lesson) })
                        }
                    }
                }.padding()
            }
        }
        .navigationDestination(for: Lesson.self) { LessonView(lesson: $0) }
        .navigationTitle("Vương Quốc Tuyết")
    }

    private var hero: some View {
        SnowCard {
            HStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Cùng Công chúa Tuyết").font(.caption.bold()).foregroundStyle(SnowTheme.violet)
                    Text("Hồ Băng").font(.system(size: 36, weight: .bold, design: .rounded))
                    ProgressView(value: model.progress).tint(SnowTheme.teal)
                    NavigationLink(value: model.currentLesson) {
                        Label("Học tiếp chữ \(model.currentLesson.upper)", systemImage: "play.fill")
                    }.buttonStyle(KidButtonStyle())
                }
                Spacer()
                Text("👸🏻❄️").font(.system(size: 64)).accessibilityLabel("Công chúa Tuyết")
            }
        }
    }

    private func letterCard(_ lesson: Lesson) -> some View {
        VStack(spacing: 6) {
            Text("\(lesson.upper) \(lesson.lower)").font(.system(size: 32, weight: .medium, design: .rounded)).foregroundStyle(SnowTheme.teal)
            Text(lesson.emoji).font(.system(size: 38))
            if model.completed.contains(lesson.id) { Image(systemName: "checkmark.circle.fill").foregroundStyle(.green) }
        }
        .frame(maxWidth: .infinity, minHeight: 118)
        .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 22))
        .shadow(color: SnowTheme.violet.opacity(0.12), radius: 8, y: 5)
    }
}

