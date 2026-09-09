# Chat App - Android Frontend

Kotlin Android chat client with WebSocket support.

## Features
- Connect to WebSocket server at `ws://10.0.2.2:8080/chat?username=user`
- Send/receive JSON messages: `{ "type": "CHAT", "from": "user", "content": "text", "timestamp": 123 }`
- Chat bubble UI with sent (blue) and received (gray) message styles
- Auto-reconnect on disconnect
- Connection status indicator
- Relative timestamps (now, 5s, 2m, 1h)

## Project Structure
```
app/src/main/kotlin/com/chat/app/
├── MainActivity.kt       # UI setup and event handling
├── ChatViewModel.kt      # LiveData for messages and connection
├── WebSocketClient.kt    # OkHttp WebSocket with auto-reconnect
├── ChatAdapter.kt        # RecyclerView adapter for messages
└── Message.kt            # Data class
```

## Setup
1. Open `frontend/app` in Android Studio
2. Sync Gradle
3. Start backend server on port 8080
4. Run on emulator (localhost = 10.0.2.2)

## Message Types
- `CHAT` - User message
- `JOIN` - Server broadcast when user joins
- `LEAVE` - Server broadcast when user leaves

## Dependencies
- OkHttp 4.12.0 (WebSocket)
- AndroidX Lifecycle (ViewModel, LiveData)
- AndroidX RecyclerView
