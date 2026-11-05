package fr.courrierdesnuages.app.dictee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.courrierdesnuages.app.data.EtatSeance
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Seance

/** Les étapes d'un mot : l'avion passe, l'enfant écrit, seconde chance, recopie. */
enum class Phase { AVION, SAISIE, SECONDE_CHANCE, RECOPIE, BRAVO, FIN }

/**
 * Le déroulé d'une dictée. Si [compte] est vrai, c'est la séance qui compte : chaque mot rapporte
 * 1, 0,5 ou 0 point et la progression est enregistrée après chaque mot. Sinon c'est un
 * entraînement, sans points.
 */
class SeanceDictee(
    val profil: Profil,
    val soir: String,
    val compte: Boolean,
    val ordre: List<String>,
    val motsRevision: List<String>,
    resultatsDeja: List<ResultatMot>,
    private val debutLe: Long,
    private val enregistrer: (SeanceDictee) -> Unit,
    private val terminer: (SeanceDictee) -> Unit,
) {
    val resultats = mutableStateListOf<ResultatMot>().apply { addAll(resultatsDeja) }

    /** Les mots repris en fin de séance, sans points. */
    var rattrapage by mutableStateOf<List<String>>(emptyList())
        private set
    var enRattrapage by mutableStateOf(false)
        private set
    var index by mutableStateOf(0)
        private set
    var phase by mutableStateOf(Phase.AVION)
        private set
    var saisie by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** Seconde chance : les lettres posées et celles qui restent (par leur position dans le tirage). */
    var tirage by mutableStateOf<List<Char>>(emptyList())
        private set
    val posees = mutableStateListOf<Int>()

    /** Vrai quand le dernier mot a été réussi au premier essai (pour l'affichage du tampon). */
    var dernierPoint by mutableStateOf(0.0)
        private set

    /** Numéro du passage de l'avion : change à chaque nouveau mot, même identique. */
    var passage by mutableStateOf(0)
        private set

    init {
        index = resultats.size
        if (index >= ordre.size) finirPartiePrincipale() else phase = Phase.AVION
    }

    val mot: String
        get() = if (enRattrapage) rattrapage.getOrElse(index) { "" } else ordre.getOrElse(index) { "" }

    val numero: Int get() = index + 1
    val total: Int get() = if (enRattrapage) rattrapage.size else ordre.size
    val points: Double get() = resultats.sumOf { it.points }
    val fautes: Int get() = resultats.count { it.points < 1.0 }
    val medaille: Medaille get() = if (compte) Medaille.pour(fautes) else Medaille.AUCUNE

    /** L'avion est sorti de l'écran : l'enfant peut écrire. */
    fun avionPasse() {
        if (phase == Phase.AVION) {
            phase = Phase.SAISIE
            message = null
        }
    }

    fun taper(texte: String) {
        if (phase == Phase.SAISIE || phase == Phase.RECOPIE) {
            if (saisie.length < 40) saisie += texte
        }
    }

    fun effacer() {
        if (phase == Phase.SAISIE || phase == Phase.RECOPIE) saisie = saisie.dropLast(1)
    }

    /** Touche « Valider » du clavier. */
    fun valider() {
        if (saisie.isBlank()) return
        when (phase) {
            Phase.SAISIE -> {
                if (Mots.identiques(saisie, mot)) {
                    noter(1.0)
                    bravo(1.0)
                } else if (enRattrapage) {
                    passerALaRecopie()
                } else {
                    phase = Phase.SECONDE_CHANCE
                    tirage = Mots.melanger(mot)
                    posees.clear()
                    message = "Presque ! Remets les lettres dans l'ordre."
                }
            }
            Phase.RECOPIE -> {
                if (Mots.identiques(saisie, mot)) {
                    suivant()
                } else {
                    message = "Pas encore : regarde bien chaque lettre."
                }
            }
            else -> Unit
        }
    }

    // ---------- Seconde chance ----------

    /** Les cases à remplir : null pour un espace du mot, sinon la place de la lettre. */
    val nombreDeLettres: Int get() = tirage.size

    fun poserTuile(position: Int) {
        if (phase != Phase.SECONDE_CHANCE || position in posees || posees.size >= tirage.size) return
        posees += position
    }

    fun retirerTuile() {
        if (phase == Phase.SECONDE_CHANCE && posees.isNotEmpty()) posees.removeAt(posees.lastIndex)
    }

    /** Le mot reconstruit avec les tuiles, avec les espaces du vrai mot remis à leur place. */
    fun motReconstruit(): String {
        val lettres = posees.map { tirage[it] }
        val modele = Mots.normaliser(mot)
        val sortie = StringBuilder()
        var i = 0
        for (c in modele) {
            if (c == ' ') {
                sortie.append(' ')
            } else if (i < lettres.size) {
                sortie.append(lettres[i])
                i++
            }
        }
        return sortie.toString()
    }

    fun validerTuiles() {
        if (phase != Phase.SECONDE_CHANCE || posees.size < tirage.size) return
        if (Mots.identiques(motReconstruit(), mot)) {
            noter(0.5)
            bravo(0.5)
        } else {
            noter(0.0)
            passerALaRecopie()
        }
    }

    // ---------- Enchaînement ----------

    private fun bravo(points: Double) {
        dernierPoint = points
        phase = Phase.BRAVO
        message = null
    }

    private fun passerALaRecopie() {
        phase = Phase.RECOPIE
        saisie = ""
        message = "Regarde bien le mot et recopie-le."
    }

    /** Appelé après l'animation du tampon. */
    fun apresBravo() {
        if (phase == Phase.BRAVO) suivant()
    }

    private fun noter(points: Double) {
        if (enRattrapage || resultats.size > index) return
        resultats += ResultatMot(mot = mot, points = points, revision = motsRevision.any { Mots.identiques(it, mot) })
        if (compte) enregistrer(this)
    }

    private fun suivant() {
        saisie = ""
        message = null
        posees.clear()
        tirage = emptyList()
        index += 1
        if (!enRattrapage && index >= ordre.size) {
            finirPartiePrincipale()
        } else if (enRattrapage && index >= rattrapage.size) {
            phase = Phase.FIN
        } else {
            phase = Phase.AVION
            passage += 1
        }
    }

    private fun finirPartiePrincipale() {
        if (compte) terminer(this)
        rattrapage = resultats.filter { it.points < 1.0 }.map { it.mot }
        index = 0
        if (rattrapage.isEmpty()) {
            phase = Phase.FIN
        } else {
            enRattrapage = true
            phase = Phase.AVION
            passage += 1
        }
    }

    /**
     * L'enfant quitte l'appli pendant un mot : ce mot compte 0. À son retour, il le recopie.
     * Ne fait rien en entraînement, en rattrapage ou si le mot est déjà noté.
     */
    fun quitteLAppli() {
        if (!compte || enRattrapage) return
        if (phase == Phase.AVION || phase == Phase.SAISIE || phase == Phase.SECONDE_CHANCE) {
            noter(0.0)
            passerALaRecopie()
        }
    }

    /** L'état à enregistrer dans la base. */
    fun versSeance(): Seance = Seance(
        soir = soir,
        matiere = Matiere.FRANCAIS,
        etat = if (resultats.size >= ordre.size) EtatSeance.TERMINEE else EtatSeance.EN_COURS,
        ordre = ordre,
        revisions = motsRevision,
        resultats = resultats.toList(),
        debutLe = debutLe,
        finLe = if (resultats.size >= ordre.size) System.currentTimeMillis() else 0L,
    )
}
