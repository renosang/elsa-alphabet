import SwiftUI

struct LessonView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @StateObject private var audio = AudioGuide()
    @State private var listened = false
    @State private var sparkle = false
    let lesson: Lesson

    var body: some View {
        GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height
            ZStack {
                SnowTheme.background.ignoresSafeArea()
                Group {
                    if landscape {
                        HStack(spacing: 22) { lessonCard.frame(maxWidth: .infinity); controls.frame(width: geo.size.width * 0.32) }
                    } else {
                        VStack(spacing: 18) { lessonCard; controls }
                    }
                }.padding()
                if sparkle { sparkleOverlay.allowsHitTesting(false) }
            }
        }
        .navigationBarBackButtonHidden()
        .toolbar { ToolbarItem(placement: .topBarLeading) { Button { audio.stop(); dismiss() } label: { Label("Bảng chữ", systemImage: "chevron.left") } } }
        .onAppear { playIntroduction() }
        .onDisappear { audio.stop() }
    }

    private var lessonCard: some View {
        SnowCard {
            VStack(spacing: 14) {
                Text(listened ? "Con đã nghe đủ ba lượt" : "Phát âm theo cùng cô nhé")
                    .font(.system(size: 20, weight: .semibold, design: .rounded)).foregroundStyle(SnowTheme.teal)
                HStack(spacing: 30) {
                    Text("\(lesson.upper) \(lesson.lower)").font(.system(size: 72, weight: .medium, design: .rounded)).foregroundStyle(SnowTheme.teal)
                    Text(lesson.emoji).font(.system(size: 88))
                }
                Text(lesson.word).font(.system(size: 32, weight: .bold, design: .rounded))
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var controls: some View {
        VStack(spacing: 14) {
            Text(listened ? "Giỏi lắm! Con sẵn sàng chơi rồi." : "Cùng nghe Âm \(lesson.upper) ba lần nhé!")
                .font(.system(size: 21, weight: .bold, design: .rounded)).multilineTextAlignment(.center)
            Button { playSample() } label: { Label("Nghe lại mẫu", systemImage: "speaker.wave.2.fill").frame(maxWidth: .infinity) }
                .buttonStyle(KidButtonStyle(color: .white))
            NavigationLink {
                WordGameView(lesson: lesson)
            } label: {
                Label("Chơi với chữ \(lesson.upper)", systemImage: "sparkles").frame(maxWidth: .infinity)
            }
            .buttonStyle(KidButtonStyle(color: listened ? SnowTheme.mint : .gray.opacity(0.25)))
            .disabled(!listened)
            if listened {
                Button { completeAndNext() } label: { Label("Học chữ tiếp theo", systemImage: "arrow.right.circle.fill").frame(maxWidth: .infinity) }
                    .buttonStyle(KidButtonStyle(color: .yellow.opacity(0.7)))
            }
        }
    }

    private func playIntroduction() {
        let firstToday = UserDefaults.standard.string(forKey: "iosStudyDay") != Self.today
        UserDefaults.standard.set(Self.today, forKey: "iosStudyDay")
        audio.play(firstToday ? "\(lesson.id)_intro" : "\(lesson.id)_next\(Int.random(in: 0...4))") { playSample() }
    }

    private func playSample() {
        listened = false
        audio.play("\(lesson.id)_sample\(Int.random(in: 0...1))") {
            listened = true
            withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) { sparkle = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { sparkle = false }
        }
    }

    private func completeAndNext() {
        model.complete(lesson)
        model.moveNext()
        let next = model.currentLesson
        audio.play("\(next.id)_bridge")
        DispatchQueue.main.asyncAfter(deadline: .now() + model.autoNextSeconds) { dismiss() }
    }

    private var sparkleOverlay: some View {
        ZStack { ForEach(0..<18, id: \.self) { i in Text(i.isMultiple(of: 2) ? "✨" : "❄️").font(.system(size: 28)).offset(x: CGFloat.random(in: -150...150), y: CGFloat.random(in: -280...280)) } }
    }

    private static var today: String { ISO8601DateFormatter().string(from: Date()).prefix(10).description }
}

