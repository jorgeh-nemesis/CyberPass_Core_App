# CyberPass Core

**CyberPass** is a fully offline, open‑source password manager designed for privacy and security.  
It stores your credentials in an encrypted vault that never touches the network—only you hold the key.

> **Developed with ❤️ by [Cyber Castle](https://cybercastle.dev)** — we build secure, privacy-first digital tools.

## ✨ Features
- **Offline‑first** – No internet permission, no cloud sync, no telemetry.
- **Strong encryption** – AES‑GCM with PBKDF2 (600k iterations) and Android Keystore for biometric unlock.
- **Autofill support** – Seamlessly fills usernames/passwords in apps and browsers (Android Autofill).
- **Backup & Restore** – Export/import encrypted vaults (self‑contained files).
- **QR sharing** – Securely share passwords via QR codes.
- **Biometric unlock** – Fingerprint with cryptographic binding.
- **Lightweight & modern** – Built with Kotlin and Jetpack Compose.

## 🔒 Security Model
- Master password is **never stored**; only a salted PBKDF2 verifier is kept.
- All sensitive data are encrypted with a key derived from your master password.
- The key is **never persisted**; it lives only in memory while the app is unlocked.
- Biometric authentication uses Android’s hardware‑backed Keystore—the key is never exposed to the app.
- Clipboard is automatically cleared after 45 seconds and marked as sensitive on Android 13+.

## 📄 License
This project is licensed under the Mozilla Public License 2.0 – see the LICENSE file for details.


## 📥 Download
[![F‑Droid](https://img.shields.io/badge/F‑Droid-Get%20it%20on%20F‑Droid-blue)](https://f-droid.org/packages/com.cybercastle.cyberpass/) *(Coming soon)*

You can also grab the latest APK from the [Releases](../../releases) page.

## 🛠️ Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/jorgeh-nemesis/CyberPass_Core_App.git
