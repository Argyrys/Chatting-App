import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {
    private const val DB_URL = "jdbc:sqlite:chat.db"

    fun init() {
        Class.forName("org.sqlite.JDBC")
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        password TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        last_seen INTEGER NOT NULL DEFAULT 0
                    )
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        from_id TEXT NOT NULL,
                        to_id TEXT,
                        content TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'CHAT',
                        message_type TEXT NOT NULL DEFAULT 'TEXT',
                        file_url TEXT DEFAULT '',
                        file_name TEXT DEFAULT '',
                        file_size INTEGER DEFAULT 0,
                        reply_to_id INTEGER,
                        is_deleted INTEGER NOT NULL DEFAULT 0,
                        edited_at INTEGER,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        token TEXT PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS fcm_tokens (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id TEXT NOT NULL,
                        fcm_token TEXT NOT NULL UNIQUE,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        println("Database initialized")
    }

    fun getConnection(): Connection = DriverManager.getConnection(DB_URL)
}
