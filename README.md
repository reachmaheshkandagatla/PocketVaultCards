# PocketVault Cards

PocketVault Cards is an offline Android app for storing private card images, coupon cards, grocery lists, bank card details, and scanned bills on your device.

## Features

- Biometric or device credential unlock before opening the vault.
- Folder-based organization for cards, coupons, groceries, bank cards, and bills.
- Long-press a folder to open folder actions, including `Rename` and `Delete`.
- Rename saved card files and scanned bills from their list/detail screens.
- Scan or upload front and rear card images.
- Pin frequently used cards and track recently opened cards.
- Grocery lists with quantities, checkboxes, and delete actions.
- Grocery-only reminder switch that can notify you daily at about 8:00 PM when a grocery folder still has unchecked items.
- Scanned bill storage with share, rename, view, and delete actions.
- Masked bank card storage with PIN reveal behind biometric authentication.
- Local sharing through Android share sheets.
- No internet permission; data stays on the device.

## Grocery Reminders

Open a groceries folder and use the `Grocery reminders` switch to turn reminders on for that folder.

When enabled:

- Android 13+ asks for notification permission the first time.
- The app schedules a local reminder for that groceries folder.
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

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- AndroidX Biometric
- ML Kit Document Scanner
- Coil

## Privacy

PocketVault Cards is designed to work offline. It does not request internet access, and saved data remains local to the device.
