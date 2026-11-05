package fr.courrierdesnuages.app.data

import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
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
 *
 * profils/{id}                      prénom, classe, avatar, code, liste de la semaine
 * profils/{id}/soirs/{jour}         les mots (et plus tard les tables) envoyés ce soir-là
 * profils/{id}/seances/{jour_matiere}  la séance qui compte, mot par mot
 * profils/{id}/revisions/{clé}      les mots ratés à revoir
 * reglages/parent                   l'empreinte du code parent
 */
class Depot {
    private val db = Firebase.firestore

    private fun profilRef(id: String) = db.collection("profils").document(id)

    // ---------- Profils ----------

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
                    liste = chaines(doc, "liste"),
                    listeLe = doc.getLong("listeLe") ?: 0L,
                )
            }
            rappel(liste.sortedWith(compareBy<Profil>({ it.ordre }, { it.prenom })))
        }

    fun nouvelIdProfil(): String = db.collection("profils").document().id

    fun enregistrerProfil(id: String, champs: Map<String, Any>) {
        profilRef(id).set(champs, SetOptions.merge())
    }

    /** Supprime le profil et ce qui lui appartient (soirs, séances, mots à revoir). */
    suspend fun supprimerProfil(id: String) {
        val ref = profilRef(id)
        ref.delete()
        for (sous in listOf("soirs", "seances", "revisions")) {
            val docs = runCatching { ref.collection(sous).get().await().documents }.getOrDefault(emptyList())
            for (d in docs) d.reference.delete()
        }
    }

    fun enregistrerListe(idProfil: String, mots: List<String>) {
        profilRef(idProfil).set(
            mapOf("liste" to mots, "listeLe" to System.currentTimeMillis()),
            SetOptions.merge(),
        )
    }

    // ---------- Code parent ----------

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

    // ---------- Soirs ----------

    fun ecouterSoirs(
        idProfil: String,
        nombre: Long,
        rappel: (List<Soir>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        profilRef(idProfil).collection("soirs")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(nombre)
            .addSnapshotListener { instantane, e ->
                if (e != null) {
                    erreur(e)
                    return@addSnapshotListener
                }
                rappel(
                    instantane?.documents.orEmpty().mapNotNull { doc ->
                        val date = doc.getString("date") ?: return@mapNotNull null
                        Soir(
                            date = date,
                            mots = chaines(doc, "mots"),
                            tables = chaines(doc, "tables"),
                            envoyeLe = doc.getLong("envoyeLe") ?: 0L,
                        )
                    },
                )
            }

    fun enregistrerSoir(idProfil: String, soir: Soir) {
        profilRef(idProfil).collection("soirs").document(soir.date).set(
            mapOf(
                "date" to soir.date,
                "mots" to soir.mots,
                "tables" to soir.tables,
                "envoyeLe" to soir.envoyeLe,
            ),
            SetOptions.merge(),
        )
    }

    // ---------- Séances ----------

    fun ecouterSeances(
        idProfil: String,
        nombre: Long,
        rappel: (List<Seance>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        profilRef(idProfil).collection("seances")
            .orderBy("soir", Query.Direction.DESCENDING)
            .limit(nombre)
            .addSnapshotListener { instantane, e ->
                if (e != null) {
                    erreur(e)
                    return@addSnapshotListener
                }
                rappel(instantane?.documents.orEmpty().mapNotNull { lireSeance(it) })
            }

    private fun lireSeance(doc: DocumentSnapshot): Seance? {
        val soir = doc.getString("soir") ?: return null
        val resultats = (doc.get("resultats") as? List<*>).orEmpty().mapNotNull { element ->
            val m = element as? Map<*, *> ?: return@mapNotNull null
            val mot = m["mot"] as? String ?: return@mapNotNull null
            ResultatMot(
                mot = mot,
                points = (m["points"] as? Number)?.toDouble() ?: 0.0,
                revision = m["revision"] as? Boolean ?: false,
            )
        }
        return Seance(
            soir = soir,
            matiere = doc.getString("matiere").orEmpty(),
            etat = doc.getString("etat").orEmpty(),
            ordre = chaines(doc, "ordre"),
            revisions = chaines(doc, "revisions"),
            resultats = resultats,
            debutLe = doc.getLong("debutLe") ?: 0L,
            finLe = doc.getLong("finLe") ?: 0L,
        )
    }

    private fun champsSeance(s: Seance): Map<String, Any> = mapOf(
        "soir" to s.soir,
        "matiere" to s.matiere,
        "etat" to s.etat,
        "ordre" to s.ordre,
        "revisions" to s.revisions,
        "resultats" to s.resultats.map { mapOf("mot" to it.mot, "points" to it.points, "revision" to it.revision) },
        "points" to s.points,
        "total" to s.ordre.size,
        "fautes" to s.fautes,
        "medaille" to s.medaille.cle,
        "debutLe" to s.debutLe,
        "finLe" to s.finLe,
    )

    /** Enregistre la séance en cours après chaque mot, pour pouvoir la reprendre. */
    fun enregistrerSeance(idProfil: String, seance: Seance) {
        profilRef(idProfil).collection("seances").document(seance.id).set(champsSeance(seance))
    }

    /** Fin de séance : la séance et les mots à revoir sont enregistrés ensemble. */
    fun terminerSeance(
        idProfil: String,
        seance: Seance,
        revisionsAEcrire: List<Revision>,
        revisionsASupprimer: List<String>,
    ) {
        val lot = db.batch()
        val ref = profilRef(idProfil)
        lot.set(ref.collection("seances").document(seance.id), champsSeance(seance))
        for (r in revisionsAEcrire) {
            lot.set(
                ref.collection("revisions").document(r.id),
                mapOf("mot" to r.mot, "reussitesDeSuite" to r.reussitesDeSuite, "dernierJour" to r.dernierJour),
            )
        }
        for (id in revisionsASupprimer) lot.delete(ref.collection("revisions").document(id))
        lot.commit()
    }

    // ---------- Mots à revoir ----------

    fun ecouterRevisions(
        idProfil: String,
        rappel: (List<Revision>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        profilRef(idProfil).collection("revisions").addSnapshotListener { instantane, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            rappel(
                instantane?.documents.orEmpty().mapNotNull { doc ->
                    val mot = doc.getString("mot") ?: return@mapNotNull null
                    Revision(
                        id = doc.id,
                        mot = mot,
                        reussitesDeSuite = (doc.getLong("reussitesDeSuite") ?: 0L).toInt(),
                        dernierJour = doc.getString("dernierJour").orEmpty(),
                    )
                },
            )
        }

    private fun chaines(doc: DocumentSnapshot, champ: String): List<String> =
        (doc.get(champ) as? List<*>).orEmpty().mapNotNull { it as? String }
}
