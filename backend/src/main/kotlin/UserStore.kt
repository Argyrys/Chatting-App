import java.util.UUID

object UserStore {
    fun register(loginId: String, password: String, displayName: String): AuthResponse {
        val conn = DatabaseFactory.getConnection()
        try {
            val check = conn.prepareStatement("SELECT id FROM users WHERE id = ?").apply {
                setString(1, loginId)
            }
            val rs = check.executeQuery()
            if (rs.next()) {
                rs.close()
                return AuthResponse(false, "Login ID already exists")
            }
            rs.close()

            val insertUser = conn.prepareStatement(
                "INSERT INTO users (id, display_name, password, created_at, last_seen) VALUES (?, ?, ?, ?, ?)"
            ).apply {
                setString(1, loginId)
                setString(2, displayName.ifEmpty { loginId })
                setString(3, password)
                setLong(4, System.currentTimeMillis() / 1000)
                setLong(5, System.currentTimeMillis() / 1000)
            }
            insertUser.executeUpdate()
            insertUser.close()

            val token = UUID.randomUUID().toString()
            val insertSession = conn.prepareStatement(
                "INSERT INTO sessions (token, user_id, created_at) VALUES (?, ?, ?)"
            ).apply {
                setString(1, token)
                setString(2, loginId)
                setLong(3, System.currentTimeMillis() / 1000)
            }
            insertSession.executeUpdate()
            insertSession.close()

            println("User registered: $loginId")
            return AuthResponse(true, "Registration successful", token, displayName.ifEmpty { loginId })
        } finally {
            conn.close()
        }
    }

    fun login(loginId: String, password: String): AuthResponse {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT id, display_name, password FROM users WHERE id = ?").apply {
                setString(1, loginId)
            }
            val rs = stmt.executeQuery()

            if (!rs.next()) {
                rs.close()
                stmt.close()
                return AuthResponse(false, "User not found")
            }

            val storedPassword = rs.getString("password")
            val displayName = rs.getString("display_name")
            rs.close()
            stmt.close()

            if (storedPassword != password) {
                return AuthResponse(false, "Invalid password")
            }

            val token = UUID.randomUUID().toString()
            val insertSession = conn.prepareStatement(
                "INSERT INTO sessions (token, user_id, created_at) VALUES (?, ?, ?)"
            ).apply {
                setString(1, token)
                setString(2, loginId)
                setLong(3, System.currentTimeMillis() / 1000)
            }
            insertSession.executeUpdate()
            insertSession.close()

            val updateLastSeen = conn.prepareStatement(
                "UPDATE users SET last_seen = ? WHERE id = ?"
            ).apply {
                setLong(1, System.currentTimeMillis() / 1000)
                setString(2, loginId)
            }
            updateLastSeen.executeUpdate()
            updateLastSeen.close()

            println("User logged in: $loginId")
            return AuthResponse(true, "Login successful", token, displayName)
        } finally {
            conn.close()
        }
    }

    fun validateToken(token: String): String? {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT user_id FROM sessions WHERE token = ?").apply {
                setString(1, token)
            }
            val rs = stmt.executeQuery()
            val userId = if (rs.next()) rs.getString("user_id") else null
            rs.close()
            stmt.close()
            return userId
        } finally {
            conn.close()
        }
    }

    fun logout(token: String) {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("DELETE FROM sessions WHERE token = ?").apply {
                setString(1, token)
            }
            stmt.executeUpdate()
            stmt.close()
        } finally {
            conn.close()
        }
    }

    fun getDisplayName(loginId: String): String {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT display_name FROM users WHERE id = ?").apply {
                setString(1, loginId)
            }
            val rs = stmt.executeQuery()
            val name = if (rs.next()) rs.getString("display_name") else loginId
            rs.close()
            stmt.close()
            return name
        } finally {
            conn.close()
        }
    }

    fun updateLastSeen(loginId: String) {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("UPDATE users SET last_seen = ? WHERE id = ?").apply {
                setLong(1, System.currentTimeMillis() / 1000)
                setString(2, loginId)
            }
            stmt.executeUpdate()
            stmt.close()
        } finally {
            conn.close()
        }
    }

    fun getOnlineUsers(): List<String> {
        val conn = DatabaseFactory.getConnection()
        try {
            val cutoff = (System.currentTimeMillis() / 1000) - 300
            val stmt = conn.prepareStatement(
                "SELECT display_name FROM users WHERE last_seen >= ?"
            ).apply {
                setLong(1, cutoff)
            }
            val rs = stmt.executeQuery()
            val users = mutableListOf<String>()
            while (rs.next()) {
                users.add(rs.getString("display_name"))
            }
            rs.close()
            stmt.close()
            return users
        } finally {
            conn.close()
        }
    }
}
