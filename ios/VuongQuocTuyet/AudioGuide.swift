import AVFoundation

final class AudioGuide: NSObject, ObservableObject, AVAudioPlayerDelegate {
    private var player: AVAudioPlayer?
    private var completion: (() -> Void)?

    override init() {
        super.init()
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    func play(_ name: String, completion: (() -> Void)? = nil) {
        stop()
        guard let url = Bundle.main.url(forResource: name, withExtension: "mp3", subdirectory: "assets/voice") else {
            completion?(); return
        }
        player = try? AVAudioPlayer(contentsOf: url)
        self.completion = completion
        player?.delegate = self
        player?.prepareToPlay()
        player?.play()
    }

    func stop() { player?.stop(); player = nil; completion = nil }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        let callback = completion
        completion = nil
        callback?()
    }
}

