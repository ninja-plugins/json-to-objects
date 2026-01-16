# JSON to Java/Kotlin Object

IntelliJ IDEA plugin that converts JSON to Java/Kotlin classes.

## Features

- Generate **Java classes** or **Kotlin data classes** from JSON
- **Lombok support**: @Data, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
- **Java Record** support (Java 14+)
- **@JsonProperty** annotation support
- Multiple structure options: Inner class, Separate classes, Multiple files
- JSON validation with helpful error messages

## Usage

### 1. Right-click on JSON file
Right-click on any `.json` file → **JSON to Java/Kotlin Object**

### 2. Select text in editor
Select JSON text in any file → Right-click → **JSON to Java/Kotlin Object**

### 3. Tools menu
**Tools** → **JSON to Java/Kotlin Object**

### 4. Shortcut
`Ctrl+Alt+Shift+K`

## Installation

### From JetBrains Marketplace
1. Open **Settings** → **Plugins** → **Marketplace**
2. Search for "JSON to Java/Kotlin Object"
3. Click **Install**

### Manual Installation
1. Download the latest release from [Releases](https://github.com/user/json-to-objects/releases)
2. Open **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk...**
3. Select the downloaded `.zip` file

## Build

```bash
./gradlew build
```

## Run (Development)

```bash
./gradlew runIde
```

## License

MIT License
