package com.example.cyberpass

data class PasswordEntry(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val category: String = "General",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
