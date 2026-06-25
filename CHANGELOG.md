# Changelog

## v0.1.1 - 2026-06-25

### Added

- Value editor find bar with match highlighting, counts, previous/next navigation, and keyboard shortcuts.

### Fixed

- Missing files opened from the recent-file list are removed from history and the menu.

## v0.1.0 - 2026-06-25

Initial public release.

### Added

- Java Swing JSON editor with tree navigation and typed value editing.
- File open, recent file, drag-and-drop, and command-line file loading.
- JSON path search and key filtering.
- Add, rename, duplicate, delete, and reorder actions for JSON nodes.
- Light and dark theme support.
- Maven build that produces a runnable shaded JAR.
- Lazy duplicate JSON key detection after load.
- Duplicate-key warning panel with dismiss action.
- Save confirmation when a loaded file contained duplicate keys.

### Notes

- The editor loads the last value for duplicate JSON keys, matching the underlying parser behavior.
- Saving a file that originally contained duplicate keys rewrites the JSON and removes earlier duplicate values.
- The `data/` directory is excluded from source control.
