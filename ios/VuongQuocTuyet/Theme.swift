import SwiftUI

enum SnowTheme {
    static let ink = Color(red: 0.12, green: 0.16, blue: 0.29)
    static let teal = Color(red: 0.12, green: 0.55, blue: 0.52)
    static let sky = Color(red: 0.88, green: 0.97, blue: 1.0)
    static let cream = Color(red: 1.0, green: 0.98, blue: 0.91)
    static let violet = Color(red: 0.48, green: 0.32, blue: 0.75)
    static let mint = Color(red: 0.72, green: 0.95, blue: 0.88)
    static let amber = Color(red: 1.0, green: 0.72, blue: 0.24)

    static var background: LinearGradient {
        LinearGradient(colors: [sky, Color.white, cream], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

struct KidButtonStyle: ButtonStyle {
    var color: Color = SnowTheme.mint
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 19, weight: .bold, design: .rounded))
            .foregroundStyle(SnowTheme.ink)
            .frame(minHeight: 60)
            .padding(.horizontal, 20)
            .background(color.gradient, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: color.opacity(0.35), radius: configuration.isPressed ? 2 : 8, y: configuration.isPressed ? 2 : 6)
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
            .animation(.spring(response: 0.22, dampingFraction: 0.65), value: configuration.isPressed)
    }
}

struct SnowCard<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        content
            .padding(18)
            .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(.white, lineWidth: 2))
            .shadow(color: SnowTheme.violet.opacity(0.13), radius: 14, y: 8)
    }
}

