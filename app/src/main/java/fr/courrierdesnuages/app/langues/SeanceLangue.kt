package fr.courrierdesnuages.app.langues

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.courrierdesnuages.app.data.EtatSeance
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.ResultatMot
import fr.courrierdesnuages.app.data.Seance
import kotlin.random.Random

/**
 * Les étapes d'un mot : le messager apporte le message, l'enfant répond selon le mode du mot
 * (vignettes, écriture au clavier ou lettres mélangées), et recopie le mot s'il ne l'a pas trouvé.
 */
enum class EtapeLangue { MESSAGER, CHOIX, SAISIE, LETTRES, CORRECTION, RECOPIE, BRAVO, FIN }

/**
 * Une séance de langue. Le mot français reste affiché ; pour chaque mot, le parent a choisi la façon
 * de répondre :
 * - vignettes : un seul essai parmi 4 mots de la semaine (1 point, sinon 0) ;
 * - écrire : deux essais au clavier (1 point), puis les lettres mélangées (0,5 point) ;
 * - lettres : deux essais avec les lettres mélangées (1 point).
 * Un mot pas trouvé compte 0 et l'enfant le recopie. Si [compte] est faux, c'est un entraînement.
 * [ordre] contient les mots sous la forme « le chat|il gatto » ; [motsDeLaSemaine], les traductions
 * parmi lesquelles sont tirées les autres vignettes.
 */
class SeanceLangue(
    val langue: Langue,
    val profil: Profil,
    val soir: String,
    val compte: Boolean,
    val ordre: List<String>,
    /** La façon de répondre de chaque mot, par son code (vignettes si rien n'est dit). */
    private val modes: Map<String, ModeReponse>,
    val motsRevision: List<String>,
    resultatsDeja: List<ResultatMot>,
    private val debutLe: Long,
    private val enregistrer: (SeanceLangue) -> Unit,
    private val terminer: (SeanceLangue) -> Unit,
    private val motsDeLaSemaine: List<String> = emptyList(),
    /** Les traductions de la semaine d'avant, s'il manque des vignettes. */
    private val motsDeSecours: List<String> = emptyList(),
    private val hasard: Random = Random.Default,
) {
    val resultats = mutableStateListOf<ResultatMot>().apply { addAll(resultatsDeja) }

    /** Vrai une fois la séance qui compte terminée (enregistrée, mots à revoir, compteurs, carte). */
    private var termine = false

    var rattrapage by mutableStateOf<List<String>>(emptyList())
        private set
    var enRattrapage by mutableStateOf(false)
        private set
    var index by mutableStateOf(0)
        private set
    var etape by mutableStateOf(EtapeLangue.MESSAGER)
        private set
    var saisie by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var dernierPoint by mutableStateOf(0.0)
        private set

    /** 1 ou 2 : l'essai en cours, au clavier ou avec les lettres. */
    var essai by mutableStateOf(1)
        private set

    /** Les vignettes du mot en cours, dans l'ordre où elles sont montrées. */
    var vignettes by mutableStateOf<List<String>>(emptyList())
        private set

    /** La vignette que l'enfant a touchée. */
    var choisie by mutableStateOf<String?>(null)
        private set

    /** Les lettres mélangées, et celles déjà posées (par leur place dans le tirage). */
    var tirage by mutableStateOf<List<Char>>(emptyList())
        private set
    val posees = mutableStateListOf<Int>()

    /** Change à chaque nouveau mot : le messager revient. */
    var passage by mutableStateOf(0)
        private set

    /** L'identifiant de la carte postale gagnée à la fin de cette séance (semaine sans faute). */
    var carteGagnee by mutableStateOf<String?>(null)

    init {
        index = resultats.size
        if (index >= ordre.size) finirPartiePrincipale()
    }

    val code: String
        get() = if (enRattrapage) rattrapage.getOrElse(index) { "" } else ordre.getOrElse(index) { "" }

    fun modeDe(code: String): ModeReponse = modes[code] ?: ModeReponse.VIGNETTES

    val mode: ModeReponse get() = modeDe(code)

    val mot: MotLangue get() = MotLangue.lire(code, mode) ?: MotLangue("", "")

    val numero: Int get() = index + 1
    val total: Int get() = if (enRattrapage) rattrapage.size else ordre.size
    val points: Double get() = resultats.sumOf { it.points }
    val fautes: Int get() = resultats.count { it.points < 1.0 }

    /** Vrai si [vignette] est la bonne traduction du mot en cours. */
    fun estLaBonne(vignette: String): Boolean = Mots.identiques(vignette, mot.traduction)

    /** Les lettres rapportent 1 point en mode « lettres », 0,5 après deux essais au clavier. */
    val lettresDuPremierCoup: Boolean get() = mode == ModeReponse.LETTRES

    /** L'enfant a déroulé le message : il répond selon le mode du mot. */
    fun deroule() {
        if (etape != EtapeLangue.MESSAGER) return
        message = null
        choisie = null
        essai = 1
        saisie = ""
        when (mode) {
            ModeReponse.VIGNETTES -> {
                val autres = motsDeSecours + ordre.mapNotNull { MotLangue.lire(it)?.traduction }
                vignettes = tirerVignettes(mot.traduction, motsDeLaSemaine, autres, hasard)
                etape = EtapeLangue.CHOIX
            }
            ModeReponse.TRADUCTION -> etape = EtapeLangue.SAISIE
            ModeReponse.LETTRES -> preparerLettres()
        }
    }

    // ---------- Vignettes : un seul essai ----------

    fun choisir(vignette: String) {
        if (etape != EtapeLangue.CHOIX) return
        choisie = vignette
        if (estLaBonne(vignette)) {
            noter(1.0)
            bravo(1.0)
        } else {
            noter(0.0)
            etape = EtapeLangue.CORRECTION
            message = "Ce n'est pas ça : le bon mot est en vert."
        }
    }

    /** Après avoir vu la bonne vignette, l'enfant recopie le mot. */
    fun allerALaRecopie() {
        if (etape == EtapeLangue.CORRECTION) passerALaRecopie()
    }

    // ---------- Au clavier : deux essais, puis les lettres ----------

    fun taper(texte: String) {
        if ((etape == EtapeLangue.SAISIE || etape == EtapeLangue.RECOPIE) && saisie.length < 40) saisie += texte
    }

    fun effacer() {
        if (etape == EtapeLangue.SAISIE || etape == EtapeLangue.RECOPIE) saisie = saisie.dropLast(1)
    }

    fun valider() {
        if (saisie.isBlank()) return
        when (etape) {
            EtapeLangue.SAISIE -> when {
                Mots.identiques(saisie, mot.traduction) -> {
                    noter(1.0)
                    bravo(1.0)
                }
                essai == 1 -> {
                    // Deuxième essai, sans que ce soit une faute : l'enfant relit et corrige.
                    essai = 2
                    message = "Presque ! Relis ton mot et corrige-le."
                }
                enRattrapage -> passerALaRecopie()
                else -> {
                    preparerLettres()
                    message = "Remets les lettres dans l'ordre."
                }
            }
            EtapeLangue.RECOPIE -> {
                if (Mots.identiques(saisie, mot.traduction)) suivant() else message = "Pas encore : regarde bien chaque lettre."
            }
            else -> Unit
        }
    }

    // ---------- Les lettres mélangées ----------

    private fun preparerLettres() {
        tirage = Mots.melanger(mot.traduction, hasard)
        posees.clear()
        etape = EtapeLangue.LETTRES
    }

    fun poserTuile(position: Int) {
        if (etape != EtapeLangue.LETTRES || position in posees || posees.size >= tirage.size) return
        posees += position
    }

    fun retirerTuile() {
        if (etape == EtapeLangue.LETTRES && posees.isNotEmpty()) posees.removeAt(posees.lastIndex)
    }

    /** Le mot reconstruit avec les tuiles, avec les espaces du vrai mot remis à leur place. */
    fun motReconstruit(): String {
        val lettres = posees.map { tirage[it] }
        val sortie = StringBuilder()
        var i = 0
        for (c in Mots.normaliser(mot.traduction)) {
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
        if (etape != EtapeLangue.LETTRES || posees.size < tirage.size) return
        val juste = Mots.identiques(motReconstruit(), mot.traduction)
        when {
            juste && lettresDuPremierCoup -> {
                noter(1.0)
                bravo(1.0)
            }
            juste -> {
                noter(0.5)
                bravo(0.5)
            }
            lettresDuPremierCoup && essai == 1 -> {
                essai = 2
                posees.clear()
                message = "Presque ! Essaie encore une fois."
            }
            else -> {
                noter(0.0)
                passerALaRecopie()
            }
        }
    }

    // ---------- Enchaînement ----------

    private fun bravo(points: Double) {
        dernierPoint = points
        etape = EtapeLangue.BRAVO
        message = null
    }

    private fun passerALaRecopie() {
        etape = EtapeLangue.RECOPIE
        saisie = ""
        message = "Regarde bien le mot ${langue.adjectif} et recopie-le."
    }

    fun apresBravo() {
        if (etape == EtapeLangue.BRAVO) suivant()
    }

    private fun noter(points: Double) {
        if (enRattrapage || resultats.size > index) return
        resultats += ResultatMot(mot = code, points = points, revision = code in motsRevision)
        if (compte) enregistrer(this)
        // Le dernier résultat est noté : la séance qui compte est finie tout de suite.
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
        essai = 1
        tirage = emptyList()
        posees.clear()
        index += 1
        if (!enRattrapage && index >= ordre.size) {
            finirPartiePrincipale()
        } else if (enRattrapage && index >= rattrapage.size) {
            etape = EtapeLangue.FIN
        } else {
            etape = EtapeLangue.MESSAGER
            passage += 1
        }
    }

    private fun finirPartiePrincipale() {
        if (compte) terminerUneFois()
        rattrapage = resultats.filter { it.points < 1.0 }.map { it.mot }
        index = 0
        if (rattrapage.isEmpty()) {
            etape = EtapeLangue.FIN
        } else {
            enRattrapage = true
            etape = EtapeLangue.MESSAGER
            passage += 1
        }
    }

    /** Quitter l'appli pendant un mot : il compte 0 et l'enfant le recopiera à son retour. */
    fun quitteLAppli() {
        if (!compte || enRattrapage) return
        when (etape) {
            EtapeLangue.MESSAGER, EtapeLangue.CHOIX, EtapeLangue.SAISIE, EtapeLangue.LETTRES -> {
                noter(0.0)
                passerALaRecopie()
            }
            EtapeLangue.CORRECTION -> passerALaRecopie()
            else -> Unit
        }
    }

    fun versSeance(): Seance = Seance(
        soir = soir,
        matiere = langue.matiere,
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
 * Les vignettes d'un mot : la [bonne] traduction et jusqu'à 3 autres mots différents, pris d'abord
 * parmi les mots de la [semaine], puis, s'il en manque, parmi les [autres] mots. Le tout dans le désordre.
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
