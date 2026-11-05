package fr.courrierdesnuages.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import fr.courrierdesnuages.app.BuildConfig
import fr.courrierdesnuages.app.calcul.Calcul
import fr.courrierdesnuages.app.calcul.SeanceCalcul
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.data.CodeParent
import fr.courrierdesnuages.app.data.Codes
import fr.courrierdesnuages.app.data.Compte
import fr.courrierdesnuages.app.data.Depot
import fr.courrierdesnuages.app.data.DonneesEnfant
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.REVISIONS_FRANCAIS
import fr.courrierdesnuages.app.data.REVISIONS_ITALIEN
import fr.courrierdesnuages.app.data.REVISIONS_MATHS
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Revision
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.data.Soir
import fr.courrierdesnuages.app.dictee.SeanceDictee
import fr.courrierdesnuages.app.italien.CARTES_INTEGREES
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.italien.CartePostale
import fr.courrierdesnuages.app.italien.MotItalien
import fr.courrierdesnuages.app.italien.RecompenseItalien
import fr.courrierdesnuages.app.italien.SeanceItalien
import fr.courrierdesnuages.app.italien.SemaineItalien
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.italien.Ville
import fr.courrierdesnuages.app.maj.MiseAJour
import fr.courrierdesnuages.app.maj.NouvelleVersion
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.son.Voix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Les écrans de l'appli. Les profils sont désignés par leur identifiant. */
sealed interface Ecran {
    data object Relier : Ecran
    data object ChoixProfil : Ecran
    data class CodeEnfant(val idProfil: String) : Ecran
    data object CodeDuParent : Ecran
    data class NouveauCodeParent(val premiereFois: Boolean) : Ecran
    data object EspaceParent : Ecran
    data class FicheEnfant(val idProfil: String?) : Ecran
    data class AccueilEnfant(val idProfil: String) : Ecran
    data class CeSoir(val idProfil: String) : Ecran
    data class LectureListe(val idProfil: String, val image: String?) : Ecran
    data class Dictee(val idProfil: String) : Ecran
    data class TriDesColis(val idProfil: String) : Ecran
    data class Pigeon(val idProfil: String) : Ecran
    data class CartePostaleVue(val idProfil: String, val idCarte: String, val nouvelle: Boolean) : Ecran
    data class Epingler(val idProfil: String, val idCarte: String) : Ecran
    data class Album(val idProfil: String) : Ecran
    data object MotsItalien : Ecran
    data object NouvelleCarte : Ecran
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val depot = Depot()

    var ecran by mutableStateOf<Ecran>(if (Compte.estRelie) Ecran.ChoixProfil else Ecran.Relier)
        private set

    // Données de la famille
    var profils by mutableStateOf<List<Profil>>(emptyList())
        private set
    var profilsCharges by mutableStateOf(false)
        private set
    var codeParent by mutableStateOf<CodeParent?>(null)
        private set
    var codeParentCharge by mutableStateOf(false)
        private set

    /** Vrai quand la base (et pas seulement le cache du téléphone) confirme qu'il n'y a pas de code parent. */
    var absenceCodeConfirmee by mutableStateOf(false)
        private set
    var erreurDonnees by mutableStateOf<String?>(null)
        private set

    // Relier le téléphone
    var relierEnCours by mutableStateOf(false)
        private set
    var erreurRelier by mutableStateOf<String?>(null)
        private set

    // Code parent oublié
    var verificationMotDePasse by mutableStateOf(false)
        private set
    var erreurMotDePasse by mutableStateOf<String?>(null)
        private set

    // Espace parent : message après un envoi
    var messageParent by mutableStateOf<String?>(null)
        private set

    // Dictée en cours
    var seanceDictee by mutableStateOf<SeanceDictee?>(null)
        private set

    // Tri des colis en cours
    var seanceCalcul by mutableStateOf<SeanceCalcul?>(null)
        private set

    // Pigeon voyageur en cours
    var seanceItalien by mutableStateOf<SeanceItalien?>(null)
        private set

    /** Les semaines d'italien suivies (lundi → mots, ou null si le parent ne les a pas encore saisis). */
    var semaines by mutableStateOf<Map<String, SemaineItalien?>>(emptyMap())
        private set
    var cartesDuParent by mutableStateOf<List<CartePostale>>(emptyList())
        private set

    /** Les 20 cartes livrées avec l'appli, puis celles créées par le parent. */
    val toutesLesCartes: List<CartePostale> get() = CARTES_INTEGREES + cartesDuParent

    // Mises à jour
    var nouvelleVersion by mutableStateOf<NouvelleVersion?>(null)
        private set
    var progressionMaj by mutableStateOf<Float?>(null)
        private set
    var messageMaj by mutableStateOf<String?>(null)
        private set

    val emailCompte: String? get() = Compte.email
    val nomVersion: String = BuildConfig.VERSION_NAME

    private var ecouteProfils: ListenerRegistration? = null
    private var ecouteCode: ListenerRegistration? = null
    private var ecouteCartes: ListenerRegistration? = null
    private val ecoutesSemaines = HashMap<String, ListenerRegistration>()
    private val donneesParEnfant = HashMap<String, DonneesEnfant>()
    private var momentArrierePlan = 0L
    private var activiteExterne = false
    private var fichierMaj: File? = null
    private var attenteAutorisation = false

    private var _voix: Voix? = null
    private var _sons: Sons? = null

    /** La voix d'Android, préparée dès que l'enfant ouvre sa séance. */
    val voix: Voix
        get() = _voix ?: Voix(getApplication<Application>()).also { _voix = it }

    val sons: Sons
        get() = _sons ?: Sons(getApplication<Application>()).also { _sons = it }

    init {
        if (Compte.estRelie) demarrerEcoute()
        verifierMiseAJour(silencieux = true)
    }

    fun profil(id: String?): Profil? = profils.firstOrNull { it.id == id }

    /** Les soirs, séances et mots à revoir d'un enfant (écoutés en direct dès le premier appel). */
    fun donnees(idProfil: String): DonneesEnfant =
        donneesParEnfant.getOrPut(idProfil) { DonneesEnfant(idProfil, depot) }

    // ---------- Données ----------

    private fun demarrerEcoute() {
        arreterEcoute()
        erreurDonnees = null
        ecouteProfils = depot.ecouterProfils(
            rappel = { liste ->
                profils = liste
                profilsCharges = true
                erreurDonnees = null
                oublierEnfantsSupprimes()
                synchroniserEcran()
            },
            erreur = { e -> erreurDonnees = messageErreurBase(e) },
        )
        ecouteCode = depot.ecouterCodeParent(
            rappel = { code, depuisCache ->
                codeParent = code
                codeParentCharge = true
                absenceCodeConfirmee = code == null && !depuisCache
                synchroniserEcran()
            },
            erreur = { e -> erreurDonnees = messageErreurBase(e) },
        )
        ecouteCartes = depot.ecouterCartesDuParent(
            rappel = { cartesDuParent = it },
            erreur = { e -> erreurDonnees = messageErreurBase(e) },
        )
        suivreSemaines()
    }

    /** Suit la semaine en cours et celle que le parent prépare (la suivante, le week-end). */
    private fun suivreSemaines() {
        if (!Compte.estRelie) return
        val aujourdhui = LocalDate.now()
        val voulues = setOf(Semaines.lundi(aujourdhui).toString(), Semaines.semaineAPreparer(aujourdhui).toString())
        for (lundi in ecoutesSemaines.keys.filter { it !in voulues }) ecoutesSemaines.remove(lundi)?.remove()
        for (lundi in voulues.filter { it !in ecoutesSemaines.keys }) {
            ecoutesSemaines[lundi] = depot.ecouterSemaineItalien(
                lundi,
                rappel = { semaine -> semaines = semaines + (lundi to semaine) },
                erreur = { e -> erreurDonnees = messageErreurBase(e) },
            )
        }
    }

    fun semaine(lundi: String): SemaineItalien? = semaines[lundi]

    fun semaineChargee(lundi: String): Boolean = semaines.containsKey(lundi)

    private fun arreterEcoute() {
        ecouteProfils?.remove()
        ecouteCode?.remove()
        ecouteCartes?.remove()
        ecouteProfils = null
        ecouteCode = null
        ecouteCartes = null
        ecoutesSemaines.values.forEach { it.remove() }
        ecoutesSemaines.clear()
        semaines = emptyMap()
        donneesParEnfant.values.forEach { it.arreter() }
        donneesParEnfant.clear()
    }

    private fun oublierEnfantsSupprimes() {
        val ids = profils.map { it.id }.toSet()
        val partis = donneesParEnfant.keys.filter { it !in ids }
        for (id in partis) donneesParEnfant.remove(id)?.arreter()
    }

    private fun messageErreurBase(e: Exception): String =
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "La base refuse l'accès. Délie puis relie le téléphone avec le compte de la famille."
        } else {
            "Problème avec la base : ${e.message}"
        }

    /** Si un profil affiché a été supprimé (sur ce téléphone ou un autre), on revient en arrière. */
    private fun synchroniserEcran() {
        when (val e = ecran) {
            is Ecran.CodeEnfant -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.AccueilEnfant -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Dictee -> if (profil(e.idProfil) == null) fermerDictee(Ecran.ChoixProfil)
            is Ecran.TriDesColis -> if (profil(e.idProfil) == null) fermerCalcul(Ecran.ChoixProfil)
            is Ecran.Pigeon -> if (profil(e.idProfil) == null) fermerItalien(Ecran.ChoixProfil)
            is Ecran.CartePostaleVue -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Epingler -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Album -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.FicheEnfant -> if (e.idProfil != null && profil(e.idProfil) == null) ecran = Ecran.EspaceParent
            is Ecran.CeSoir -> if (profil(e.idProfil) == null) ecran = Ecran.EspaceParent
            is Ecran.LectureListe -> if (profil(e.idProfil) == null) ecran = Ecran.EspaceParent
            is Ecran.NouveauCodeParent -> if (e.premiereFois && codeParent != null) ecran = Ecran.CodeDuParent
            else -> Unit
        }
    }

    // ---------- Relier le téléphone au compte de la famille ----------

    fun relier(email: String, motDePasse: String) {
        if (relierEnCours) return
        if (email.isBlank() || motDePasse.isEmpty()) {
            erreurRelier = "Remplis l'adresse e-mail et le mot de passe."
            return
        }
        relierEnCours = true
        erreurRelier = null
        viewModelScope.launch {
            try {
                Compte.connecter(email, motDePasse)
                demarrerEcoute()
                ecran = Ecran.ChoixProfil
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                erreurRelier = messageErreurConnexion(e)
            } finally {
                relierEnCours = false
            }
        }
    }

    private fun messageErreurConnexion(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Adresse e-mail ou mot de passe incorrect."
        is FirebaseAuthInvalidUserException -> "Ce compte n'existe pas ou a été désactivé."
        is FirebaseNetworkException -> "Pas de connexion Internet. Réessaie avec le Wi-Fi."
        is FirebaseTooManyRequestsException -> "Trop d'essais. Attends quelques minutes."
        else -> "Impossible de se connecter : ${e.message}"
    }

    fun delierTelephone() {
        fermerDictee(Ecran.Relier)
        fermerCalcul(Ecran.Relier)
        fermerItalien(Ecran.Relier)
        arreterEcoute()
        Compte.deconnecter()
        profils = emptyList()
        profilsCharges = false
        codeParent = null
        codeParentCharge = false
        absenceCodeConfirmee = false
        erreurDonnees = null
        ecran = Ecran.Relier
    }

    // ---------- Navigation ----------

    fun choisirProfil(profil: Profil) {
        ecran = Ecran.CodeEnfant(profil.id)
    }

    fun ouvrirEspaceParent() {
        ecran = if (codeParent != null) Ecran.CodeDuParent else Ecran.NouveauCodeParent(premiereFois = true)
    }

    fun ouvrirFiche(profil: Profil?) {
        ecran = Ecran.FicheEnfant(profil?.id)
    }

    fun ouvrirCeSoir(profil: Profil) {
        donnees(profil.id)
        messageParent = null
        ecran = Ecran.CeSoir(profil.id)
    }

    fun ouvrirLecture(profil: Profil, image: String?) {
        ecran = Ecran.LectureListe(profil.id, image)
    }

    fun changerCodeParent() {
        ecran = Ecran.NouveauCodeParent(premiereFois = false)
    }

    fun quitter() {
        fermerDictee(Ecran.ChoixProfil)
        fermerCalcul(Ecran.ChoixProfil)
        fermerItalien(Ecran.ChoixProfil)
        messageParent = null
        ecran = Ecran.ChoixProfil
    }

    /** Le bouton « retour » d'Android. Renvoie faux quand il n'y a plus rien à fermer. */
    fun retour(): Boolean {
        ecran = when (val e = ecran) {
            Ecran.Relier, Ecran.ChoixProfil -> return false
            is Ecran.NouveauCodeParent -> if (e.premiereFois) Ecran.ChoixProfil else Ecran.EspaceParent
            is Ecran.FicheEnfant -> Ecran.EspaceParent
            is Ecran.CeSoir -> Ecran.EspaceParent
            is Ecran.LectureListe -> Ecran.CeSoir(e.idProfil)
            is Ecran.Dictee -> {
                quitterDictee()
                return true
            }
            is Ecran.TriDesColis -> {
                quitterCalcul()
                return true
            }
            is Ecran.Pigeon -> {
                quitterItalien()
                return true
            }
            is Ecran.CartePostaleVue -> if (e.nouvelle) Ecran.AccueilEnfant(e.idProfil) else Ecran.Album(e.idProfil)
            is Ecran.Epingler -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.Album -> Ecran.AccueilEnfant(e.idProfil)
            Ecran.MotsItalien, Ecran.NouvelleCarte -> Ecran.EspaceParent
            else -> Ecran.ChoixProfil
        }
        return true
    }

    // ---------- Codes ----------

    fun verifierCodeEnfant(profil: Profil, code: String): Boolean {
        val bon = Codes.verifie(code, profil.codeSel, profil.codeEmpreinte)
        if (bon) {
            donnees(profil.id)
            // La voix et les bruitages mettent un moment à se préparer : on les lance tout de suite.
            voix
            sons
            ecran = Ecran.AccueilEnfant(profil.id)
        }
        return bon
    }

    fun verifierCodeParent(code: String): Boolean {
        val c = codeParent ?: return false
        val bon = Codes.verifie(code, c.sel, c.empreinte)
        if (bon) ecran = Ecran.EspaceParent
        return bon
    }

    fun enregistrerCodeParent(code: String) {
        depot.enregistrerCodeParent(code)
        ecran = Ecran.EspaceParent
    }

    /** Code parent oublié : le mot de passe du compte de la famille permet d'en choisir un nouveau. */
    fun codeOublie(motDePasse: String) {
        if (verificationMotDePasse) return
        if (motDePasse.isEmpty()) {
            erreurMotDePasse = "Tape le mot de passe du compte de la famille."
            return
        }
        verificationMotDePasse = true
        erreurMotDePasse = null
        viewModelScope.launch {
            try {
                Compte.verifierMotDePasse(motDePasse)
                ecran = Ecran.NouveauCodeParent(premiereFois = false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                erreurMotDePasse = messageErreurConnexion(e)
            } finally {
                verificationMotDePasse = false
            }
        }
    }

    fun effacerErreurMotDePasse() {
        erreurMotDePasse = null
    }

    // ---------- Profils des enfants ----------

    fun enregistrerProfil(existant: Profil?, prenom: String, classe: String, avatar: Avatar, nouveauCode: String) {
        val id = existant?.id ?: depot.nouvelIdProfil()
        val champs = mutableMapOf<String, Any>(
            "prenom" to prenom.trim(),
            "classe" to classe,
            "avatar" to avatar.cle,
        )
        if (existant == null) champs["ordre"] = (profils.maxOfOrNull { it.ordre } ?: 0L) + 1L
        if (nouveauCode.length == 4) {
            val sel = Codes.nouveauSel()
            champs["codeSel"] = sel
            champs["codeEmpreinte"] = Codes.empreinte(nouveauCode, sel)
        }
        depot.enregistrerProfil(id, champs)
        ecran = Ecran.EspaceParent
    }

    fun supprimerProfil(profil: Profil) {
        viewModelScope.launch { depot.supprimerProfil(profil.id) }
        ecran = Ecran.EspaceParent
    }

    // ---------- Saisie du soir ----------

    /** Le parent enregistre la liste de la semaine (après la photo ou une correction). */
    fun enregistrerListe(profil: Profil, mots: List<String>, retour: Boolean) {
        depot.enregistrerListe(profil.id, Mots.sansDoublons(mots))
        if (retour) ecran = Ecran.CeSoir(profil.id)
    }

    /** Envoie les mots et les tables de ce soir à l'enfant. */
    fun envoyerSoir(profil: Profil, mots: List<String>, tables: List<String>) {
        val propres = Mots.sansDoublons(mots)
        if (propres.isEmpty() && tables.isEmpty()) return
        val jour = Jours.aujourdhui()
        depot.enregistrerSoir(
            profil.id,
            Soir(date = jour, mots = propres, tables = tables, envoyeLe = System.currentTimeMillis()),
        )
        val parties = buildList {
            if (propres.isNotEmpty()) add("${propres.size} mots")
            if (tables.isNotEmpty()) add("${tables.size} table${if (tables.size > 1) "s" else ""}")
        }
        messageParent = "C'est envoyé : ${parties.joinToString(" et ")} pour ${profil.prenom} ce soir."
        ecran = Ecran.EspaceParent
    }

    fun debutActiviteExterne() {
        activiteExterne = true
    }

    fun finActiviteExterne() {
        activiteExterne = false
    }

    // ---------- La dictée ----------

    /**
     * Ouvre la dictée du dernier soir envoyé. La première séance compte (et se reprend si elle a
     * été interrompue) ; une fois terminée, les suivantes sont des entraînements.
     */
    fun commencerDictee(profil: Profil) {
        val d = donnees(profil.id)
        val soir = d.dernierSoir ?: return
        if (soir.mots.isEmpty()) return
        val existante = d.seance(soir.date, Matiere.FRANCAIS)
        val revisions = d.revisions.map { it.mot }.filter { r -> soir.mots.none { Mots.identiques(it, r) } }
        val maintenant = System.currentTimeMillis()
        val seance = when {
            existante != null && !existante.terminee -> SeanceDictee(
                profil = profil,
                soir = soir.date,
                compte = true,
                ordre = existante.ordre,
                motsRevision = existante.revisions,
                resultatsDeja = existante.resultats,
                debutLe = existante.debutLe,
                enregistrer = ::enregistrerProgression,
                terminer = ::terminerDictee,
            )
            existante == null -> SeanceDictee(
                profil = profil,
                soir = soir.date,
                compte = true,
                ordre = Mots.sansDoublons(soir.mots + revisions).shuffled(),
                motsRevision = revisions,
                resultatsDeja = emptyList(),
                debutLe = maintenant,
                enregistrer = ::enregistrerProgression,
                terminer = ::terminerDictee,
            ).also { enregistrerProgression(it) }
            else -> SeanceDictee(
                profil = profil,
                soir = soir.date,
                compte = false,
                ordre = Mots.sansDoublons(soir.mots + revisions).shuffled(),
                motsRevision = revisions,
                resultatsDeja = emptyList(),
                debutLe = maintenant,
                enregistrer = {},
                terminer = {},
            )
        }
        voix
        sons
        seanceDictee = seance
        ecran = Ecran.Dictee(profil.id)
    }

    private fun enregistrerProgression(s: SeanceDictee) {
        depot.enregistrerSeance(s.profil.id, s.versSeance())
    }

    /** Fin de la séance qui compte : points, médaille et mots à revoir. */
    private fun terminerDictee(s: SeanceDictee) {
        val actuelles = donnees(s.profil.id).revisions.associateBy { it.id }
        val aEcrire = ArrayList<Revision>()
        val aSupprimer = ArrayList<String>()
        for (r in s.resultats) {
            val cle = Mots.cle(r.mot)
            val existante = actuelles[cle]
            if (r.points < 1.0) {
                // Raté (0,5 ou 0) : il revient les jours suivants, le compteur repart de zéro.
                aEcrire += Revision(cle, Mots.normaliser(r.mot), 0, s.soir)
            } else if (existante != null && existante.dernierJour != s.soir) {
                // Réussi du premier coup : deux jours de suite et il est maîtrisé.
                val reussites = existante.reussitesDeSuite + 1
                if (reussites >= 2) aSupprimer += cle else aEcrire += existante.copy(reussitesDeSuite = reussites, dernierJour = s.soir)
            }
        }
        depot.terminerSeance(s.profil.id, s.versSeance(), REVISIONS_FRANCAIS, aEcrire, aSupprimer)
    }

    /** L'enfant arrête la dictée : le mot en cours compte 0, la séance se reprendra. */
    fun quitterDictee() {
        val id = seanceDictee?.profil?.id
        fermerDictee(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerDictee(suite: Ecran) {
        val s = seanceDictee ?: return
        s.quitteLAppli()
        _voix?.taire()
        _sons?.arreterMoteur()
        seanceDictee = null
        ecran = suite
    }

    // ---------- Le tri des colis ----------

    /**
     * Ouvre le tri des colis du dernier soir envoyé, s'il comporte des tables. Comme pour la dictée,
     * la première séance compte, se reprend si elle a été interrompue, puis les suivantes sont des
     * entraînements.
     */
    fun commencerCalcul(profil: Profil) {
        val d = donnees(profil.id)
        val soir = d.dernierSoir ?: return
        if (soir.tables.isEmpty()) return
        val existante = d.seance(soir.date, Matiere.MATHS)
        val calculsDuSoir = Calcul.pourLesTables(soir.tables)
        val dejaPoses = calculsDuSoir.map { it.canonique.texte }.toSet()
        // Les calculs ratés les jours précédents reviennent, posés au hasard dans un sens ou dans l'autre.
        val aRevoir = d.revisionsMaths.mapNotNull { Calcul.lire(it.mot) }
            .filter { it.canonique.texte !in dejaPoses }
            .map { if (Math.random() < 0.5) it else it.retourne() }
        val revisions = aRevoir.map { it.canonique.texte }
        val maintenant = System.currentTimeMillis()
        val seance = when {
            existante != null && !existante.terminee -> SeanceCalcul(
                profil = profil,
                soir = soir.date,
                compte = true,
                ordre = existante.ordre,
                calculsRevision = existante.revisions,
                resultatsDeja = existante.resultats,
                debutLe = existante.debutLe,
                enregistrer = ::enregistrerProgressionCalcul,
                terminer = ::terminerCalcul,
            )
            existante == null -> SeanceCalcul(
                profil = profil,
                soir = soir.date,
                compte = true,
                ordre = (calculsDuSoir + aRevoir).shuffled().map { it.texte },
                calculsRevision = revisions,
                resultatsDeja = emptyList<ResultatMot>(),
                debutLe = maintenant,
                enregistrer = ::enregistrerProgressionCalcul,
                terminer = ::terminerCalcul,
            ).also { enregistrerProgressionCalcul(it) }
            else -> SeanceCalcul(
                profil = profil,
                soir = soir.date,
                compte = false,
                ordre = (calculsDuSoir + aRevoir).shuffled().map { it.texte },
                calculsRevision = revisions,
                resultatsDeja = emptyList<ResultatMot>(),
                debutLe = maintenant,
                enregistrer = {},
                terminer = {},
            )
        }
        sons
        seanceCalcul = seance
        ecran = Ecran.TriDesColis(profil.id)
    }

    private fun enregistrerProgressionCalcul(s: SeanceCalcul) {
        depot.enregistrerSeance(s.profil.id, s.versSeance())
    }

    /** Fin de la séance de maths qui compte : points, médaille et calculs à revoir. */
    private fun terminerCalcul(s: SeanceCalcul) {
        val actuelles = donnees(s.profil.id).revisionsMaths.associateBy { it.id }
        val aEcrire = ArrayList<Revision>()
        val aSupprimer = ArrayList<String>()
        // Un calcul posé dans les deux sens (4 × 5 et 5 × 4) n'est réussi que si les deux le sont.
        val parCalcul = s.resultats
            .mapNotNull { r -> Calcul.lire(r.mot)?.let { it.canonique.texte to r.points } }
            .groupBy({ it.first }, { it.second })
        for ((texte, points) in parCalcul) {
            val cle = Mots.cle(texte)
            val existante = actuelles[cle]
            if (points.any { it < 1.0 }) {
                aEcrire += Revision(cle, texte, 0, s.soir)
            } else if (existante != null && existante.dernierJour != s.soir) {
                val reussites = existante.reussitesDeSuite + 1
                if (reussites >= 2) aSupprimer += cle else aEcrire += existante.copy(reussitesDeSuite = reussites, dernierJour = s.soir)
            }
        }
        depot.terminerSeance(s.profil.id, s.versSeance(), REVISIONS_MATHS, aEcrire, aSupprimer)
    }

    /** L'enfant arrête le tri : le calcul en cours compte 0, la séance se reprendra. */
    fun quitterCalcul() {
        val id = seanceCalcul?.profil?.id
        fermerCalcul(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerCalcul(suite: Ecran) {
        val s = seanceCalcul ?: return
        s.quitteLAppli()
        _sons?.arreterMoteur()
        seanceCalcul = null
        ecran = suite
    }

    // ---------- Le pigeon voyageur ----------

    private fun seancesItalienDeLaSemaine(d: DonneesEnfant, lundi: String): List<Seance> {
        val vendredi = LocalDate.parse(lundi).plusDays(4).toString()
        return d.seances.filter { it.matiere == Matiere.ITALIEN && it.soir >= lundi && it.soir <= vendredi }
    }

    /** Les mots que le pigeon apportera ce soir (vide le week-end ou sans mots de la semaine). */
    fun motsDuSoirItalien(profil: Profil): List<MotItalien> {
        val d = donnees(profil.id)
        val aujourdhui = LocalDate.now()
        val semaine = semaine(Semaines.lundi(aujourdhui).toString()) ?: return emptyList()
        val aRevoir = d.revisionsItalien.mapNotNull { MotItalien.lire(it.mot) }
        return RecompenseItalien.motsDuSoir(semaine, aujourdhui, seancesItalienDeLaSemaine(d, semaine.lundi), aRevoir)
    }

    /**
     * Ouvre le pigeon voyageur de ce soir : le mot du jour, plus ceux qui reviennent. La première séance
     * compte (et se reprend), les suivantes sont des entraînements.
     */
    fun commencerItalien(profil: Profil) {
        val d = donnees(profil.id)
        val jour = Jours.aujourdhui()
        val existante = d.seance(jour, Matiere.ITALIEN)
        val maintenant = System.currentTimeMillis()
        val seance = if (existante != null && !existante.terminee) {
            SeanceItalien(
                profil = profil,
                soir = jour,
                compte = true,
                ordre = existante.ordre,
                motsRevision = existante.revisions,
                resultatsDeja = existante.resultats,
                debutLe = existante.debutLe,
                enregistrer = ::enregistrerProgressionItalien,
                terminer = ::terminerItalien,
            )
        } else {
            val mots = if (existante != null) existante.ordre else motsDuSoirItalien(profil).map { it.code }
            if (mots.isEmpty()) return
            SeanceItalien(
                profil = profil,
                soir = jour,
                compte = existante == null,
                ordre = mots,
                motsRevision = mots.drop(1),
                resultatsDeja = emptyList<ResultatMot>(),
                debutLe = maintenant,
                enregistrer = if (existante == null) ::enregistrerProgressionItalien else { _ -> },
                terminer = if (existante == null) ::terminerItalien else { _ -> },
            ).also { if (existante == null) enregistrerProgressionItalien(it) }
        }
        seanceItalien = seance
        sons
        ecran = Ecran.Pigeon(profil.id)
    }

    private fun enregistrerProgressionItalien(s: SeanceItalien) {
        depot.enregistrerSeance(s.profil.id, s.versSeance())
    }

    /** Fin de la séance qui compte : mots qui reviendront demain, et peut-être la carte postale de la semaine. */
    private fun terminerItalien(s: SeanceItalien) {
        val d = donnees(s.profil.id)
        val actuelles = d.revisionsItalien.associateBy { it.id }
        val aEcrire = ArrayList<Revision>()
        val aSupprimer = ArrayList<String>()
        for (r in s.resultats) {
            val cle = Mots.cle(r.mot)
            if (r.points < 1.0) {
                aEcrire += Revision(cle, r.mot, 0, s.soir) // pas su du premier coup : il revient le lendemain
            } else if (actuelles.containsKey(cle)) {
                aSupprimer += cle // enfin su du premier coup : il ne revient plus
            }
        }
        depot.terminerSeance(s.profil.id, s.versSeance(), REVISIONS_ITALIEN, aEcrire, aSupprimer)

        // Une semaine sans aucune faute vaut une carte postale, apportée après le dernier mot de la semaine.
        val lundi = Semaines.lundi(LocalDate.parse(s.soir)).toString()
        val semaine = semaine(lundi) ?: return
        if (RecompenseItalien.dernierSoir(semaine) != s.soir) return
        if (d.cartesGagnees.any { it.semaine == lundi }) return
        val seances = seancesItalienDeLaSemaine(d, lundi).filter { it.soir != s.soir } + s.versSeance()
        if (!RecompenseItalien.semaineParfaite(semaine, seances)) return
        val carte = RecompenseItalien.tirerCarte(toutesLesCartes, d.cartesGagnees) ?: return
        depot.enregistrerCarteGagnee(
            s.profil.id,
            CarteGagnee(carte.id, lundi, System.currentTimeMillis(), epinglee = false, bienPlacee = false),
        )
        s.carteGagnee = carte
    }

    fun quitterItalien() {
        val id = seanceItalien?.profil?.id
        fermerItalien(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerItalien(suite: Ecran) {
        val s = seanceItalien ?: return
        s.quitteLAppli()
        seanceItalien = null
        ecran = suite
    }

    // ---------- Cartes postales et album ----------

    fun carte(id: String): CartePostale? = toutesLesCartes.firstOrNull { it.id == id }

    /** Les cartes qu'un enfant peut encore gagner. */
    fun cartesRestantes(idProfil: String): Int {
        val gagnees = donnees(idProfil).cartesGagnees.map { it.idCarte }.toSet()
        return toutesLesCartes.count { it.id !in gagnees }
    }

    /** Après la séance : le pigeon apporte la carte gagnée. */
    fun ouvrirCarteGagnee() {
        val s = seanceItalien ?: return
        val carte = s.carteGagnee ?: return
        seanceItalien = null
        ecran = Ecran.CartePostaleVue(s.profil.id, carte.id, nouvelle = true)
    }

    fun ouvrirCarte(profil: Profil, idCarte: String, nouvelle: Boolean) {
        ecran = Ecran.CartePostaleVue(profil.id, idCarte, nouvelle)
    }

    fun ouvrirEpingle(profil: Profil, idCarte: String) {
        ecran = Ecran.Epingler(profil.id, idCarte)
    }

    fun epingler(profil: Profil, idCarte: String, bienPlacee: Boolean) {
        depot.marquerEpinglee(profil.id, idCarte, bienPlacee)
    }

    fun ouvrirAlbum(profil: Profil) {
        donnees(profil.id)
        ecran = Ecran.Album(profil.id)
    }

    fun retourAccueil(profil: Profil) {
        ecran = Ecran.AccueilEnfant(profil.id)
    }

    // ---------- Italien : côté parent ----------

    fun ouvrirMotsItalien() {
        messageParent = null
        ecran = Ecran.MotsItalien
    }

    fun enregistrerMotsItalien(lundi: String, mots: List<MotItalien>) {
        depot.enregistrerSemaineItalien(SemaineItalien(lundi, mots))
        messageParent = "Les mots d'italien de la semaine sont enregistrés."
        ecran = Ecran.EspaceParent
    }

    fun ouvrirNouvelleCarte() {
        messageParent = null
        ecran = Ecran.NouvelleCarte
    }

    fun creerCarte(ville: Ville, texte: String, motFrancais: String, motItalien: String, image: String) {
        depot.enregistrerCarteDuParent(ville.nom, ville.region, texte, motFrancais, motItalien, image)
        messageParent = "La carte postale de ${ville.nom} est ajoutée."
        ecran = Ecran.EspaceParent
    }

    // ---------- Premier plan / arrière-plan ----------

    fun passeEnArrierePlan() {
        momentArrierePlan = SystemClock.elapsedRealtime()
        // Quitter l'appli pendant un mot ou un calcul : il compte 0.
        seanceDictee?.quitteLAppli()
        seanceCalcul?.quitteLAppli()
        seanceItalien?.quitteLAppli()
        _voix?.taire()
        _sons?.arreterMoteur()
    }

    fun revientAuPremierPlan() {
        val depuis = momentArrierePlan
        momentArrierePlan = 0L
        // Au-delà de 30 secondes hors de l'appli, la séance se referme (sauf photo de la liste en cours).
        if (depuis != 0L && !activiteExterne && SystemClock.elapsedRealtime() - depuis > 30_000L) {
            if (ecran != Ecran.Relier && ecran != Ecran.ChoixProfil) quitter()
        }
        if (erreurDonnees != null && Compte.estRelie) demarrerEcoute() else suivreSemaines()
        if (attenteAutorisation && MiseAJour.peutInstaller(getApplication<Application>())) {
            attenteAutorisation = false
            messageMaj = null
            lancerInstallation()
        }
    }

    // ---------- Mises à jour ----------

    fun verifierMiseAJour(silencieux: Boolean) {
        viewModelScope.launch {
            if (!silencieux) messageMaj = "Recherche d'une nouvelle version…"
            val resultat = runCatching { MiseAJour.chercher() }
            val probleme = resultat.exceptionOrNull()
            if (probleme is CancellationException) throw probleme
            val version = resultat.getOrNull()
            if (probleme == null) nouvelleVersion = version
            if (!silencieux) {
                messageMaj = when {
                    probleme != null -> "Impossible de vérifier. Le téléphone est-il connecté à Internet ?"
                    version == null -> "L'appli est à jour (version $nomVersion)."
                    else -> null
                }
            }
        }
    }

    fun installerMiseAJour() {
        val version = nouvelleVersion ?: return
        if (progressionMaj != null) return
        val contexte = getApplication<Application>()
        viewModelScope.launch {
            progressionMaj = 0f
            messageMaj = null
            try {
                fichierMaj = MiseAJour.telecharger(contexte, version) { p -> progressionMaj = p }
                progressionMaj = null
                lancerInstallation()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                messageMaj = "Le téléchargement a échoué. Réessaie avec une bonne connexion."
            } finally {
                progressionMaj = null
            }
        }
    }

    private fun lancerInstallation() {
        val contexte = getApplication<Application>()
        val fichier = fichierMaj ?: return
        if (MiseAJour.peutInstaller(contexte)) {
            MiseAJour.installer(contexte, fichier)
        } else {
            attenteAutorisation = true
            messageMaj = "Autorise « Courrier des Nuages » à installer des applis, puis reviens ici."
            MiseAJour.demanderAutorisation(contexte)
        }
    }

    override fun onCleared() {
        arreterEcoute()
        _voix?.liberer()
        _sons?.liberer()
    }
}
