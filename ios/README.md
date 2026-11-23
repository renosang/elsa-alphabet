# Bản iOS

Ứng dụng iOS dùng SwiftUI, hỗ trợ iPhone/iPad, màn hình dọc và ngang. Target yêu cầu iOS 16 trở lên.

## Tạo project trên macOS

```bash
brew install xcodegen
cd ios
xcodegen generate
open VuongQuocTuyet.xcodeproj
```

## IPA chưa ký

Workflow `Build unsigned iOS IPA` biên dịch với `CODE_SIGNING_ALLOWED=NO`, đóng gói `.app` thành IPA và đính kèm vào GitHub Release `v2.9.3`.

Để cài lên iPhone, hãy ký lại IPA bằng certificate và provisioning profile hợp lệ. Bundle ID mặc định:

```text
A4.alexd.ios.beta
```

Provisioning profile phải khớp Bundle ID này, hoặc công cụ ký cần đổi Bundle ID và cập nhật profile tương ứng. Với Ad Hoc profile, UDID của thiết bị phải nằm trong profile.
