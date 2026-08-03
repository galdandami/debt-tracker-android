package com.example.data.model

data class UserSession(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val authToken: String = "",
    val isLoggedIn: Boolean = false,
    val lastSyncTimeMillis: Long? = null,
    val autoSyncEnabled: Boolean = true
)
