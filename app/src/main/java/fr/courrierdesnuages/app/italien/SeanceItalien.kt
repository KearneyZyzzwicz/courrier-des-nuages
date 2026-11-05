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

/** Les étapes d'un mot : le pigeon apporte le tube, l'enfant écrit, seconde chance, recopie. */
enum class EtapeItalien { PIGEON, SAISIE, SECONDE_CHANCE, RECOPIE, BRAVO, FIN }

/**
 * Le pigeon voyageur. Le mot français reste affiché, l'enfant écrit sa traduction en italien.
 * Si [compte] est vrai, c'est la séance qui compte (1, 0,5 ou 0 point par mot) ; sinon, entraînement.
 * [ordre] contient les mots sous la forme « le chat|il gatto ».
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
) {
    val resultats = mutableStateListOf<ResultatMot>().apply { addAll(resultatsDeja) }

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
    var tirage by mutableStateOf<List<Char>>(emptyList())
        private set
    val posees = mutableStateListOf<Int>()
    var dernierPoint by mutableStateOf(0.0)
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

    /** L'enfant a déroulé le message du pigeon : il peut écrire. */
    fun deroule() {
        if (etape == EtapeItalien.PIGEON) {
            etape = EtapeItalien.SAISIE
            message = null
        }
    }

    fun taper(texte: String) {
        if ((etape == EtapeItalien.SAISIE || etape == EtapeItalien.RECOPIE) && saisie.length < 40) saisie += texte
    }

    fun effacer() {
        if (etape == EtapeItalien.SAISIE || etape == EtapeItalien.RECOPIE) saisie = saisie.dropLast(1)
    }

    fun valider() {
        if (saisie.isBlank()) return
        when (etape) {
            EtapeItalien.SAISIE -> {
                if (Mots.identiques(saisie, mot.italien)) {
                    noter(1.0)
                    bravo(1.0)
                } else if (enRattrapage) {
                    passerALaRecopie()
                } else {
                    etape = EtapeItalien.SECONDE_CHANCE
                    tirage = Mots.melanger(mot.italien)
                    posees.clear()
                    message = "Presque ! Remets les lettres dans l'ordre."
                }
            }
            EtapeItalien.RECOPIE -> {
                if (Mots.identiques(saisie, mot.italien)) suivant() else message = "Pas encore : regarde bien chaque lettre."
            }
            else -> Unit
        }
    }

    fun poserTuile(position: Int) {
        if (etape != EtapeItalien.SECONDE_CHANCE || position in posees || posees.size >= tirage.size) return
        posees += position
    }

    fun retirerTuile() {
        if (etape == EtapeItalien.SECONDE_CHANCE && posees.isNotEmpty()) posees.removeAt(posees.lastIndex)
    }

    /** Le mot reconstruit avec les tuiles, avec les espaces du vrai mot remis à leur place. */
    fun motReconstruit(): String {
        val lettres = posees.map { tirage[it] }
        val sortie = StringBuilder()
        var i = 0
        for (c in Mots.normaliser(mot.italien)) {
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
        if (etape != EtapeItalien.SECONDE_CHANCE || posees.size < tirage.size) return
        if (Mots.identiques(motReconstruit(), mot.italien)) {
            noter(0.5)
            bravo(0.5)
        } else {
            noter(0.0)
            passerALaRecopie()
        }
    }

    private fun bravo(points: Double) {
        dernierPoint = points
        etape = EtapeItalien.BRAVO
        message = null
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
            etape = EtapeItalien.FIN
        } else {
            etape = EtapeItalien.PIGEON
            passage += 1
        }
    }

    private fun finirPartiePrincipale() {
        if (compte) terminer(this)
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
        if (etape == EtapeItalien.PIGEON || etape == EtapeItalien.SAISIE || etape == EtapeItalien.SECONDE_CHANCE) {
            noter(0.0)
            passerALaRecopie()
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
