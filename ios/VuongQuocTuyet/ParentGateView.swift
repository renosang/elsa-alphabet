import SwiftUI

struct ParentGateView: View {
    @State private var code = (0..<3).map { _ in Int.random(in: 0...9) }
    @State private var entered: [Int] = []
    @State private var unlocked = false

    var body: some View {
        if unlocked { ParentDashboardView() } else {
            ZStack {
                Color(red: 0.97, green: 0.95, blue: 1).ignoresSafeArea()
                GeometryReader { geo in
                    let landscape = geo.size.width > geo.size.height
                    Group {
                        if landscape { HStack(spacing: 40) { prompt; keypad } }
                        else { VStack(spacing: 26) { prompt; keypad } }
                    }.padding(24)
                }
            }
        }
    }

    private var prompt: some View {
        VStack(spacing: 12) {
            Text("Dành cho phụ huynh").font(.system(size: 30, weight: .bold, design: .rounded))
            Text("Nhập các số:").font(.headline)
            Text(code.map(String.init).joined(separator: "  ")).font(.system(size: 44, weight: .black, design: .rounded)).foregroundStyle(.green)
            Text("👩‍👩‍👧‍👦").font(.system(size: 74))
            HStack { ForEach(0..<3, id: \.self) { i in Text(i < entered.count ? "●" : "—").frame(width: 58, height: 58).background(.white, in: RoundedRectangle(cornerRadius: 16)) } }
        }.frame(maxWidth: .infinity)
    }

    private var keypad: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
            ForEach(1...9, id: \.self) { numberButton($0) }
            numberButton(0)
            Button { if !entered.isEmpty { entered.removeLast() } } label: { Image(systemName: "delete.left.fill").frame(maxWidth: .infinity) }.buttonStyle(KidButtonStyle(color: .yellow.opacity(0.65)))
        }.frame(maxWidth: 360)
    }
    private func numberButton(_ n: Int) -> some View {
        Button("\(n)") {
            entered.append(n)
            if entered.count == 3 {
                if entered == code { unlocked = true } else { entered.removeAll(); code = (0..<3).map { _ in Int.random(in: 0...9) } }
            }
        }.buttonStyle(KidButtonStyle(color: .yellow.opacity(0.65)))
    }
}

struct ParentDashboardView: View {
    @EnvironmentObject private var model: AppModel
    @State private var newName = ""
    var body: some View {
        ZStack {
            Color(red: 0.97, green: 0.95, blue: 1).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Góc phụ huynh").font(.system(size: 38, weight: .bold, design: .rounded))
                    SnowCard { VStack(alignment: .leading, spacing: 14) {
                        Text("Hồ sơ của bé").font(.title2.bold())
                        ScrollView(.horizontal) { HStack { ForEach(Array(model.children.enumerated()), id: \.offset) { index, name in Button { model.activeChild = index } label: { VStack { Text("⛄️").font(.system(size: 44)); Text(name).bold() }.padding().background(index == model.activeChild ? SnowTheme.mint : .white, in: RoundedRectangle(cornerRadius: 18)) } } } }
                        HStack { TextField("Tên bé", text: $newName).textFieldStyle(.roundedBorder); Button("Thêm") { if !newName.isEmpty && model.children.count < 8 { model.children.append(newName); newName = "" } } }
                    } }
                    SnowCard { VStack(alignment: .leading, spacing: 12) {
                        Text("Tiến độ hôm nay").font(.title2.bold())
                        ProgressView(value: model.progress).tint(.green)
                        Text("Đã học \(model.completed.count)/29 chữ cái")
                    } }
                    SnowCard { VStack(alignment: .leading, spacing: 12) {
                        Text("Tự động chuyển bài").font(.title2.bold())
                        Slider(value: $model.autoNextSeconds, in: 2...12, step: 1)
                        Text("Chuyển sang chữ tiếp theo sau \(Int(model.autoNextSeconds)) giây")
                    } }
                }.padding()
            }
        }
    }
}

