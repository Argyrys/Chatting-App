# Chat App - Android Frontend

Kotlin Android chat client with WebSocket support and a WhatsApp-inspired UI.

## What is included
- Login and registration screens
- WebSocket chat connection
- Sent/received chat bubbles
- File/image/video/audio/document attachment support
- Connection status indicator
- Auto-reconnect after WebSocket failure
- Attractive green WhatsApp-style chat UI
- Rounded message bubbles and modern login card

## Server
The Android emulator connects to the backend through:
- HTTP: `http://10.0.2.2:8080`
- WebSocket: `ws://10.0.2.2:8080/chat`

Start the backend before testing login/chat.

## Open in Android Studio
1. Extract this ZIP.
2. Open the **Chatting-App-main** folder (the folder containing `settings.gradle`).
3. Let Android Studio sync Gradle.
4. Start the backend on port 8080.
5. Run the `app` configuration on an Android emulator.

## Important
The app uses Material Components. The project theme is configured as
`Theme.ChatApp` so the Material login widgets can inflate correctly.
