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
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.data.CodeParent
import fr.courrierdesnuages.app.data.Codes
import fr.courrierdesnuages.app.data.Compte
import fr.courrierdesnuages.app.data.Depot
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.maj.MiseAJour
import fr.courrierdesnuages.app.maj.NouvelleVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

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
    private var momentArrierePlan = 0L
    private var fichierMaj: File? = null
    private var attenteAutorisation = false

    init {
        if (Compte.estRelie) demarrerEcoute()
        verifierMiseAJour(silencieux = true)
    }

    fun profil(id: String?): Profil? = profils.firstOrNull { it.id == id }

    // ---------- Données ----------

    private fun demarrerEcoute() {
        arreterEcoute()
        erreurDonnees = null
        ecouteProfils = depot.ecouterProfils(
            rappel = { liste ->
                profils = liste
                profilsCharges = true
                erreurDonnees = null
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
    }

    private fun arreterEcoute() {
        ecouteProfils?.remove()
        ecouteCode?.remove()
        ecouteProfils = null
        ecouteCode = null
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
            is Ecran.FicheEnfant -> if (e.idProfil != null && profil(e.idProfil) == null) ecran = Ecran.EspaceParent
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

    fun changerCodeParent() {
        ecran = Ecran.NouveauCodeParent(premiereFois = false)
    }

    fun quitter() {
        ecran = Ecran.ChoixProfil
    }

    /** Le bouton « retour » d'Android. Renvoie faux quand il n'y a plus rien à fermer. */
    fun retour(): Boolean {
        ecran = when (val e = ecran) {
            Ecran.Relier, Ecran.ChoixProfil -> return false
            is Ecran.NouveauCodeParent -> if (e.premiereFois) Ecran.ChoixProfil else Ecran.EspaceParent
            is Ecran.FicheEnfant -> Ecran.EspaceParent
            else -> Ecran.ChoixProfil
        }
        return true
    }

    // ---------- Codes ----------

    fun verifierCodeEnfant(profil: Profil, code: String): Boolean {
        val bon = Codes.verifie(code, profil.codeSel, profil.codeEmpreinte)
        if (bon) ecran = Ecran.AccueilEnfant(profil.id)
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
        depot.supprimerProfil(profil.id)
        ecran = Ecran.EspaceParent
    }

    // ---------- Premier plan / arrière-plan ----------

    fun passeEnArrierePlan() {
        momentArrierePlan = SystemClock.elapsedRealtime()
    }

    fun revientAuPremierPlan() {
        val depuis = momentArrierePlan
        momentArrierePlan = 0L
        // Au-delà de 30 secondes hors de l'appli, la séance se referme.
        if (depuis != 0L && SystemClock.elapsedRealtime() - depuis > 30_000L) {
            if (ecran != Ecran.Relier && ecran != Ecran.ChoixProfil) ecran = Ecran.ChoixProfil
        }
        if (erreurDonnees != null && Compte.estRelie) demarrerEcoute()
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
    }
}
