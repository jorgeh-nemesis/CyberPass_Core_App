package com.cybercastle.cyberpass

// password is intentionally String, not CharArray. CharArray would let us
// explicitly wipe() plaintext after use, but String is the pragmatic choice
// here: PasswordEntry is serialized directly via Gson (would need a custom
// TypeAdapter<CharArray> kept in sync across PasswordRepository and
// MainViewModel.changeMasterPassword's separate Gson instances), Compose's
// OutlinedTextField only accepts String so EntryDialog/EntryCard would hold a
// String copy regardless, and a shared wipe() is unsafe unless every consumer
// (ClipboardGuard, autofill) is guaranteed to receive its own CharArray copy
// rather than the live array backing the entry in MainViewModel._entries -
// get that wrong and e.g. a clipboard copy zeroes the vault's in-memory
// password. See ClipboardGuard.copySensitive for the one real long-lived-
// plaintext exposure this audit found; everything else (autofill field
// parsing/save, load/save paths) only holds password strings in
// function/coroutine-local scope.
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
