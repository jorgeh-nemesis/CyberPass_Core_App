package com.cybercastle.cyberpass

data class PasswordEntry(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val category: String = VaultCategories.LOGINS,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object VaultCategories {
    const val LOGINS = "Logins"
    const val CARDS = "Cards"
    const val NOTES = "Notes"

    val ALL = listOf(LOGINS, CARDS, NOTES)
}

enum class CategoryFilter {
    ALL, LOGINS, CARDS, NOTES, FAVORITES;

    fun matches(entry: PasswordEntry): Boolean = when (this) {
        ALL -> true
        LOGINS -> entry.category.equals(VaultCategories.LOGINS, ignoreCase = true)
        CARDS -> entry.category.equals(VaultCategories.CARDS, ignoreCase = true)
        NOTES -> entry.category.equals(VaultCategories.NOTES, ignoreCase = true)
        FAVORITES -> entry.isFavorite
    }
}
