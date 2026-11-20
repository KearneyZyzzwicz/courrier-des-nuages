package fr.courrierdesnuages.app.langues

import fr.courrierdesnuages.app.data.CARTES_ITALIE
import fr.courrierdesnuages.app.data.CARTES_MONDE
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.REVISIONS_ANGLAIS
import fr.courrierdesnuages.app.data.REVISIONS_ITALIEN
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.italien.Semaines
import java.time.LocalDate
import kotlin.random.Random

/**
 * Les langues étrangères. Elles marchent toutes de la même façon (des mots jour par jour, communs
 * aux enfants, et une carte postale pour une semaine sans faute) ; chacune a son messager et son album.
 */
enum class Langue(
    val matiere: String,
    /** « Italien », « Anglais ». */
    val nom: String,
    /** « d'italien », « d'anglais » : « les mots d'italien ». */
    val deLaLangue: String,
    /** « italien », « anglais » : « le mot italien ». */
    val adjectif: String,
    /** La collection des semaines de mots dans la base. */
    val collectionSemaines: String,
    /** La collection des mots à revoir de chaque enfant. */
    val collectionRevisions: String,
    /** La collection des cartes postales gagnées par chaque enfant. */
    val collectionCartes: String,
    /** Écrit sur la tuile de l'accueil. */
    val symbole: String,
    /** Les touches d'accents du clavier. */
    val accents: List<String>,
) {
    ITALIEN(Matiere.ITALIEN, "Italien", "d'italien", "italien", "italien", REVISIONS_ITALIEN, CARTES_ITALIE, "Ciao", listOf("à", "è", "é", "ì", "ò", "ù")),
    ANGLAIS(Matiere.ANGLAIS, "Anglais", "d'anglais", "anglais", "anglais", REVISIONS_ANGLAIS, CARTES_MONDE, "Hello", emptyList());

    companion object {
        fun depuisMatiere(matiere: String): Langue? = entries.firstOrNull { it.matiere == matiere }
    }
}

/** Comment l'enfant répond pour un mot, au choix du parent. */
enum class ModeReponse(val cle: String, val nom: String, val explication: String) {
    VIGNETTES("vignettes", "Vignettes", "choisir parmi 4 mots de la semaine"),
    TRADUCTION("traduction", "Écrire", "écrire le mot au clavier"),
    LETTRES("lettres", "Lettres", "remettre les lettres dans l'ordre");

    companion object {
        fun depuisCle(cle: String?): ModeReponse = entries.firstOrNull { it.cle == cle } ?: VIGNETTES
    }
}

/** Un mot de vocabulaire : le français affiché, la traduction à trouver, et la façon de la trouver. */
data class MotLangue(val francais: String, val traduction: String, val mode: ModeReponse = ModeReponse.VIGNETTES) {
    /** La forme enregistrée dans les séances : « le chat|il gatto ». */
    val code: String get() = "${Mots.normaliser(francais)}|${Mots.normaliser(traduction)}"

    val complet: Boolean get() = francais.isNotBlank() && traduction.isNotBlank()

    companion object {
        fun lire(code: String, mode: ModeReponse = ModeReponse.VIGNETTES): MotLangue? {
            val morceaux = code.split('|')
            return if (morceaux.size == 2) MotLangue(morceaux[0], morceaux[1], mode) else null
        }
    }
}

/** Au plus 10 mots par jour. */
const val MOTS_PAR_JOUR = 10

/**
 * Les mots d'une semaine, jour par jour (1 = lundi … 7 = dimanche), communs aux trois enfants.
 * [lundi] au format 2026-11-16.
 */
data class SemaineLangue(val lundi: String, val jours: Map<Int, List<MotLangue>>) {
    fun motsDuJour(numero: Int): List<MotLangue> = jours[numero].orEmpty().filter { it.complet }

    /** Tous les mots de la semaine, du lundi au dimanche, sans doublon. */
    val tous: List<MotLangue> get() = (1..7).flatMap { motsDuJour(it) }.distinctBy { it.code }

    /** La façon de répondre choisie pour un mot de la semaine. */
    fun mode(code: String): ModeReponse? = tous.firstOrNull { it.code == code }?.mode
}

object RecompenseLangue {

    /**
     * Les mots de la séance d'un jour : ceux du jour, ceux des jours d'avant dans la semaine qui n'ont
     * pas encore été travaillés (un soir sans séance n'est pas une faute : ses mots attendent), et ceux
     * qui n'ont pas été sus du premier coup, qui reviennent.
     */
    fun motsDuSoir(
        semaine: SemaineLangue?,
        jour: LocalDate,
        seancesDeLaSemaine: List<Seance>,
        aRevoir: List<MotLangue>,
    ): List<MotLangue> {
        val numero = Semaines.numeroDuJour(jour)
        val travailles = seancesDeLaSemaine.flatMap { s -> s.resultats.map { it.mot } }.toSet()
        val duJour = semaine?.motsDuJour(numero).orEmpty()
        val enAttente = if (semaine == null) {
            emptyList()
        } else {
            (1 until numero).flatMap { semaine.motsDuJour(it) }.filter { it.code !in travailles }
        }
        val vus = HashSet<String>()
        return (duJour + enAttente + aRevoir).filter { vus.add(it.code) }
    }

    /**
     * La carte postale : tous les mots de la semaine ont été travaillés, et chacun a été trouvé du
     * premier coup (1 point) la première fois qu'il a été demandé, quel que soit le jour.
     */
    fun semaineParfaite(semaine: SemaineLangue, seancesDeLaSemaine: List<Seance>): Boolean {
        val mots = semaine.tous.map { it.code }
        if (mots.isEmpty()) return false
        val premiereFois = HashMap<String, Double>()
        for (s in seancesDeLaSemaine.sortedBy { it.soir }) {
            for (r in s.resultats) premiereFois.putIfAbsent(r.mot, r.points)
        }
        return mots.all { premiereFois[it] == 1.0 }
    }

    /** Une carte au hasard parmi celles que l'enfant n'a pas encore. */
    fun <T> tirer(toutes: List<T>, id: (T) -> String, gagnees: List<CarteGagnee>, hasard: Random = Random.Default): T? {
        val dejaLa = gagnees.map { it.idCarte }.toSet()
        return toutes.filter { id(it) !in dejaLa }.randomOrNull(hasard)
    }
}
