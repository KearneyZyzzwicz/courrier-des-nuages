package fr.courrierdesnuages.app.data

import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/** Le compte familial unique qui relie chaque téléphone à la base. */
object Compte {
    val estRelie: Boolean get() = Firebase.auth.currentUser != null
    val email: String? get() = Firebase.auth.currentUser?.email

    suspend fun connecter(email: String, motDePasse: String) {
        Firebase.auth.signInWithEmailAndPassword(email.trim(), motDePasse).await()
    }

    fun deconnecter() {
        Firebase.auth.signOut()
    }

    /** Vérifie le mot de passe du compte sans changer la connexion du téléphone. */
    suspend fun verifierMotDePasse(motDePasse: String) {
        val utilisateur = Firebase.auth.currentUser ?: error("Téléphone non relié")
        val email = utilisateur.email ?: error("Compte sans adresse e-mail")
        utilisateur.reauthenticate(EmailAuthProvider.getCredential(email, motDePasse)).await()
    }
}

/**
 * Accès à la base Firestore. Les écritures ne sont pas attendues : la base les garde
 * en mémoire sur le téléphone et les envoie dès que la connexion revient.
 */
class Depot {
    private val db = Firebase.firestore

    fun ecouterProfils(
        rappel: (List<Profil>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        db.collection("profils").addSnapshotListener { instantane, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            val liste = instantane?.documents.orEmpty().mapNotNull { doc ->
                val prenom = doc.getString("prenom") ?: return@mapNotNull null
                Profil(
                    id = doc.id,
                    prenom = prenom,
                    classe = doc.getString("classe").orEmpty(),
                    avatar = Avatar.depuisCle(doc.getString("avatar")),
                    codeEmpreinte = doc.getString("codeEmpreinte").orEmpty(),
                    codeSel = doc.getString("codeSel").orEmpty(),
                    ordre = doc.getLong("ordre") ?: 0L,
                )
            }
            rappel(liste.sortedWith(compareBy<Profil>({ it.ordre }, { it.prenom })))
        }

    /** [rappel] reçoit le code (ou null s'il n'existe pas) et indique si l'info vient du cache. */
    fun ecouterCodeParent(
        rappel: (CodeParent?, Boolean) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        // INCLUDE : on est aussi prévenu quand l'info du cache est confirmée par la base.
        db.collection("reglages").document("parent").addSnapshotListener(MetadataChanges.INCLUDE) { doc, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            val empreinte = doc?.getString("codeEmpreinte")
            val sel = doc?.getString("codeSel")
            val code = if (empreinte != null && sel != null) CodeParent(empreinte, sel) else null
            rappel(code, doc?.metadata?.isFromCache ?: true)
        }

    fun enregistrerCodeParent(code: String) {
        val sel = Codes.nouveauSel()
        db.collection("reglages").document("parent").set(
            mapOf("codeEmpreinte" to Codes.empreinte(code, sel), "codeSel" to sel),
            SetOptions.merge(),
        )
    }

    fun nouvelIdProfil(): String = db.collection("profils").document().id

    fun enregistrerProfil(id: String, champs: Map<String, Any>) {
        db.collection("profils").document(id).set(champs, SetOptions.merge())
    }

    fun supprimerProfil(id: String) {
        db.collection("profils").document(id).delete()
    }
}
