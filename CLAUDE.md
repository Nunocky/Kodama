# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

Kodama (木霊) is an Android application for recording audio from microphone input until speech stops
and playing it back. The name means "echo" in Japanese.

## 基本

- 日本語で応答すること
- 必要に応じて、ユーザに質問を行い、要求を明確にすること
- 作業後、作業内容とユーザが次に取れる行動を説明すること
- 作業項目が多い場合は、段階に区切り、git commit を行いながら進めること
    - semantic commit を使用する
- コマンドの出力が確認できない場合、 get last command / check background terminal を使用して確認すること

## develop

t-wadaの推奨するTDD（Test Driven Development）に従って開発を行う。

作業を以下のように定義する

- 「調査」と指示された場合、都度 docs/nuno/reports に記載すること
- 「計画」と指示した場合、docs/nuno/tasks.md に計画を記載する。 tasks.md は UTF-8で記載すること。
    - 前回の内容が残っている場合は、読まずに消して構わない
    - コードベース、および docs を読み込み、要件に関連性のあるファイルパスをすべて記載すること
    - 不明な点については、fetch mcp を使用して検索すること
    - 必要最小限の要件のみを記載すること
    - このフェーズで、コードを書いては絶対にいけない
- ユーザが「実装」と指示した場合、docs/nuno/tasks.md に記載された内容に基づいて実装を行う
    - 記載されている以上の実装を絶対に行わない
    - ここでデバッグしない
- 「デバッグ」と指示された場合、直前のタスクのデバッグ「手順」のみを示す

## Build System & Commands

This is a standard Android project using Gradle with Kotlin DSL:

- `./gradlew build` - Build the entire project
- `./gradlew :app:assembleDebug` - Build debug APK
- `./gradlew :app:assembleRelease` - Build release APK
- `./gradlew :app:installDebug` - Install debug APK on connected device
- `./gradlew test` - Run unit tests
- `./gradlew :app:connectedAndroidTest` - Run instrumented tests on device
- `./gradlew clean` - Clean build artifacts

## Architecture & Technologies

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material3
- **Dependency Injection**: Dagger Hilt
- **Navigation**: Compose Navigation
- **Serialization**: Kotlinx Serialization
- **Build Tool**: Gradle 8.12.0 with KSP for annotation processing

Key architectural patterns:

- Single Activity architecture (MainActivity only)
- Hilt for DI with @HiltAndroidApp application class
- Compose-based UI with Material3 theming
- Edge-to-edge display support

## Project Structure

- `app/src/main/java/org/nunocky/kodama/` - Main source code
    - `MainActivity.kt` - Single activity entry point with Hilt
    - `MainScreen.kt` - Main Compose UI screen
    - `MyApplication.kt` - Hilt application class
    - `ui/theme/` - Material3 theme configuration
- `app/src/test/` - Unit tests
- `app/src/androidTest/` - Instrumented tests

## Development Notes

- Minimum SDK: 28 (Android 9.0)
- Target SDK: 36
- Java compatibility: Version 11
- Uses version catalogs (gradle/libs.versions.toml) for dependency management
- ProGuard disabled for release builds currently