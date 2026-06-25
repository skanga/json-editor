# JSON Editor

JSON Editor is a desktop Java Swing application for inspecting and editing JSON files through a tree-based interface. It is designed for day-to-day JSON maintenance where you want to navigate nested data, edit values safely, and save valid JSON without hand-editing the whole file in a text editor.

The editor loads a JSON document into a navigable tree, shows the selected node as an editable value, and supports common structural operations such as adding, renaming, duplicating, deleting, moving, and searching nodes.

## Features

- Open a `.json` file from the command line, file picker, recent-file history, or drag and drop.
- Automatically remove missing files from the recent-file history when opened.
- Browse nested object and array content in a JSON tree.
- Filter the tree by JSON path, visible key name, or value text.
- Edit values as typed JSON literals or raw text.
- Find text inside the active value editor with match highlighting and keyboard navigation.
- Add, rename, duplicate, delete, and reorder JSON nodes.
- Copy the selected node path.
- Save JSON as nested object/array data.
- Optionally flatten object keys on save through preferences.
- Detect duplicate JSON object keys after load and warn before saving.
- Switch between light and dark editor themes.
- Use localized UI strings from resource bundles.

## Duplicate Key Handling

JSON object keys are expected to be unique, but real files sometimes contain duplicates:

```json
{
  "role": "user",
  "role": "admin"
}
```

The editor uses Gson for normal JSON loading, which keeps the last value for a duplicated key. To avoid silently hiding that behavior, JSON Editor also runs a lazy duplicate-key scan after the file is loaded.

When duplicates are found, the editor shows a warning panel at the top of the detail view:

```text
WARNING: Duplicate JSON keys detected. The editor loaded the last value for each duplicate key. Saving the JSON file will delete all the duplicate values. The first duplicate is: /path/to/key
```

The warning can be dismissed after review. If you try to save a file that originally contained duplicate keys, the editor asks for confirmation because saving rewrites the JSON and removes earlier duplicate values.

## Requirements

- Java 21 or newer
- Maven 3.9 or newer for local builds

## Run

Build the application first:

```sh
mvn package
```

Run without arguments to open the last file from history:

```sh
java -jar target/json-editor-0.1.jar
```

Start with a specific JSON file:

```sh
java -jar target/json-editor-0.1.jar path/to/file.json
```

## Editing Model

Keys are represented as JSON paths. For example:

- `/a/b` means the `b` property inside object `a`.
- `/a.b` is a single property literally named `a.b`.
- `/items/0/name` means the `name` property on the first object in the `items` array.

Values are edited as JSON literals:

```json
"hello"
true
42
null
["a", "b"]
{"enabled": true}
```

Use typed editing for normal values and raw editing when you need direct literal control.

## Performance Notes

JSON Editor is tuned for everyday JSON files up to about 1 MB. It includes practical safeguards for larger files:

- warns before opening files over 10 MB;
- avoids restoring large expanded tree states;
- caps editor field height for long values;
- uses an indexed save path for large node counts;
- scans duplicate keys with a streaming reader so the preflight check does not materialize a second JSON tree.

## Test

Run the full test suite:

```sh
mvn test
```

Run a package build:

```sh
mvn package
```

## Project Structure

```text
src/main/java/com/skanga/jsoneditor/editor   Swing editor UI and tree behavior
src/main/java/com/skanga/jsoneditor/io       JSON loading, saving, and duplicate-key scan
src/main/java/com/skanga/jsoneditor/model    JSON document model
src/main/java/com/skanga/jsoneditor/util     Shared utilities and message access
src/main/resources/bundles                   Localized UI strings
src/test/java/com/skanga/jsoneditor          Unit and UI behavior tests
```

The `data/` directory is intentionally ignored and is not part of the repository.

## Releases

Published releases are available on GitHub:

https://github.com/skanga/json-editor/releases

The current release line is `v0.1.1`, which adds value-editor find and recent-file cleanup.

For local release builds:

```sh
mvn clean package
```

The runnable shaded JAR is written to:

```text
target/json-editor-0.1.jar
```

See [CHANGELOG.md](CHANGELOG.md) for release notes.

## License

MIT. See [LICENSE](LICENSE).
