package fr.courrierdesnuages.app.calcul

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.courrierdesnuages.app.data.EtatSeance
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Seance

/** Les étapes d'un calcul : le colis avance, seconde chance aux 4 casiers, recopie. */
enum class EtapeCalcul { TAPIS, CASIERS, RECOPIE, BRAVO, FIN }

const val CHRONO_MS = 10_000

/**
 * Le tri des colis. Si [compte] est vrai, c'est la séance qui compte : 1, 0,5 ou 0 point par calcul,
 * progression enregistrée après chaque calcul. Sinon, entraînement sans points.
 */
class SeanceCalcul(
    val profil: Profil,
    val soir: String,
    val compte: Boolean,
    val ordre: List<String>,
    val calculsRevision: List<String>,
    resultatsDeja: List<ResultatMot>,
    private val debutLe: Long,
    private val enregistrer: (SeanceCalcul) -> Unit,
    private val terminer: (SeanceCalcul) -> Unit,
) {
    val resultats = mutableStateListOf<ResultatMot>().apply { addAll(resultatsDeja) }

    /** Vrai une fois la séance qui compte terminée (enregistrée, mots à revoir, compteurs). */
    private var termine = false

    var rattrapage by mutableStateOf<List<String>>(emptyList())
        private set
    var enRattrapage by mutableStateOf(false)
        private set
    var index by mutableStateOf(0)
        private set
    var etape by mutableStateOf(EtapeCalcul.TAPIS)
        private set
    var saisie by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var propositions by mutableStateOf<List<Int>>(emptyList())
        private set
    var dernierPoint by mutableStateOf(0.0)
        private set

    /** Change à chaque nouveau colis : relance le tapis roulant. */
    var passage by mutableStateOf(0)
        private set

    init {
        index = resultats.size
        if (index >= ordre.size) finirPartiePrincipale()
    }

    val texte: String
        get() = if (enRattrapage) rattrapage.getOrElse(index) { "" } else ordre.getOrElse(index) { "" }

    val calcul: Calcul get() = Calcul.lire(texte) ?: Calcul(0, 0, PLUS)

    val numero: Int get() = index + 1
    val total: Int get() = if (enRattrapage) rattrapage.size else ordre.size
    val points: Double get() = resultats.sumOf { it.points }
    val fautes: Int get() = resultats.count { it.points < 1.0 }
    val medaille: Medaille get() = if (compte) Medaille.pour(fautes) else Medaille.AUCUNE

    fun taper(chiffre: Char) {
        if ((etape == EtapeCalcul.TAPIS || etape == EtapeCalcul.RECOPIE) && saisie.length < 3) {
            saisie += chiffre
            message = null
        }
    }

    fun effacer() {
        if (etape == EtapeCalcul.TAPIS || etape == EtapeCalcul.RECOPIE) saisie = saisie.dropLast(1)
    }

    fun valider() {
        if (saisie.isEmpty()) return
        val reponse = saisie.toIntOrNull()
        when (etape) {
            EtapeCalcul.TAPIS -> if (reponse == calcul.resultat) {
                noter(1.0)
                bravo(1.0)
            } else {
                erreur()
            }
            EtapeCalcul.RECOPIE -> if (reponse == calcul.resultat) {
                suivant()
            } else {
                saisie = ""
                message = "Regarde bien le numéro du casier."
            }
            else -> Unit
        }
    }

    /** Le colis est arrivé au bout du tapis (ou le temps des casiers est écoulé). */
    fun tempsEcoule() {
        when (etape) {
            EtapeCalcul.TAPIS -> erreur()
            EtapeCalcul.CASIERS -> {
                noter(0.0)
                passerALaRecopie()
            }
            else -> Unit
        }
    }

    /** Seconde chance : l'enfant touche un des 4 casiers. */
    fun choisir(n: Int) {
        if (etape != EtapeCalcul.CASIERS) return
        if (n == calcul.resultat) {
            noter(0.5)
            bravo(0.5)
        } else {
            noter(0.0)
            passerALaRecopie()
        }
    }

    private fun erreur() {
        if (enRattrapage) {
            passerALaRecopie()
        } else {
            etape = EtapeCalcul.CASIERS
            saisie = ""
            propositions = Calcul.propositions(calcul)
            message = "Seconde chance : touche le bon casier !"
        }
    }

    private fun bravo(points: Double) {
        dernierPoint = points
        etape = EtapeCalcul.BRAVO
        message = null
    }

    private fun passerALaRecopie() {
        etape = EtapeCalcul.RECOPIE
        saisie = ""
        message = "Le colis va dans le casier ${calcul.resultat}. Tape ce numéro."
    }

    fun apresBravo() {
        if (etape == EtapeCalcul.BRAVO) suivant()
    }

    private fun noter(points: Double) {
        if (enRattrapage || resultats.size > index) return
        val cle = calcul.canonique.texte
        resultats += ResultatMot(mot = texte, points = points, revision = cle in calculsRevision)
        if (compte) enregistrer(this)
        // Le dernier résultat est noté : la séance qui compte est finie tout de suite, même si
        // l'enfant quitte pendant la recopie ou le tampon du dernier mot.
        if (compte && resultats.size >= ordre.size) terminerUneFois()
    }

    private fun terminerUneFois() {
        if (termine) return
        termine = true
        terminer(this)
    }

    private fun suivant() {
        saisie = ""
        message = null
        propositions = emptyList()
        index += 1
        if (!enRattrapage && index >= ordre.size) {
            finirPartiePrincipale()
        } else if (enRattrapage && index >= rattrapage.size) {
            etape = EtapeCalcul.FIN
        } else {
            etape = EtapeCalcul.TAPIS
            passage += 1
        }
    }

    private fun finirPartiePrincipale() {
        if (compte) terminerUneFois()
        rattrapage = resultats.filter { it.points < 1.0 }.map { it.mot }
        index = 0
        if (rattrapage.isEmpty()) {
            etape = EtapeCalcul.FIN
        } else {
            enRattrapage = true
            etape = EtapeCalcul.TAPIS
            passage += 1
        }
    }

    /** Quitter l'appli pendant un calcul : il compte 0 et l'enfant le recopiera à son retour. */
    fun quitteLAppli() {
        if (!compte || enRattrapage) return
        if (etape == EtapeCalcul.TAPIS || etape == EtapeCalcul.CASIERS) {
            noter(0.0)
            passerALaRecopie()
        }
    }

    fun versSeance(): Seance = Seance(
        soir = soir,
        matiere = Matiere.MATHS,
        etat = if (resultats.size >= ordre.size) EtatSeance.TERMINEE else EtatSeance.EN_COURS,
        ordre = ordre,
        revisions = calculsRevision,
        resultats = resultats.toList(),
        debutLe = debutLe,
        finLe = if (resultats.size >= ordre.size) System.currentTimeMillis() else 0L,
    )
}
