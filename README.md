<p align="center">
  <img src="/images/logo.png" alt="PassQR Logo" width="120"/>
</p>

<h1 align="center">PassQR</h1>
<p align="center">
  <strong>Akses QR Instan ke Web & Aplikasi</strong>
</p>

<p align="center">
  <a href="https://github.com/Curzyori/pass-qr/tree/main/version"><strong>📦 Current Version Build</strong></a>
</p>

<div align="center">

[![Stars](https://img.shields.io/github/stars/Curzyori/pass-qr?style=for-the-badge&color=blue)](https://github.com/Curzyori/pass-qr/stargazers)
[![Forks](https://img.shields.io/github/forks/Curzyori/pass-qr?style=for-the-badge&color=blue)](https://github.com/Curzyori/pass-qr/network/members)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-black?style=for-the-badge)](#)

</div>

<p align="center">
  <a href="#-why-passqr">Why This</a> ·
  <a href="#-key-features">Features</a> ·
  <a href="#-installation">Installation</a> ·
  <a href="#-preview">Preview</a>
</p>

---

## 🕒 Why PassQR?

PassQR is a fast, privacy-friendly QR scanner designed for the modern Android experience. It doesn't just scan codes; it bridges the gap between physical codes and the digital web instantly.

| Feature | Benefit |
| :--- | :--- |
| ⚡ **Instant Scanning** | High-speed detection using optimized scanning logic. |
| 🌐 **Web Integration** | Immediately loads URLs in the app webview. |
| 🚀 **App-to-App** | Detects and handles app-specific deep links. |
| 🔒 **Privacy First** | No data tracking, completely local processing. |

---

## 🎯 Key Features

| Feature | Status | Description |
| :--- | :---: | :--- |
| **Fast QR Detection** | ✅ | Uses `shouzhong/Scanner` logic for instant feedback. |
| **Web Mode** | ✅ | Opens detected URLs in a built-in secure webview. |
| **App Mode** | ✅ | Navigates directly to external apps via detected links. |
| **Dark Mode** | ✅ | System-wide dark and light theme support. |
| **Multi-Language** | ✅ | Supports English and Indonesian. |

---

## 🛠 Tech Stack

- **Platform:** Android
- **Language:** Kotlin & Java
- **UI Framework:** Jetpack Compose with Material Design 3
- **Scanning Engine:** Shouzhong/Scanner + ZXing Core
- **Architecture:** Single-Activity, Compose Navigation

---

## 📦 Installation

Download the latest APK from the [version folder](https://github.com/Curzyori/pass-qr/tree/main/version):

| Version | File |
| :--- | :--- |
| v3.0.0 | `PassQR-V3.0.0.apk` |

### Build from Source
```bash
git clone https://github.com/Curzyori/pass-qr.git
cd pass-qr
./gradlew assembleDebug
```

---

## 🖼️ Preview

**Mobile App Demo**
<p align="center">
  <video src="app.mp4" width="100%" controls></video>
</p>

**Web Interface Demo**
<p align="center">
  <video src="web.mp4" width="100%" controls></video>
</p>

---

## 📄 License

This project is released under the **Apache License 2.0** — see [LICENSE](LICENSE) for full text.

<sub>Built with passion as the 15th Project of the 50 Projects Challenge by <strong>@Curzyori</strong></sub>
