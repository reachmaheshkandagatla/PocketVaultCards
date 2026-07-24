# PocketVault Cards

PocketVault Cards is an offline Android app for storing private card images, coupon cards, grocery lists, bank card details, and scanned bills on your device.

## Features

- SQLCipher-encrypted local database with Android Keystore-protected key material.
- AES-256-GCM encryption for saved card and bill images.
- Biometric or device credential unlock before opening the vault.
- Folder-based organization for cards, coupons, groceries, bank cards, and bills.
- Long-press a folder to open folder actions, including `Rename` and `Delete`.
- Rename saved card files and scanned bills from their list/detail screens.
- Scan or upload front and rear card images.
- Pin frequently used cards and track recently opened cards.
- Grocery lists with quantities, checkboxes, and delete actions.
- Grocery-only reminder switch that can notify you daily when a grocery folder still has unchecked items.
- Scanned bill storage with share, rename, view, and delete actions.
- Masked bank card storage with PIN reveal behind biometric authentication.
- Local sharing through Android share sheets.
- No internet permission; data stays on the device.

## Google Play Readiness

- Targets Android API 35.
- Requests only camera and notification permissions.
- Does not request internet, Bluetooth, contacts, location, SMS, phone, or broad storage permissions.
- Uses Android share sheets for local sharing, so the destination app handles any Bluetooth or nearby transfer permissions.
- Includes a project privacy policy draft in `PRIVACY_POLICY.md`.

## Grocery Reminders

Open a groceries folder and use the `Grocery reminders` switch to turn reminders on for that folder.
Long-press the reminder bar to choose a convenient reminder time. New grocery reminders default to 4:00 PM.

When enabled:

- Android 13+ asks for notification permission the first time.
- The app schedules a local purchase reminder for that groceries folder at the selected time.
- A notification is shown only if the folder still has unchecked grocery items.
- The reminder is cancelled when the switch is turned off or when there are no pending items.

## Build

From the project root:

```bash
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

The generated debug APK uses the application name:

```text
PocketVault-Cards-debug.apk
```

### Signed release bundle

Release builds enable R8 code shrinking and resource shrinking. Set these environment variables before creating a signed bundle:

```bash
export POCKETVAULT_KEYSTORE=/absolute/path/to/upload-key.jks
export POCKETVAULT_KEYSTORE_PASSWORD=your-keystore-password
export POCKETVAULT_KEY_ALIAS=your-key-alias
export POCKETVAULT_KEY_PASSWORD=your-key-password
./gradlew bundleRelease
```

Signing secrets and keystore files must not be committed to the repository.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- AndroidX Biometric
- ML Kit Document Scanner
- Coil

## Privacy

PocketVault Cards is designed to work offline. It does not request internet access, and saved data remains local to the device.
Review `PRIVACY_POLICY.md` before publishing and host the final policy at a public URL for Google Play Console.
