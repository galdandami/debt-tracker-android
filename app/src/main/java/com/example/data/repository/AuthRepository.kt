package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

class AuthRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("debt_tracker_auth_prefs", Context.MODE_PRIVATE)

    private val _userSession = MutableStateFlow(loadSessionFromPrefs())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    init {
        // Ensure default registered accounts exist if empty
        val registeredAccounts = prefs.getString("registered_accounts_json", null)
        if (registeredAccounts == null) {
            val defaultUsers = JSONObject().apply {
                put("demo@example.com", JSONObject().apply {
                    put("userId", "usr_demo_1001")
                    put("email", "demo@example.com")
                    put("password", "password123")
                    put("displayName", "Демо Пользователь")
                })
            }
            prefs.edit().putString("registered_accounts_json", defaultUsers.toString()).commit()
        }
    }

    fun getLastEmail(): String {
        return prefs.getString("last_email", "") ?: prefs.getString("email", "") ?: ""
    }

    private fun loadSessionFromPrefs(): UserSession {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return UserSession()

        val userId = prefs.getString("user_id", "") ?: ""
        val email = prefs.getString("email", "") ?: ""
        val displayName = prefs.getString("display_name", "") ?: ""
        val authToken = prefs.getString("auth_token", "") ?: ""
        val lastSync = if (prefs.contains("last_sync_time")) prefs.getLong("last_sync_time", 0L) else null
        val autoSync = prefs.getBoolean("auto_sync_enabled", true)

        return UserSession(
            userId = userId,
            email = email,
            displayName = displayName,
            authToken = authToken,
            isLoggedIn = true,
            lastSyncTimeMillis = lastSync,
            autoSyncEnabled = autoSync
        )
    }

    suspend fun login(email: String, password: String): Result<UserSession> {
        val cleanEmail = email.trim().lowercase()
        val accountsJsonStr = prefs.getString("registered_accounts_json", "{}") ?: "{}"
        val accountsJson = JSONObject(accountsJsonStr)

        if (!accountsJson.has(cleanEmail)) {
            return Result.failure(Exception("Пользователь с таким E-mail не найден"))
        }

        val userObj = accountsJson.getJSONObject(cleanEmail)
        val storedPassword = userObj.getString("password")

        if (storedPassword != password) {
            return Result.failure(Exception("Неверный пароль"))
        }

        val userId = userObj.getString("userId")
        val displayName = userObj.getString("displayName")
        val token = "token_" + UUID.randomUUID().toString().take(8)

        val session = UserSession(
            userId = userId,
            email = cleanEmail,
            displayName = displayName,
            authToken = token,
            isLoggedIn = true,
            lastSyncTimeMillis = prefs.getLong("last_sync_time_$userId", System.currentTimeMillis()),
            autoSyncEnabled = true
        )

        saveSession(session)
        return Result.success(session)
    }

    suspend fun register(displayName: String, email: String, password: String): Result<UserSession> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(Exception("Введите корректный E-mail"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Пароль должен содержать минимум 6 символов"))
        }

        val accountsJsonStr = prefs.getString("registered_accounts_json", "{}") ?: "{}"
        val accountsJson = JSONObject(accountsJsonStr)

        if (accountsJson.has(cleanEmail)) {
            return Result.failure(Exception("Аккаунт с таким E-mail уже зарегистрирован"))
        }

        val newUserId = "usr_" + UUID.randomUUID().toString().take(8)
        val userObj = JSONObject().apply {
            put("userId", newUserId)
            put("email", cleanEmail)
            put("password", password)
            put("displayName", displayName.ifBlank { "Пользователь" })
        }

        accountsJson.put(cleanEmail, userObj)
        prefs.edit().putString("registered_accounts_json", accountsJson.toString()).commit()

        val token = "token_" + UUID.randomUUID().toString().take(8)
        val session = UserSession(
            userId = newUserId,
            email = cleanEmail,
            displayName = displayName.ifBlank { "Пользователь" },
            authToken = token,
            isLoggedIn = true,
            lastSyncTimeMillis = null,
            autoSyncEnabled = true
        )

        saveSession(session)
        return Result.success(session)
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_id")
            .remove("email")
            .remove("display_name")
            .remove("auth_token")
            .commit()

        _userSession.value = UserSession()
    }

    fun updateLastSyncTime(timestamp: Long) {
        val current = _userSession.value
        if (current.isLoggedIn) {
            prefs.edit()
                .putLong("last_sync_time", timestamp)
                .putLong("last_sync_time_${current.userId}", timestamp)
                .commit()
            _userSession.value = current.copy(lastSyncTimeMillis = timestamp)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        val current = _userSession.value
        prefs.edit().putBoolean("auto_sync_enabled", enabled).commit()
        _userSession.value = current.copy(autoSyncEnabled = enabled)
    }

    private fun saveSession(session: UserSession) {
        val editor = prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putString("display_name", session.displayName)
            .putString("auth_token", session.authToken)
            .putBoolean("auto_sync_enabled", session.autoSyncEnabled)
            .putString("last_email", session.email)

        if (session.lastSyncTimeMillis != null) {
            editor.putLong("last_sync_time", session.lastSyncTimeMillis)
        }

        editor.commit()

        _userSession.value = session
    }
}
