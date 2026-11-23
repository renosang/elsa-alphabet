# Vương quốc Tuyết – Elsa Alphabet

Ứng dụng Android giáo dục tương tác dành cho trẻ nhỏ học bảng chữ cái tiếng Việt, từ vựng và các mini-game trực quan.

APK Android mới nhất: `vuong-quoc-tuyet-v2.9.3.apk`.

## Tính năng chính

- Học 29 chữ cái tiếng Việt theo đúng thứ tự.
- Giọng đọc nữ miền Nam và câu dẫn thay đổi theo tiến độ học.
- Hình minh họa, hoạt ảnh và nhân vật Công chúa Tuyết.
- Mini-game chọn từ, nối từ, đếm hình và sáng tạo tô màu.
- Góc phụ huynh, nhiều hồ sơ trẻ và theo dõi tiến độ.
- Giao diện responsive cho màn hình dọc và ngang.

## Build APK

Yêu cầu JDK, Android platform JAR, Android Build Tools và ECJ. Thiết lập các biến môi trường rồi chạy:

```bash
ANDROID_PLATFORM_JAR=/path/to/android.jar \
ANDROID_BUILD_TOOLS=/path/to/build-tools \
ANDROID_ECJ_JAR=/path/to/ecj.jar \
./build.sh build/vuong-quoc-tuyet.apk
```

APK cài đặt sẵn có trong mục [Releases](../../releases).

## iOS

Mã nguồn SwiftUI nằm trong thư mục `ios/`. GitHub Actions tạo file
`vuong-quoc-tuyet-ios-v2.9.3-esign-A4-unsigned.ipa` chưa ký, đã đặt sẵn Bundle ID
`A4.alexd.ios.beta` để chủ sở hữu ký bằng provisioning profile ESign `44` và
certificate tương ứng.

IPA chưa ký không thể cài trực tiếp lên iPhone. Bản ESign A4 này phải được ký với
profile `44`, Team ID `K2KTQ4M95W` và thiết bị phải có trong danh sách của profile.
