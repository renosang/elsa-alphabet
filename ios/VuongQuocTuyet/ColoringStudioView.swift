import SwiftUI
import WebKit

struct ColoringStudioView: View {
    var body: some View {
        StudioWebView()
            .ignoresSafeArea(edges: .bottom)
            .navigationTitle("Sáng tạo & Tô màu")
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct StudioWebView: UIViewRepresentable {
    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        let web = WKWebView(frame: .zero, configuration: config)
        web.isOpaque = false
        web.backgroundColor = .clear
        if let file = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "assets/play") {
            web.loadFileURL(file, allowingReadAccessTo: file.deletingLastPathComponent())
        }
        return web
    }
    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

