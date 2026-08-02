# CyberPass Core

An offline-first Android password manager built with Kotlin and Jetpack Compose. 

CyberPass stores credentials in a local encrypted vault. The app operates completely isolated from the network—no cloud syncing, no remote backup, and zero telemetry.

## Key Security Properties

* **Zero Network Access:** `android.permission.INTERNET` is explicitly omitted from the manifest.
* **Encryption:** Vault payload encrypted using AES-256-GCM.
* **Key Derivation:** PBKDF2 with 600,000 iterations + local salt.
* **Memory Safety:** Derived keys reside only in memory while unlocked and are zeroed out upon vault lock or process suspension.
* **Verifiers:** The master password is never stored on disk; authentication relies solely on a PBKDF2 verifier digest.
* **Biometrics:** Uses Android Keystore for hardware-bound cryptographic unlock.
* **Hardening:** `FLAG_SECURE` enabled against screenshots/task previews; clipboard automatically cleared after 45 seconds (flagged sensitive on Android 13+).

## Features

* System-wide Android Autofill service.
* Encrypted local JSON export/import for manual backups.
* Peer-to-peer credential transfer via camera/QR code.

## Maintainer

Developed and maintained by **Cyber Castle**.

* Website: [cybercastle.dev](https://cybercastle.dev)
* Source / Issues: [GitHub Repository](https://github.com/jorgeh-nemesis/CyberPass_Core_App)

## License

Mozilla Public License 2.0 (MPL-2.0), see `LICENSE` for details.

## Building

git clone https://github.com/jorgeh-nemesis/CyberPass_Core_App.git
cd CyberPass_Core_App
./gradlew assembleDebug
