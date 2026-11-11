package fr.courrierdesnuages.app.data

import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.italien.CartePostale
import fr.courrierdesnuages.app.italien.MotItalien
import fr.courrierdesnuages.app.italien.SemaineItalien
import fr.courrierdesnuages.app.suivi.Ajout
import fr.courrierdesnuages.app.suivi.RemiseAZero
import fr.courrierdesnuages.app.suivi.parMatiere
import kotlinx.coroutines.tasks.await

const val REVISIONS_FRANCAIS = "revisions"
const val REVISIONS_MATHS = "revisionsMaths"
const val REVISIONS_ITALIEN = "revisionsItalien"

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
 * profils/{id}/revisionsMaths/{clé} les calculs ratés à revoir
 * profils/{id}/revisionsItalien/{clé} les mots d'italien pas sus, qui reviennent le lendemain
 * profils/{id}/cartes/{idCarte}     les cartes postales gagnées
 * italien/{lundi}                   les 5 mots d'italien de la semaine, communs aux enfants
 * cartes/{id}                       les cartes postales créées par le parent
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
                    stats = lireStats(doc.get("annees") as? Map<*, *>, Jours.anneeScolaire()),
                    medailles = lireMedailles(doc.get("annees") as? Map<*, *>),
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
        for (sous in listOf("soirs", "seances", REVISIONS_FRANCAIS, REVISIONS_MATHS, REVISIONS_ITALIEN, "cartes")) {
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
                rappel(instantane?.documents.orEmpty().mapNotNull { lireSoir(it) })
            }

    private fun lireSoir(doc: DocumentSnapshot): Soir? {
        val date = doc.getString("date") ?: return null
        return Soir(
            date = date,
            mots = chaines(doc, "mots"),
            tables = chaines(doc, "tables"),
            envoyeLe = doc.getLong("envoyeLe") ?: 0L,
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
            revisionsAvant = (doc.get("revisionsAvant") as? List<*>)?.mapNotNull { element ->
                val m = element as? Map<*, *> ?: return@mapNotNull null
                val cle = m["cle"] as? String ?: return@mapNotNull null
                val avant = if (m["existait"] == true) {
                    Revision(
                        id = cle,
                        mot = m["mot"] as? String ?: "",
                        reussitesDeSuite = (m["reussitesDeSuite"] as? Number)?.toInt() ?: 0,
                        dernierJour = m["dernierJour"] as? String ?: "",
                    )
                } else {
                    null
                }
                RevisionAvant(cle, avant)
            },
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

    /**
     * Fin de séance : la séance et les mots (ou calculs) à revoir sont enregistrés ensemble.
     * [collection] : [REVISIONS_FRANCAIS] ou [REVISIONS_MATHS].
     */
    fun terminerSeance(
        idProfil: String,
        seance: Seance,
        collection: String,
        revisionsAEcrire: List<Revision>,
        revisionsASupprimer: List<String>,
        revisionsAvant: List<RevisionAvant>,
    ) {
        val lot = db.batch()
        val ref = profilRef(idProfil)
        // La séance garde l'état d'avant des mots à revoir, pour pouvoir être remise à zéro.
        val trace = revisionsAvant.map { r ->
            mapOf(
                "cle" to r.cle,
                "existait" to (r.avant != null),
                "mot" to (r.avant?.mot ?: ""),
                "reussitesDeSuite" to (r.avant?.reussitesDeSuite ?: 0),
                "dernierJour" to (r.avant?.dernierJour ?: ""),
            )
        }
        lot.set(ref.collection("seances").document(seance.id), champsSeance(seance) + ("revisionsAvant" to trace))
        for (r in revisionsAEcrire) {
            lot.set(
                ref.collection(collection).document(r.id),
                mapOf("mot" to r.mot, "reussitesDeSuite" to r.reussitesDeSuite, "dernierJour" to r.dernierJour),
            )
        }
        for (id in revisionsASupprimer) lot.delete(ref.collection(collection).document(id))
        lot.commit()
    }

    /**
     * Remise à zéro d'une séance par le parent, tout ensemble : la séance est effacée, les mots à
     * revoir reprennent leur état d'avant, ce qu'elle avait ajouté aux compteurs de l'année est
     * retiré, et la carte postale qu'elle avait rapportée quitte l'album.
     */
    fun remettreAZero(idProfil: String, an: String, collection: String, plan: RemiseAZero.Plan) {
        val lot = db.batch()
        val ref = profilRef(idProfil)
        lot.delete(ref.collection("seances").document(plan.idSeance))
        for (r in plan.aRestaurer) {
            lot.set(
                ref.collection(collection).document(r.id),
                mapOf("mot" to r.mot, "reussitesDeSuite" to r.reussitesDeSuite, "dernierJour" to r.dernierJour),
            )
        }
        for (id in plan.aSupprimer) lot.delete(ref.collection(collection).document(id))
        val retrait = plan.retrait
        if (retrait != null) {
            val m = retrait.matiere
            val champs = HashMap<String, Any>()
            champs["annees.$an.$m.points"] = FieldValue.increment(-retrait.points)
            champs["annees.$an.$m.possibles"] = FieldValue.increment(-retrait.possibles)
            if (retrait.livraisons > 0) champs["annees.$an.livraisons"] = FieldValue.increment(-retrait.livraisons)
            when (retrait.medaille) {
                Medaille.OR -> champs["annees.$an.$m.or"] = FieldValue.increment(-1L)
                Medaille.ARGENT -> champs["annees.$an.$m.argent"] = FieldValue.increment(-1L)
                Medaille.AUCUNE -> Unit
            }
            lot.update(ref, champs)
        }
        plan.idCarte?.let { lot.delete(ref.collection("cartes").document(it)) }
        lot.commit()
    }

    // ---------- Mots et calculs à revoir ----------

    fun ecouterRevisions(
        idProfil: String,
        collection: String,
        rappel: (List<Revision>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        profilRef(idProfil).collection(collection).addSnapshotListener { instantane, e ->
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

    // ---------- Italien ----------

    fun ecouterSemaineItalien(
        lundi: String,
        rappel: (SemaineItalien?) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        db.collection("italien").document(lundi).addSnapshotListener { doc, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            if (doc == null || !doc.exists()) {
                rappel(null)
                return@addSnapshotListener
            }
            val mots = (doc.get("mots") as? List<*>).orEmpty().map { element ->
                val m = element as? Map<*, *>
                MotItalien(m?.get("fr") as? String ?: "", m?.get("it") as? String ?: "")
            }
            rappel(SemaineItalien(lundi, mots))
        }

    fun enregistrerSemaineItalien(semaine: SemaineItalien) {
        db.collection("italien").document(semaine.lundi).set(
            mapOf(
                "lundi" to semaine.lundi,
                "mots" to semaine.mots.map { mapOf("fr" to Mots.normaliser(it.francais), "it" to Mots.normaliser(it.italien)) },
                "modifieLe" to System.currentTimeMillis(),
            ),
        )
    }

    // ---------- Cartes postales ----------

    fun ecouterCartesGagnees(
        idProfil: String,
        rappel: (List<CarteGagnee>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        profilRef(idProfil).collection("cartes").addSnapshotListener { instantane, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            rappel(
                instantane?.documents.orEmpty().map { doc ->
                    CarteGagnee(
                        idCarte = doc.id,
                        semaine = doc.getString("semaine").orEmpty(),
                        gagneeLe = doc.getLong("gagneeLe") ?: 0L,
                        epinglee = doc.getBoolean("epinglee") ?: false,
                        bienPlacee = doc.getBoolean("bienPlacee") ?: false,
                    )
                }.sortedBy { it.gagneeLe },
            )
        }

    fun enregistrerCarteGagnee(idProfil: String, carte: CarteGagnee) {
        profilRef(idProfil).collection("cartes").document(carte.idCarte).set(
            mapOf(
                "semaine" to carte.semaine,
                "gagneeLe" to carte.gagneeLe,
                "epinglee" to carte.epinglee,
                "bienPlacee" to carte.bienPlacee,
            ),
        )
    }

    fun marquerEpinglee(idProfil: String, idCarte: String, bienPlacee: Boolean) {
        profilRef(idProfil).collection("cartes").document(idCarte).set(
            mapOf("epinglee" to true, "bienPlacee" to bienPlacee),
            SetOptions.merge(),
        )
    }

    /** Les cartes créées par le parent, communes aux trois enfants (avec leur photo). */
    fun ecouterCartesDuParent(
        rappel: (List<CartePostale>) -> Unit,
        erreur: (Exception) -> Unit,
    ): ListenerRegistration =
        db.collection("cartes").addSnapshotListener { instantane, e ->
            if (e != null) {
                erreur(e)
                return@addSnapshotListener
            }
            rappel(
                instantane?.documents.orEmpty().mapNotNull { doc ->
                    val ville = doc.getString("ville") ?: return@mapNotNull null
                    CartePostale(
                        id = "parent-" + doc.id,
                        ville = ville,
                        region = doc.getString("region").orEmpty(),
                        paragraphes = listOfNotNull(doc.getString("texte")?.takeIf { it.isNotBlank() }),
                        motFrancais = doc.getString("motFrancais").orEmpty(),
                        motItalien = doc.getString("motItalien").orEmpty(),
                        legende = ville.uppercase(),
                        image = doc.getString("image"),
                    )
                }.sortedBy { it.ville },
            )
        }

    fun enregistrerCarteDuParent(ville: String, region: String, texte: String, motFrancais: String, motItalien: String, image: String) {
        db.collection("cartes").document().set(
            mapOf(
                "ville" to ville,
                "region" to region,
                "texte" to texte.trim(),
                "motFrancais" to Mots.normaliser(motFrancais),
                "motItalien" to Mots.normaliser(motItalien),
                "image" to image,
                "creeLe" to System.currentTimeMillis(),
            ),
        )
    }

    // ---------- Compteurs de l'année (classement, médailles, tournée) ----------

    /** Toutes les médailles gagnées, d'une année scolaire à l'autre : la collection ne se vide jamais. */
    private fun lireMedailles(annees: Map<*, *>?): Map<String, MedaillesGagnees> {
        val r = HashMap<String, MedaillesGagnees>()
        for (annee in annees?.values.orEmpty()) {
            val m = annee as? Map<*, *> ?: continue
            for (matiere in listOf(Matiere.FRANCAIS, Matiere.MATHS)) {
                val x = m[matiere] as? Map<*, *> ?: continue
                val avant = r[matiere] ?: MedaillesGagnees()
                r[matiere] = MedaillesGagnees(
                    or = avant.or + ((x["or"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
                    argent = avant.argent + ((x["argent"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
                )
            }
        }
        return r
    }

    private fun lireStats(annees: Map<*, *>?, an: String): StatsAnnee {
        val m = annees?.get(an) as? Map<*, *> ?: return StatsAnnee()
        fun matiere(cle: String): StatsMatiere {
            val x = m[cle] as? Map<*, *> ?: return StatsMatiere()
            return StatsMatiere(
                // Jamais en dessous de zéro, même après une remise à zéro mal tombée.
                points = ((x["points"] as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0),
                possibles = ((x["possibles"] as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0),
                or = ((x["or"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
                argent = ((x["argent"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
            )
        }
        return StatsAnnee(
            francais = matiere(Matiere.FRANCAIS),
            maths = matiere(Matiere.MATHS),
            italien = matiere(Matiere.ITALIEN),
            livraisons = ((m["livraisons"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
            calcule = m["calcule"] as? Boolean ?: false,
        )
    }

    /**
     * Ajoute une séance terminée aux compteurs de l'année. Les ajouts sont atomiques : plusieurs
     * téléphones peuvent compter en même temps.
     */
    fun ajouterAuxStats(idProfil: String, an: String, ajouts: List<Ajout>) {
        val champs = HashMap<String, Any>()
        var livraisons = 0L
        for ((matiere, a) in ajouts.parMatiere()) {
            champs["annees.$an.$matiere.points"] = FieldValue.increment(a.points)
            champs["annees.$an.$matiere.possibles"] = FieldValue.increment(a.possibles)
            val or = ajouts.count { it.matiere == matiere && it.medaille == Medaille.OR }
            val argent = ajouts.count { it.matiere == matiere && it.medaille == Medaille.ARGENT }
            if (or > 0) champs["annees.$an.$matiere.or"] = FieldValue.increment(or.toLong())
            if (argent > 0) champs["annees.$an.$matiere.argent"] = FieldValue.increment(argent.toLong())
            livraisons += a.livraisons
        }
        if (livraisons > 0) champs["annees.$an.livraisons"] = FieldValue.increment(livraisons)
        if (champs.isNotEmpty()) profilRef(idProfil).update(champs)
    }

    /** Remplace les compteurs de l'année par ceux recalculés à partir des séances et des soirs. */
    fun ecrireStats(idProfil: String, an: String, stats: StatsAnnee) {
        fun matiere(m: StatsMatiere) = mapOf("points" to m.points, "possibles" to m.possibles, "or" to m.or, "argent" to m.argent)
        profilRef(idProfil).update(
            mapOf(
                "annees.$an" to mapOf(
                    Matiere.FRANCAIS to matiere(stats.francais),
                    Matiere.MATHS to matiere(stats.maths),
                    Matiere.ITALIEN to matiere(stats.italien),
                    "livraisons" to stats.livraisons,
                    "calcule" to true,
                ),
            ),
        )
    }

    /** Les séances depuis [debut]. Avec [serveur], seulement depuis Internet (sinon : échec), pour ne rien oublier. */
    suspend fun seancesDepuis(idProfil: String, debut: String, serveur: Boolean): List<Seance> =
        profilRef(idProfil).collection("seances").whereGreaterThanOrEqualTo("soir", debut)
            .get(if (serveur) Source.SERVER else Source.DEFAULT).await()
            .documents.mapNotNull { lireSeance(it) }

    suspend fun soirsDepuis(idProfil: String, debut: String, serveur: Boolean): List<Soir> =
        profilRef(idProfil).collection("soirs").whereGreaterThanOrEqualTo("date", debut)
            .get(if (serveur) Source.SERVER else Source.DEFAULT).await()
            .documents.mapNotNull { lireSoir(it) }

    private fun chaines(doc: DocumentSnapshot, champ: String): List<String> =
        (doc.get(champ) as? List<*>).orEmpty().mapNotNull { it as? String }
}
