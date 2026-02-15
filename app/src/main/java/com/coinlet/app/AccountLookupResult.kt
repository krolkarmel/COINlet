import com.google.firebase.firestore.FirebaseFirestore

data class AccountLookupResult(
    val userId: String,
    val accountRefPath: String
)

fun findUserIdByIban(
    db: FirebaseFirestore,
    iban: String,
    onSuccess: (AccountLookupResult) -> Unit,
    onError: (String) -> Unit
) {
    val normalizedIban = iban.trim()

    db.collectionGroup("accounts")
        .whereEqualTo("accountNumber", normalizedIban)
        .limit(1)
        .get()
        .addOnSuccessListener { snap ->
            if (snap.isEmpty) {
                onError("Nie znaleziono konta dla IBAN: $normalizedIban")
                return@addOnSuccessListener
            }

            val doc = snap.documents.first()
            val userId = doc.getString("userId")
            if (userId.isNullOrBlank()) {
                onError("Konto znalezione, ale brak pola userId w dokumencie konta.")
                return@addOnSuccessListener
            }

            onSuccess(
                AccountLookupResult(
                    userId = userId,
                    accountRefPath = doc.reference.path
                )
            )
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Błąd wyszukiwania konta po IBAN")
        }
}
