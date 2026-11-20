package fr.courrierdesnuages.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import fr.courrierdesnuages.app.cartes.Atlases
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.data.CARTES_FRANCE
import fr.courrierdesnuages.app.data.CARTES_ITALIE
import fr.courrierdesnuages.app.data.CARTES_MONDE
import fr.courrierdesnuages.app.data.CodeParent
import fr.courrierdesnuages.app.data.Codes
import fr.courrierdesnuages.app.data.Compte
import fr.courrierdesnuages.app.data.Depot
import fr.courrierdesnuages.app.data.DonneesEnfant
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.REVISIONS_FRANCAIS
import fr.courrierdesnuages.app.data.REVISIONS_MATHS
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Revision
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.data.Soir
import fr.courrierdesnuages.app.dictee.SeanceDictee
import fr.courrierdesnuages.app.france.DEPARTEMENTS
import fr.courrierdesnuages.app.hangar.Hangar
import fr.courrierdesnuages.app.hangar.piecesDuHangar
import fr.courrierdesnuages.app.italien.CARTES_INTEGREES
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.italien.CartePostale
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.italien.Ville
import fr.courrierdesnuages.app.langues.Langue
import fr.courrierdesnuages.app.langues.ModeReponse
import fr.courrierdesnuages.app.langues.MotLangue
import fr.courrierdesnuages.app.langues.RecompenseLangue
import fr.courrierdesnuages.app.langues.SeanceLangue
import fr.courrierdesnuages.app.langues.SemaineLangue
import fr.courrierdesnuages.app.maj.MiseAJour
import fr.courrierdesnuages.app.maj.NouvelleVersion
import fr.courrierdesnuages.app.monde.TOUR_DU_MONDE
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.son.Voix
import fr.courrierdesnuages.app.suivi.Classement
import fr.courrierdesnuages.app.suivi.RemiseAZero
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    data class SeanceDeLangue(val idProfil: String, val langue: Langue) : Ecran
    data class CartePostaleVue(val idProfil: String, val idCarte: String, val nouvelle: Boolean) : Ecran
    data class Epingler(val idProfil: String, val idCarte: String) : Ecran
    data class Album(val idProfil: String) : Ecran
    data class CarteDepartement(val idProfil: String, val code: String, val nouvelle: Boolean) : Ecran
    data class EpinglerDepartement(val idProfil: String, val code: String) : Ecran
    data class AlbumFrance(val idProfil: String) : Ecran
    data class CarteMondeVue(val idProfil: String, val id: String, val nouvelle: Boolean) : Ecran
    data class EpinglerMonde(val idProfil: String, val id: String) : Ecran
    data class AlbumMonde(val idProfil: String) : Ecran
    data class VoirHangar(val idProfil: String) : Ecran
    data class MotsLangue(val langue: Langue) : Ecran
    data object NouvelleCarte : Ecran
    data class VoirClassement(val idProfil: String) : Ecran
    data class Tournee(val idProfil: String) : Ecran
    data class Suivi(val idProfil: String) : Ecran
}

/** Une carte offerte par le parent n'appartient à aucune semaine (elle n'empêche pas d'en gagner une). */
const val SEMAINE_OFFERTE = "offerte"

/** Les séances et les soirs d'un enfant depuis la rentrée, pour le suivi parent. */
data class AnneeSuivie(val idProfil: String, val seances: List<Seance>, val soirs: List<Soir>)

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
    /** Un message pour l'enfant sur son accueil (après une remise à zéro faite par le parent). */
    var messageEnfant by mutableStateOf<Pair<String, String>?>(null)
        private set

    fun oublierMessageEnfant() {
        messageEnfant = null
    }

    private var surveillance: Job? = null

    var seanceDictee by mutableStateOf<SeanceDictee?>(null)
        private set

    // Tri des colis en cours
    var seanceCalcul by mutableStateOf<SeanceCalcul?>(null)
        private set

    // Séance de langue en cours (le pigeon voyageur pour l'italien, l'albatros pour l'anglais)
    var seanceLangue by mutableStateOf<SeanceLangue?>(null)
        private set

    /**
     * Les semaines de mots suivies, par langue (« italien:2026-11-16 » → mots, ou null si le parent ne
     * les a pas encore saisis) : la semaine d'avant, celle en cours et la suivante.
     */
    var semaines by mutableStateOf<Map<String, SemaineLangue?>>(emptyMap())
        private set
    var cartesDuParent by mutableStateOf<List<CartePostale>>(emptyList())
        private set

    /** Les 20 cartes livrées avec l'appli, puis celles créées par le parent. */
    val toutesLesCartes: List<CartePostale> get() = CARTES_INTEGREES + cartesDuParent

    // Mises à jour
    var nouvelleVersion by mutableStateOf<NouvelleVersion?>(null)
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
    private val recalculsLances = HashSet<String>()
    private var momentArrierePlan = 0L
    private var activiteExterne = false
    private var dernierControleMaj = 0L

    private var _voix: Voix? = null
    private var _sons: Sons? = null

    /** La voix d'Android, préparée dès que l'enfant ouvre sa séance. */
    val voix: Voix
        get() = _voix ?: Voix(getApplication<Application>()).also { _voix = it }

    val sons: Sons
        get() = _sons ?: Sons(getApplication<Application>()).also { _sons = it }

    init {
        if (Compte.estRelie) demarrerEcoute()
        MiseAJour.nettoyer(application)
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
                recalculerSiBesoin()
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

    /**
     * Suit, pour chaque langue, la semaine d'avant (pour les vignettes), celle en cours et la suivante
     * (que le parent peut préparer à l'avance).
     */
    private fun suivreSemaines() {
        if (!Compte.estRelie) return
        val lundi = Semaines.lundi(LocalDate.now())
        val voulues = Langue.entries.flatMap { langue ->
            listOf(lundi.minusWeeks(1), lundi, lundi.plusWeeks(1)).map { langue to it.toString() }
        }
        val cles = voulues.map { (langue, l) -> cleSemaine(langue, l) }.toSet()
        for (cle in ecoutesSemaines.keys.filter { it !in cles }) ecoutesSemaines.remove(cle)?.remove()
        for ((langue, l) in voulues) {
            val cle = cleSemaine(langue, l)
            if (cle in ecoutesSemaines) continue
            ecoutesSemaines[cle] = depot.ecouterSemaine(
                langue,
                l,
                rappel = { semaine -> semaines = semaines + (cle to semaine) },
                erreur = { e -> erreurDonnees = messageErreurBase(e) },
            )
        }
    }

    private fun cleSemaine(langue: Langue, lundi: String) = "${langue.collectionSemaines}:$lundi"

    fun semaine(langue: Langue, lundi: String): SemaineLangue? = semaines[cleSemaine(langue, lundi)]

    fun semaineChargee(langue: Langue, lundi: String): Boolean = semaines.containsKey(cleSemaine(langue, lundi))

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
            is Ecran.SeanceDeLangue -> if (profil(e.idProfil) == null) fermerLangue(Ecran.ChoixProfil)
            is Ecran.CartePostaleVue -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Epingler -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Album -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.CarteDepartement -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.EpinglerDepartement -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.AlbumFrance -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.CarteMondeVue -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.EpinglerMonde -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.AlbumMonde -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.VoirHangar -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.VoirClassement -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Tournee -> if (profil(e.idProfil) == null) ecran = Ecran.ChoixProfil
            is Ecran.Suivi -> if (profil(e.idProfil) == null) ecran = Ecran.EspaceParent
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
        fermerLangue(Ecran.Relier)
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
        fermerLangue(Ecran.ChoixProfil)
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
            is Ecran.SeanceDeLangue -> {
                quitterLangue()
                return true
            }
            is Ecran.CartePostaleVue -> if (e.nouvelle) Ecran.AccueilEnfant(e.idProfil) else Ecran.Album(e.idProfil)
            is Ecran.Epingler -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.Album -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.CarteDepartement -> if (e.nouvelle) Ecran.AccueilEnfant(e.idProfil) else Ecran.AlbumFrance(e.idProfil)
            is Ecran.EpinglerDepartement -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.AlbumFrance -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.CarteMondeVue -> if (e.nouvelle) Ecran.AccueilEnfant(e.idProfil) else Ecran.AlbumMonde(e.idProfil)
            is Ecran.EpinglerMonde -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.AlbumMonde -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.VoirHangar -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.VoirClassement -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.Tournee -> Ecran.AccueilEnfant(e.idProfil)
            is Ecran.Suivi -> Ecran.EspaceParent
            is Ecran.MotsLangue, Ecran.NouvelleCarte -> Ecran.EspaceParent
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
            // La carte de France des albums se lit pendant ce temps.
            viewModelScope.launch(Dispatchers.Default) {
                runCatching {
                    Atlases.france(getApplication())
                    Atlases.monde(getApplication())
                }
            }
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
        // Le soir précédent se ferme : une séance pas faite compte 0, une séance pas finie compte ce
        // qui a été fait. Les compteurs de l'année sont recalculés en entier à cette occasion.
        recalculer(profil.id, ouvert = jour)
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
        messageEnfant = null
        surveiller(profil, seance.compte, Seance.idSeance(seance.soir, Matiere.FRANCAIS), Matiere.FRANCAIS)
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
        depot.terminerSeance(s.profil.id, s.versSeance(), REVISIONS_FRANCAIS, aEcrire, aSupprimer, RemiseAZero.trace(aEcrire, aSupprimer, actuelles))
        compter(s.profil, s.versSeance())

        // Tous les points : le Petit Avion rapporte la carte d'un département que l'enfant n'a pas
        // encore. Elle est liée à la séance, pour disparaître si le parent la remet à zéro.
        if (s.medaille == Medaille.OR) {
            val dep = RecompenseLangue.tirer(DEPARTEMENTS, { it.code }, donnees(s.profil.id).cartesFrance) ?: return
            depot.enregistrerCarteGagnee(
                s.profil.id,
                CARTES_FRANCE,
                CarteGagnee(dep.code, Seance.idSeance(s.soir, Matiere.FRANCAIS), System.currentTimeMillis(), epinglee = false, bienPlacee = false),
            )
            s.carteGagnee = dep.code
        }
    }

    /** Après une dictée à 100 % : la carte de France rapportée par le Petit Avion. */
    fun ouvrirCarteDeLaDictee() {
        val s = seanceDictee ?: return
        val code = s.carteGagnee ?: return
        fermerDictee(Ecran.CarteDepartement(s.profil.id, code, nouvelle = true))
    }

    /** Ajoute une séance terminée aux compteurs de l'année (classement, médailles, tournée des îles). */
    private fun compter(profil: Profil, seance: Seance) {
        val an = Jours.anneeScolaire(LocalDate.parse(seance.soir))
        depot.ajouterAuxStats(profil.id, an, listOf(Classement.pourSeance(seance)))
    }

    /**
     * Première ouverture de la version avec le classement (ou nouvelle année scolaire) : les compteurs
     * de l'année sont calculés à partir des séances déjà faites.
     */
    private fun recalculerSiBesoin() {
        for (p in profils) {
            if (!p.stats.calcule && recalculsLances.add(p.id)) {
                recalculer(p.id, ouvert = null, apresEchec = { recalculsLances.remove(p.id) })
            }
        }
    }

    /**
     * Recalcule les compteurs de l'année à partir de toutes les séances et de tous les soirs depuis la
     * rentrée. Seulement avec Internet, pour ne rien oublier : sans connexion, on réessaiera plus tard.
     * [ouvert] est le soir encore ouvert (par défaut, le dernier envoyé).
     */
    private fun recalculer(idProfil: String, ouvert: String?, apresEchec: () -> Unit = {}) {
        viewModelScope.launch {
            val an = Jours.anneeScolaire()
            val debut = Jours.rentree(an)
            try {
                val seances = depot.seancesDepuis(idProfil, debut, serveur = true)
                val soirs = depot.soirsDepuis(idProfil, debut, serveur = true)
                val dernier = ouvert ?: soirs.maxOfOrNull { it.date } ?: ""
                depot.ecrireStats(idProfil, an, Classement.recalculer(seances, soirs, dernier))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                apresEchec()
            }
        }
    }

    /** L'enfant arrête la dictée : le mot en cours compte 0, la séance se reprendra. */
    fun quitterDictee() {
        val id = seanceDictee?.profil?.id
        fermerDictee(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerDictee(suite: Ecran) {
        val s = seanceDictee ?: return
        arreterSurveillance()
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
        messageEnfant = null
        surveiller(profil, seance.compte, Seance.idSeance(seance.soir, Matiere.MATHS), Matiere.MATHS)
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
        depot.terminerSeance(s.profil.id, s.versSeance(), REVISIONS_MATHS, aEcrire, aSupprimer, RemiseAZero.trace(aEcrire, aSupprimer, actuelles))
        // Tous les points : une pièce pour le hangar (lue avant que la médaille ne soit comptée).
        if (s.medaille == Medaille.OR) {
            val avant = (profil(s.profil.id) ?: s.profil).piecesDuHangar
            s.nouvellePiece = Hangar.nouvellePiece(avant + 1)
        }
        compter(s.profil, s.versSeance())
    }

    /** Après une séance de maths à 100 % : le hangar, avec sa nouvelle pièce. */
    fun ouvrirHangarDepuisCalcul() {
        val s = seanceCalcul ?: return
        fermerCalcul(Ecran.VoirHangar(s.profil.id))
    }

    fun ouvrirHangar(profil: Profil) {
        ecran = Ecran.VoirHangar(profil.id)
    }

    /** L'enfant choisit l'engin de son facteur (null : l'engin de départ de son avatar). */
    fun choisirEngin(profil: Profil, cle: String?) {
        depot.choisirEngin(profil.id, cle)
    }

    /** L'enfant arrête le tri : le calcul en cours compte 0, la séance se reprendra. */
    fun quitterCalcul() {
        val id = seanceCalcul?.profil?.id
        fermerCalcul(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerCalcul(suite: Ecran) {
        val s = seanceCalcul ?: return
        arreterSurveillance()
        s.quitteLAppli()
        _sons?.arreterMoteur()
        seanceCalcul = null
        ecran = suite
    }

    // ---------- Les langues : italien et anglais ----------

    /** Les séances d'une langue dans une semaine, du lundi au dimanche. */
    private fun seancesDeLaSemaine(d: DonneesEnfant, langue: Langue, lundi: String): List<Seance> {
        val dimanche = LocalDate.parse(lundi).plusDays(6).toString()
        return d.seances.filter { it.matiere == langue.matiere && it.soir >= lundi && it.soir <= dimanche }
    }

    /**
     * La façon de répondre d'un mot : celle choisie par le parent dans la semaine en cours (ou celle
     * d'avant), sinon celle gardée avec le mot à revoir.
     */
    private fun modeDuMot(langue: Langue, code: String, revision: Revision?): ModeReponse {
        val lundi = Semaines.lundi(LocalDate.now())
        for (l in listOf(lundi, lundi.minusWeeks(1))) {
            semaine(langue, l.toString())?.mode(code)?.let { return it }
        }
        return ModeReponse.depuisCle(revision?.mode)
    }

    /** Les mots que le messager apportera aujourd'hui : ceux du jour, ceux qui attendent et ceux à revoir. */
    fun motsDuSoir(langue: Langue, profil: Profil): List<MotLangue> {
        val d = donnees(profil.id)
        val aujourdhui = LocalDate.now()
        val lundi = Semaines.lundi(aujourdhui).toString()
        val aRevoir = d.revisionsDe(langue).mapNotNull { r -> MotLangue.lire(r.mot, modeDuMot(langue, r.mot, r)) }
        return RecompenseLangue.motsDuSoir(semaine(langue, lundi), aujourdhui, seancesDeLaSemaine(d, langue, lundi), aRevoir)
    }

    /** Les mots que le parent a prévus pour aujourd'hui. */
    fun motsDuJour(langue: Langue): List<MotLangue> {
        val aujourdhui = LocalDate.now()
        return semaine(langue, Semaines.lundi(aujourdhui).toString())?.motsDuJour(Semaines.numeroDuJour(aujourdhui)).orEmpty()
    }

    /**
     * Ouvre la séance de langue du jour : les mots du jour, plus ceux qui attendent et ceux qui
     * reviennent. La première séance du jour compte (et se reprend), les suivantes sont des entraînements.
     */
    fun commencerLangue(langue: Langue, profil: Profil) {
        val d = donnees(profil.id)
        val jour = Jours.aujourdhui()
        val existante = d.seance(jour, langue.matiere)
        val maintenant = System.currentTimeMillis()
        // Les vignettes sont tirées parmi les mots de la semaine (et, s'il en manque, de la semaine d'avant).
        val lundi = Semaines.lundi(LocalDate.now())
        val deLaSemaine = semaine(langue, lundi.toString())?.tous.orEmpty().map { it.traduction }
        val deSecours = semaine(langue, lundi.minusWeeks(1).toString())?.tous.orEmpty().map { it.traduction }
        val revisions = d.revisionsDe(langue).associateBy { it.mot }
        val seance = if (existante != null && !existante.terminee) {
            SeanceLangue(
                langue = langue,
                profil = profil,
                soir = jour,
                compte = true,
                ordre = existante.ordre,
                modes = existante.ordre.associateWith { modeDuMot(langue, it, revisions[it]) },
                motsRevision = existante.revisions,
                resultatsDeja = existante.resultats,
                debutLe = existante.debutLe,
                enregistrer = ::enregistrerProgressionLangue,
                terminer = ::terminerLangue,
                motsDeLaSemaine = deLaSemaine,
                motsDeSecours = deSecours,
            )
        } else {
            val mots = if (existante != null) {
                existante.ordre.mapNotNull { MotLangue.lire(it, modeDuMot(langue, it, revisions[it])) }
            } else {
                motsDuSoir(langue, profil).shuffled()
            }
            if (mots.isEmpty()) return
            val duJour = motsDuJour(langue).map { it.code }.toSet()
            val ordre = mots.map { it.code }
            SeanceLangue(
                langue = langue,
                profil = profil,
                soir = jour,
                compte = existante == null,
                ordre = ordre,
                modes = mots.associate { it.code to it.mode },
                motsRevision = ordre.filter { it !in duJour },
                resultatsDeja = emptyList<ResultatMot>(),
                debutLe = maintenant,
                enregistrer = if (existante == null) ::enregistrerProgressionLangue else { _ -> },
                terminer = if (existante == null) ::terminerLangue else { _ -> },
                motsDeLaSemaine = deLaSemaine,
                motsDeSecours = deSecours,
            ).also { if (existante == null) enregistrerProgressionLangue(it) }
        }
        seanceLangue = seance
        sons
        messageEnfant = null
        surveiller(profil, seance.compte, Seance.idSeance(seance.soir, langue.matiere), langue.matiere)
        ecran = Ecran.SeanceDeLangue(profil.id, langue)
    }

    private fun enregistrerProgressionLangue(s: SeanceLangue) {
        depot.enregistrerSeance(s.profil.id, s.versSeance())
    }

    /** Fin de la séance qui compte : mots qui reviendront, compteurs, et peut-être la carte postale de la semaine. */
    private fun terminerLangue(s: SeanceLangue) {
        val langue = s.langue
        val d = donnees(s.profil.id)
        val actuelles = d.revisionsDe(langue).associateBy { it.id }
        val aEcrire = ArrayList<Revision>()
        val aSupprimer = ArrayList<String>()
        for (r in s.resultats) {
            val cle = Mots.cle(r.mot)
            if (r.points < 1.0) {
                // Pas su du premier coup : il revient le lendemain, avec la même façon de répondre.
                aEcrire += Revision(cle, r.mot, 0, s.soir, mode = s.modeDe(r.mot).cle)
            } else if (actuelles.containsKey(cle)) {
                aSupprimer += cle // enfin su du premier coup : il ne revient plus
            }
        }
        depot.terminerSeance(s.profil.id, s.versSeance(), langue.collectionRevisions, aEcrire, aSupprimer, RemiseAZero.trace(aEcrire, aSupprimer, actuelles))
        compter(s.profil, s.versSeance())

        // Tous les mots de la semaine sus du premier coup : le messager apporte une carte postale,
        // dès que le dernier d'entre eux a été fait (le dernier jour, ou plus tard pour un soir rattrapé).
        val lundi = Semaines.lundi(LocalDate.parse(s.soir)).toString()
        val semaine = semaine(langue, lundi) ?: return
        val cartes = d.cartesDe(langue)
        if (cartes.any { it.semaine == lundi }) return
        val seances = seancesDeLaSemaine(d, langue, lundi).filter { it.soir != s.soir } + s.versSeance()
        if (!RecompenseLangue.semaineParfaite(semaine, seances)) return
        val idCarte = tirerCarteDeLangue(langue, cartes) ?: return
        depot.enregistrerCarteGagnee(
            s.profil.id,
            langue.collectionCartes,
            CarteGagnee(idCarte, lundi, System.currentTimeMillis(), epinglee = false, bienPlacee = false),
        )
        s.carteGagnee = idCarte
    }

    /** Une carte de l'album de la langue que l'enfant n'a pas encore. */
    private fun tirerCarteDeLangue(langue: Langue, gagnees: List<CarteGagnee>): String? = when (langue) {
        Langue.ITALIEN -> RecompenseLangue.tirer(toutesLesCartes, { it.id }, gagnees)?.id
        Langue.ANGLAIS -> RecompenseLangue.tirer(TOUR_DU_MONDE, { it.id }, gagnees)?.id
    }

    fun quitterLangue() {
        val id = seanceLangue?.profil?.id
        fermerLangue(if (id != null) Ecran.AccueilEnfant(id) else Ecran.ChoixProfil)
    }

    private fun fermerLangue(suite: Ecran) {
        val s = seanceLangue ?: return
        arreterSurveillance()
        s.quitteLAppli()
        seanceLangue = null
        ecran = suite
    }

    // ---------- Remise à zéro (toujours décidée par le parent) ----------

    /**
     * Le parent remet à zéro la séance du jour d'un enfant (après un souci de téléphone, par
     * exemple) : elle est effacée avec ses points, sa médaille, ses mots à revoir et la carte
     * postale qu'elle avait rapportée. L'enfant la recommence, et elle compte de nouveau.
     */
    fun remettreAZero(profil: Profil, seance: Seance) {
        val d = donnees(profil.id)
        val langue = Langue.depuisMatiere(seance.matiere)
        val cartes = when {
            langue != null -> d.cartesDe(langue)
            seance.matiere == Matiere.FRANCAIS -> d.cartesFrance
            else -> emptyList()
        }
        val plan = RemiseAZero.preparer(seance, cartes)
        val collection = when {
            langue != null -> langue.collectionRevisions
            seance.matiere == Matiere.MATHS -> REVISIONS_MATHS
            else -> REVISIONS_FRANCAIS
        }
        val an = runCatching { Jours.anneeScolaire(LocalDate.parse(seance.soir)) }.getOrDefault(Jours.anneeScolaire())
        depot.remettreAZero(profil.id, an, collection, plan)
    }

    /**
     * Pendant une séance qui compte : si le parent la remet à zéro depuis un autre téléphone, elle
     * s'arrête aussi ici, sans rien réécrire, et l'enfant peut la recommencer.
     */
    private fun surveiller(profil: Profil, compte: Boolean, idSeance: String, matiere: String) {
        arreterSurveillance()
        if (!compte) return
        val d = donnees(profil.id)
        surveillance = viewModelScope.launch {
            var vue = false
            snapshotFlow { d.seancesChargees to d.seances.any { it.id == idSeance } }.collect { (chargees, presente) ->
                if (presente) {
                    vue = true
                } else if (vue && chargees) {
                    seanceRemiseAZero(profil, matiere)
                }
            }
        }
    }

    private fun arreterSurveillance() {
        surveillance?.cancel()
        surveillance = null
    }

    private fun seanceRemiseAZero(profil: Profil, matiere: String) {
        arreterSurveillance()
        _voix?.taire()
        _sons?.arreterMoteur()
        when (matiere) {
            Matiere.FRANCAIS -> seanceDictee = null
            Matiere.MATHS -> seanceCalcul = null
            else -> seanceLangue = null
        }
        val e = ecran
        if (e is Ecran.Dictee || e is Ecran.TriDesColis || e is Ecran.SeanceDeLangue) ecran = Ecran.AccueilEnfant(profil.id)
        val nom = when (matiere) {
            Matiere.MATHS -> "maths"
            Matiere.ITALIEN -> "italien"
            Matiere.ANGLAIS -> "anglais"
            else -> "français"
        }
        messageEnfant = profil.id to "Ta séance de $nom a été remise à zéro : tu peux la recommencer."
    }

    // ---------- Cartes postales et album ----------

    fun carte(id: String): CartePostale? = toutesLesCartes.firstOrNull { it.id == id }

    /** Les cartes qu'un enfant peut encore gagner. */
    fun cartesRestantes(idProfil: String): Int {
        val gagnees = donnees(idProfil).cartesGagnees.map { it.idCarte }.toSet()
        return toutesLesCartes.count { it.id !in gagnees }
    }

    /** Le parent offre une carte à un enfant (au hasard parmi celles qu'il n'a pas, ou une carte choisie). */
    fun offrirCarte(profil: Profil, carte: CartePostale?) {
        val choisie = carte ?: RecompenseLangue.tirer(toutesLesCartes, { it.id }, donnees(profil.id).cartesGagnees) ?: return
        depot.enregistrerCarteGagnee(
            profil.id,
            CARTES_ITALIE,
            CarteGagnee(choisie.id, SEMAINE_OFFERTE, System.currentTimeMillis(), epinglee = false, bienPlacee = false),
        )
        messageParent = "La carte de ${choisie.ville} attend ${profil.prenom} sur son accueil."
    }

    /** Les cartes qu'un enfant n'a pas encore. */
    fun cartesAGagner(idProfil: String): List<CartePostale> {
        val gagnees = donnees(idProfil).cartesGagnees.map { it.idCarte }.toSet()
        return toutesLesCartes.filter { it.id !in gagnees }
    }

    /** Après la séance : le messager apporte la carte gagnée. */
    fun ouvrirCarteGagnee() {
        val s = seanceLangue ?: return
        val carte = s.carteGagnee ?: return
        arreterSurveillance()
        seanceLangue = null
        ecran = when (s.langue) {
            Langue.ITALIEN -> Ecran.CartePostaleVue(s.profil.id, carte, nouvelle = true)
            Langue.ANGLAIS -> Ecran.CarteMondeVue(s.profil.id, carte, nouvelle = true)
        }
    }

    fun ouvrirCarte(profil: Profil, idCarte: String, nouvelle: Boolean) {
        ecran = Ecran.CartePostaleVue(profil.id, idCarte, nouvelle)
    }

    fun ouvrirEpingle(profil: Profil, idCarte: String) {
        ecran = Ecran.Epingler(profil.id, idCarte)
    }

    fun epingler(profil: Profil, idCarte: String, bienPlacee: Boolean) {
        depot.marquerEpinglee(profil.id, CARTES_ITALIE, idCarte, bienPlacee)
    }

    fun ouvrirAlbum(profil: Profil) {
        donnees(profil.id)
        ecran = Ecran.Album(profil.id)
    }

    // ---------- L'album de France ----------

    fun ouvrirCarteDepartement(profil: Profil, code: String, nouvelle: Boolean) {
        ecran = Ecran.CarteDepartement(profil.id, code, nouvelle)
    }

    fun ouvrirEpingleDepartement(profil: Profil, code: String) {
        ecran = Ecran.EpinglerDepartement(profil.id, code)
    }

    fun epinglerDepartement(profil: Profil, code: String, bienPlacee: Boolean) {
        depot.marquerEpinglee(profil.id, CARTES_FRANCE, code, bienPlacee)
    }

    fun ouvrirAlbumFrance(profil: Profil) {
        donnees(profil.id)
        ecran = Ecran.AlbumFrance(profil.id)
    }

    // ---------- Le tour du monde anglophone ----------

    fun ouvrirCarteMonde(profil: Profil, id: String, nouvelle: Boolean) {
        ecran = Ecran.CarteMondeVue(profil.id, id, nouvelle)
    }

    fun ouvrirEpingleMonde(profil: Profil, id: String) {
        ecran = Ecran.EpinglerMonde(profil.id, id)
    }

    fun epinglerMonde(profil: Profil, id: String, bienPlacee: Boolean) {
        depot.marquerEpinglee(profil.id, CARTES_MONDE, id, bienPlacee)
    }

    fun ouvrirAlbumMonde(profil: Profil) {
        donnees(profil.id)
        ecran = Ecran.AlbumMonde(profil.id)
    }

    fun ouvrirClassement(profil: Profil) {
        ecran = Ecran.VoirClassement(profil.id)
    }

    fun ouvrirTournee(profil: Profil) {
        ecran = Ecran.Tournee(profil.id)
    }

    /** L'année du suivi parent (séances et soirs depuis la rentrée), lue à l'ouverture du suivi. */
    var anneeSuivie by mutableStateOf<AnneeSuivie?>(null)
        private set

    fun ouvrirSuivi(profil: Profil) {
        donnees(profil.id)
        messageParent = null
        if (anneeSuivie?.idProfil != profil.id) anneeSuivie = null
        ecran = Ecran.Suivi(profil.id)
        viewModelScope.launch {
            val debut = Jours.rentree(Jours.anneeScolaire())
            try {
                val seances = depot.seancesDepuis(profil.id, debut, serveur = false)
                val soirs = depot.soirsDepuis(profil.id, debut, serveur = false)
                anneeSuivie = AnneeSuivie(profil.id, seances, soirs)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Sans l'année entière, le suivi montre les derniers jours (toujours à jour).
            }
        }
    }

    fun retourAccueil(profil: Profil) {
        ecran = Ecran.AccueilEnfant(profil.id)
    }

    // ---------- Langues : côté parent ----------

    fun ouvrirMotsLangue(langue: Langue) {
        messageParent = null
        ecran = Ecran.MotsLangue(langue)
    }

    fun enregistrerMotsLangue(langue: Langue, semaines: List<SemaineLangue>) {
        for (s in semaines) depot.enregistrerSemaine(langue, s)
        messageParent = "Les mots ${langue.deLaLangue} sont enregistrés."
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
        seanceLangue?.quitteLAppli()
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
        recalculerSiBesoin()
        // L'appli reste souvent ouverte en arrière-plan : on regarde s'il y a une nouvelle version
        // en revenant dessus (au plus une fois toutes les 10 minutes).
        if (SystemClock.elapsedRealtime() - dernierControleMaj > 10 * 60_000L) {
            verifierMiseAJour(silencieux = true)
        }
    }

    // ---------- Mises à jour ----------

    fun verifierMiseAJour(silencieux: Boolean) {
        dernierControleMaj = SystemClock.elapsedRealtime()
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

    /**
     * Ouvre le téléchargement de la nouvelle version dans le navigateur. Android l'installe quand on
     * ouvre le fichier téléchargé (la première fois, il demande d'autoriser le navigateur).
     */
    fun telechargerMiseAJour() {
        val ok = MiseAJour.ouvrirTelechargement(getApplication(), nouvelleVersion)
        messageMaj = if (ok) {
            "Le navigateur télécharge la nouvelle version : ouvre ensuite le fichier téléchargé pour l'installer."
        } else {
            "Aucun navigateur n'est installé sur ce téléphone pour télécharger la mise à jour."
        }
    }

    override fun onCleared() {
        arreterEcoute()
        _voix?.liberer()
        _sons?.liberer()
    }
}
