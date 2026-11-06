package fr.courrierdesnuages.app.italien

import androidx.annotation.DrawableRes
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Seance
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** Un mot de vocabulaire : le français affiché, l'italien à retrouver parmi les vignettes. */
data class MotItalien(val francais: String, val italien: String) {
    /** Forme enregistrée dans les séances : « le chat|il gatto ». */
    val code: String get() = "${Mots.normaliser(francais)}|${Mots.normaliser(italien)}"

    val complet: Boolean get() = francais.isNotBlank() && italien.isNotBlank()

    companion object {
        fun lire(code: String): MotItalien? {
            val morceaux = code.split('|')
            return if (morceaux.size == 2) MotItalien(morceaux[0], morceaux[1]) else null
        }
    }
}

/** Les 5 mots de la semaine (du lundi au vendredi), communs aux trois enfants. [lundi] au format 2026-11-09. */
data class SemaineItalien(val lundi: String, val mots: List<MotItalien>) {
    /** Le mot d'un jour : 1 = lundi … 5 = vendredi. */
    fun motDuJour(jour: Int): MotItalien? = mots.getOrNull(jour - 1)?.takeIf { it.complet }
}

/** Une carte postale : recto illustré, verso écrit par le pigeon voyageur. */
data class CartePostale(
    val id: String,
    val ville: String,
    val region: String,
    val paragraphes: List<String>,
    val motFrancais: String,
    val motItalien: String,
    val legende: String,
    @param:DrawableRes val recto: Int = 0,
    @param:DrawableRes val timbre: Int = 0,
    /** Pour une carte créée par le parent : la photo, en JPEG encodé en base64. */
    val image: String? = null,
) {
    val creeeParLeParent: Boolean get() = recto == 0
}

/** Une carte gagnée par un enfant. */
data class CarteGagnee(
    val idCarte: String,
    val semaine: String,
    val gagneeLe: Long,
    val epinglee: Boolean,
    val bienPlacee: Boolean,
)

object Semaines {
    /** Le lundi de la semaine d'un jour. */
    fun lundi(jour: LocalDate): LocalDate = jour.with(DayOfWeek.MONDAY)

    /** 1 = lundi … 7 = dimanche. */
    fun numeroDuJour(jour: LocalDate): Int = jour.dayOfWeek.value

    /** La semaine à préparer par le parent : celle en cours, ou la suivante le samedi et le dimanche. */
    fun semaineAPreparer(aujourdhui: LocalDate): LocalDate =
        if (numeroDuJour(aujourdhui) >= 6) lundi(aujourdhui).plusWeeks(1) else lundi(aujourdhui)
}

object RecompenseItalien {

    /** Les mots de la semaine déjà travaillés (présents dans une séance de la semaine), par leur code. */
    private fun dejaTravailles(seancesDeLaSemaine: List<Seance>): Set<String> =
        seancesDeLaSemaine.flatMap { s -> s.resultats.map { it.mot } }.toSet()

    /**
     * Les mots de la séance : le mot du jour (du lundi au vendredi), les mots de la semaine pas encore
     * travaillés (un soir sans séance ne compte pas comme une faute : son mot attend), et les mots pas
     * sus du premier coup, qui reviennent. Le week-end, il n'y a pas de nouveau mot, mais on peut
     * rattraper ceux de la semaine et revoir ceux qui n'étaient pas sus.
     */
    fun motsDuSoir(
        semaine: SemaineItalien?,
        jour: LocalDate,
        seancesDeLaSemaine: List<Seance>,
        aRevoir: List<MotItalien>,
    ): List<MotItalien> {
        val numero = Semaines.numeroDuJour(jour)
        val travailles = dejaTravailles(seancesDeLaSemaine)
        val duJour = if (semaine != null && numero <= 5) semaine.motDuJour(numero) else null
        val enAttente = if (semaine == null) {
            emptyList()
        } else {
            (1 until minOf(numero, 6)).mapNotNull { n -> semaine.motDuJour(n) }.filter { it.code !in travailles }
        }
        val tous = listOfNotNull(duJour) + enAttente + aRevoir
        val vus = HashSet<String>()
        return tous.filter { vus.add(it.code) }
    }

    /** Le mot du jour s'il y en a un (du lundi au vendredi), sinon null. */
    fun motDuJour(semaine: SemaineItalien?, jour: LocalDate): MotItalien? {
        val numero = Semaines.numeroDuJour(jour)
        return if (semaine != null && numero <= 5) semaine.motDuJour(numero) else null
    }

    /**
     * La carte postale : les mots de la semaine ont tous été travaillés, et chacun a été trouvé du
     * premier coup la première fois qu'il a été demandé (quel que soit le jour).
     */
    fun semaineParfaite(semaine: SemaineItalien, seancesDeLaSemaine: List<Seance>): Boolean {
        val mots = (1..5).mapNotNull { semaine.motDuJour(it) }.map { it.code }
        if (mots.isEmpty()) return false
        val premiereFois = HashMap<String, Double>()
        for (s in seancesDeLaSemaine.sortedBy { it.soir }) {
            for (r in s.resultats) premiereFois.putIfAbsent(r.mot, r.points)
        }
        return mots.all { premiereFois[it] == 1.0 }
    }

    /** Une carte au hasard parmi celles que l'enfant n'a pas encore. */
    fun tirerCarte(toutes: List<CartePostale>, gagnees: List<CarteGagnee>, hasard: Random = Random.Default): CartePostale? {
        val dejaLa = gagnees.map { it.idCarte }.toSet()
        return toutes.filter { it.id !in dejaLa }.randomOrNull(hasard)
    }
}
