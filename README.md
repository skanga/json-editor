# JSON Editor

A small Java Swing editor for object-shaped JSON files.

## Features

- Open a single `.json` file directly.
- Create a new JSON file from the File menu.
- Browse nested object keys in a tree.
- Add, rename, duplicate, delete, and find keys using JSON paths.
- Search by JSON path, visible key name, or value text.
- Edit values as JSON literals, including strings, numbers, booleans, null, objects, and arrays.
- Save as nested JSON or flatten keys on save through preferences.

## Requirements

Java 21 or newer.

## Usage

Run without arguments to open the last file from history:

```sh
java -jar target/json-editor-0.1.jar
```

Start with a JSON file:

```sh
java -jar target/json-editor-0.1.jar path/to/file.json
```

You can also open files with `File > Open JSON File...` or by dragging a JSON file onto the window.

Keys are represented as JSON paths. For example, `/a/b` means the `b` property inside object `a`, while `/a.b` is a single property named `a.b`. Values are edited as JSON literals. For example, use `"hello"` for a string, `true` for a boolean, `42` for a number, `null` for null, and `["a","b"]` for an array.

The editor is tuned for everyday JSON files up to about 1 MB. It avoids restoring large expanded tree states, caps editor field height, warns before opening files over 10 MB, and uses an indexed save path for large node counts.

## Build

```sh
mvn package
```

## License

MIT
