import SwiftUI

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack { AlphabetHomeView() }
                .tabItem { Label("Học chữ", systemImage: "character.book.closed.fill") }
            NavigationStack { GamesHubView() }
                .tabItem { Label("Trò chơi", systemImage: "puzzlepiece.extension.fill") }
            NavigationStack { ColoringStudioView() }
                .tabItem { Label("Sáng tạo", systemImage: "paintpalette.fill") }
            NavigationStack { ParentGateView() }
                .tabItem { Label("Phụ huynh", systemImage: "person.2.fill") }
        }
        .tint(SnowTheme.violet)
    }
}

