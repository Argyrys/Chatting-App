# Chat App - Android Frontend

Kotlin Android chat client with WebSocket support.

## Features
- Connect to WebSocket server at `ws://10.0.2.2:8080/chat` (emulator localhost)
- Send/receive JSON messages: `{ "type": "JOIN|CHAT|LEAVE", "from": "user", "content": "text", "timestamp": 123 }`
- Chat bubble UI with sent (blue) and received (gray) message styles
- Auto-reconnect on disconnect
- Connection status indicator

## Project Structure
```
frontend/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/chat/app/
│       │   ├── MainActivity.kt
│       │   ├── ChatViewModel.kt
│       │   ├── WebSocketClient.kt
│       │   ├── Message.kt
│       │   └── ChatAdapter.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── item_message_sent.xml
│           │   ├── item_message_received.xml
│           │   └── item_message_system.xml
│           ├── drawable/
│           │   ├── bg_message_sent.xml
│           │   ├── bg_message_received.xml
│           │   ├── bg_message_system.xml
│           │   └── bg_input_field.xml
│           └── values/
│               ├── strings.xml
│               └── colors.xml
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

## Setup
1. Open `frontend/` in Android Studio
2. Sync Gradle
3. Run on emulator (localhost = 10.0.2.2)

## Dependencies
- OkHttp 4.12.0 (WebSocket)
- AndroidX Lifecycle (ViewModel, LiveData)
- AndroidX RecyclerView
