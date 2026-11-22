# Vương quốc Tuyết v2.9.2

- Sửa đúng nguyên nhân khiến app đóng ngay khi mở “Học chữ”: ví dụ “cái khăn” của chữ Ă không có bitmap riêng nhưng giao diện cũ vẫn cố đọc `card-1.webp`.
- Bảng chữ giờ tự động dùng hình bitmap sắc nét khi có và dùng emoji an toàn khi chưa có ảnh riêng.
- Đã chạy kiểm thử thật trên Android 15: mở “Học chữ”, hiển thị đủ bảng chữ và mở bài chữ A thành công, không có `FATAL EXCEPTION`.

- Sửa lỗi ứng dụng bị đóng đột ngột khi chạm vào một chữ để bắt đầu bài học trên một số thiết bị Android.
- Bộ phát âm thanh giờ tự động dùng tốc độ gốc nếu thiết bị không hỗ trợ thay đổi tốc độ MP3.
- Bảo vệ quá trình bắt đầu và giải phóng âm thanh để lỗi codec của thiết bị không làm đóng ứng dụng.
- Giữ nguyên toàn bộ câu chuyển bài và cách phát âm chữ cái đã sửa ở bản 2.9.0.

- Sửa toàn bộ câu chuyển bài để chữ cái được phát âm bằng clip chuẩn riêng, không còn để TTS tự đoán ký tự Ă, Â, Đ và các chữ khác.
- Đổi ví dụ chính của chữ Ă thành “cái khăn” và đồng bộ hình, từ, câu mẫu.
- Cải thiện trình tự học và ôn tập 29 chữ cái tiếng Việt.
- Thoại dẫn tự nhiên, đa dạng theo phiên học với giọng nữ miền Nam.
- Đọc mẫu “Âm A” ba lần và giới thiệu từ minh họa rõ ràng.
- Khóa mini-game cho đến khi bé hoàn thành phần nghe mẫu.
- Thêm hiệu ứng phép thuật, gợi ý thông minh và chuỗi trả lời đúng.
- Cải thiện giao diện responsive, góc phụ huynh và các module trò chơi.

SHA-256 APK: `8bdd7c73f9cd9f1c904a742a94708573e54e93a7ce5569ff69538e20650c6e8d`
