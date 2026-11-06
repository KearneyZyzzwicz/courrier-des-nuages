package fr.courrierdesnuages.app.italien

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.courrierdesnuages.app.data.EtatSeance
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Seance
import kotlin.random.Random

/**
 * Les étapes d'un mot : le pigeon apporte le tube, l'enfant touche une des 4 vignettes, et s'il se
 * trompe, il voit la bonne puis la recopie.
 */
enum class EtapeItalien { PIGEON, CHOIX, CORRECTION, RECOPIE, BRAVO, FIN }

/**
 * Le pigeon voyageur. Le mot français reste affiché, l'enfant choisit sa traduction parmi 4
 * vignettes : la bonne et 3 autres mots italiens de la semaine, dans le désordre. Un seul essai :
 * 1 point si c'est la bonne, sinon 0 et il recopie le mot. Si [compte] est faux, c'est un
 * entraînement. [ordre] contient les mots sous la forme « le chat|il gatto » ; [motsDeLaSemaine],
 * les mots italiens de la semaine, parmi lesquels sont tirées les autres vignettes.
 */
class SeanceItalien(
    val profil: Profil,
    val soir: String,
    val compte: Boolean,
    val ordre: List<String>,
    val motsRevision: List<String>,
    resultatsDeja: List<ResultatMot>,
    private val debutLe: Long,
    private val enregistrer: (SeanceItalien) -> Unit,
    private val terminer: (SeanceItalien) -> Unit,
    private val motsDeLaSemaine: List<String> = emptyList(),
    private val hasard: Random = Random.Default,
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
    var etape by mutableStateOf(EtapeItalien.PIGEON)
        private set
    var saisie by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var dernierPoint by mutableStateOf(0.0)
        private set

    /** Les vignettes du mot en cours, dans l'ordre où elles sont montrées. */
    var vignettes by mutableStateOf<List<String>>(emptyList())
        private set

    /** La vignette que l'enfant a touchée. */
    var choisie by mutableStateOf<String?>(null)
        private set

    /** Change à chaque nouveau mot : le pigeon revient. */
    var passage by mutableStateOf(0)
        private set

    /** La carte postale gagnée à la fin de cette séance (semaine sans faute), s'il y en a une. */
    var carteGagnee by mutableStateOf<CartePostale?>(null)

    init {
        index = resultats.size
        if (index >= ordre.size) finirPartiePrincipale()
    }

    val code: String
        get() = if (enRattrapage) rattrapage.getOrElse(index) { "" } else ordre.getOrElse(index) { "" }

    val mot: MotItalien get() = MotItalien.lire(code) ?: MotItalien("", "")

    val numero: Int get() = index + 1
    val total: Int get() = if (enRattrapage) rattrapage.size else ordre.size
    val points: Double get() = resultats.sumOf { it.points }
    val fautes: Int get() = resultats.count { it.points < 1.0 }

    /** Vrai si [vignette] est la bonne traduction du mot en cours. */
    fun estLaBonne(vignette: String): Boolean = Mots.identiques(vignette, mot.italien)

    /** L'enfant a déroulé le message du pigeon : les vignettes apparaissent. */
    fun deroule() {
        if (etape != EtapeItalien.PIGEON) return
        val autres = ordre.mapNotNull { MotItalien.lire(it)?.italien }
        vignettes = tirerVignettes(mot.italien, motsDeLaSemaine, autres, hasard)
        choisie = null
        etape = EtapeItalien.CHOIX
        message = null
    }

    /** Un seul essai : la bonne vignette rapporte 1 point, une autre 0. */
    fun choisir(vignette: String) {
        if (etape != EtapeItalien.CHOIX) return
        choisie = vignette
        if (estLaBonne(vignette)) {
            noter(1.0)
            dernierPoint = 1.0
            etape = EtapeItalien.BRAVO
            message = null
        } else {
            noter(0.0)
            etape = EtapeItalien.CORRECTION
            message = "Ce n'est pas ça : le bon mot est en vert."
        }
    }

    /** Après avoir vu la bonne vignette, l'enfant recopie le mot. */
    fun allerALaRecopie() {
        if (etape == EtapeItalien.CORRECTION) passerALaRecopie()
    }

    fun taper(texte: String) {
        if (etape == EtapeItalien.RECOPIE && saisie.length < 40) saisie += texte
    }

    fun effacer() {
        if (etape == EtapeItalien.RECOPIE) saisie = saisie.dropLast(1)
    }

    fun valider() {
        if (etape != EtapeItalien.RECOPIE || saisie.isBlank()) return
        if (Mots.identiques(saisie, mot.italien)) suivant() else message = "Pas encore : regarde bien chaque lettre."
    }

    private fun passerALaRecopie() {
        etape = EtapeItalien.RECOPIE
        saisie = ""
        message = "Regarde bien le mot italien et recopie-le."
    }

    fun apresBravo() {
        if (etape == EtapeItalien.BRAVO) suivant()
    }

    private fun noter(points: Double) {
        if (enRattrapage || resultats.size > index) return
        resultats += ResultatMot(mot = code, points = points, revision = code in motsRevision)
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
        vignettes = emptyList()
        choisie = null
        index += 1
        if (!enRattrapage && index >= ordre.size) {
            finirPartiePrincipale()
        } else if (enRattrapage && index >= rattrapage.size) {
            etape = EtapeItalien.FIN
        } else {
            etape = EtapeItalien.PIGEON
            passage += 1
        }
    }

    private fun finirPartiePrincipale() {
        if (compte) terminerUneFois()
        rattrapage = resultats.filter { it.points < 1.0 }.map { it.mot }
        index = 0
        if (rattrapage.isEmpty()) {
            etape = EtapeItalien.FIN
        } else {
            enRattrapage = true
            etape = EtapeItalien.PIGEON
            passage += 1
        }
    }

    /** Quitter l'appli pendant un mot : il compte 0 et l'enfant le recopiera à son retour. */
    fun quitteLAppli() {
        if (!compte || enRattrapage) return
        when (etape) {
            EtapeItalien.PIGEON, EtapeItalien.CHOIX -> {
                noter(0.0)
                passerALaRecopie()
            }
            EtapeItalien.CORRECTION -> passerALaRecopie()
            else -> Unit
        }
    }

    fun versSeance(): Seance = Seance(
        soir = soir,
        matiere = Matiere.ITALIEN,
        etat = if (resultats.size >= ordre.size) EtatSeance.TERMINEE else EtatSeance.EN_COURS,
        ordre = ordre,
        revisions = motsRevision,
        resultats = resultats.toList(),
        debutLe = debutLe,
        finLe = if (resultats.size >= ordre.size) System.currentTimeMillis() else 0L,
    )
}

/** Nombre de vignettes proposées pour chaque mot. */
const val NOMBRE_DE_VIGNETTES = 4

/**
 * Les vignettes d'un mot : la [bonne] traduction et jusqu'à 3 autres mots italiens différents, pris
 * d'abord parmi les mots de la [semaine], puis, s'il en manque, parmi les [autres] mots de la séance.
 * Le tout dans le désordre.
 */
fun tirerVignettes(bonne: String, semaine: List<String>, autres: List<String>, hasard: Random = Random.Default): List<String> {
    val choisies = mutableListOf(Mots.normaliser(bonne))
    for (candidats in listOf(semaine, autres)) {
        for (x in candidats.shuffled(hasard)) {
            if (choisies.size >= NOMBRE_DE_VIGNETTES) break
            if (x.isNotBlank() && choisies.none { Mots.identiques(it, x) }) choisies += Mots.normaliser(x)
        }
    }
    return choisies.shuffled(hasard)
}
