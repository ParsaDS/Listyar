# لیست‌یار (Listyar)

A native Android plumbing-supplies list app built with Kotlin and Jetpack Compose.

## Features
- Persian right-to-left interface
- Home, Lists and Settings navigation
- Project lists with address, date, list type and fitting quantities
- Editable pipe and fitting catalog
- Light, dark and system themes
- Local relational SQLite storage for projects, items, pipe types, sizes and fittings
- One-time migration of existing saved lists and catalog from the previous SharedPreferences JSON storage
- JSON backup export and validated restore/import, with confirmation before replacing existing data
- No account, server or internet connection required for normal use

## Open in Android Studio
1. Download or clone this repository.
2. Open the repository folder in Android Studio.
3. Allow Gradle sync to finish.
4. Run the `app` configuration on an emulator or Android phone.

## Backup and restore
Open **Settings → Backup and restore** in the app.
- **Export:** choose where to save `listyar-backup.json`.
- **Restore:** select a Listyar JSON backup. The app validates the file and asks for confirmation before replacing the current lists and pipe catalog.

The first database open migrates existing `saved_lists` and `catalog` values automatically. The old preferences are not deleted by the migration.
