package fr.courrierdesnuages.app.italien

import androidx.annotation.DrawableRes
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Seance
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** Un mot de vocabulaire : le français affiché, l'italien à écrire. */
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

    /**
     * Les mots du soir : le mot du jour, plus les mots pas sus du premier coup qui reviennent,
     * plus les mots des soirs précédents de la semaine qui n'ont pas été faits.
     */
    fun motsDuSoir(
        semaine: SemaineItalien,
        jour: LocalDate,
        seancesDeLaSemaine: List<Seance>,
        aRevoir: List<MotItalien>,
    ): List<MotItalien> {
        val numero = Semaines.numeroDuJour(jour)
        if (numero > 5) return emptyList()
        val duJour = semaine.motDuJour(numero) ?: return emptyList()
        val oublies = (1 until numero).mapNotNull { n ->
            val date = Semaines.lundi(jour).plusDays((n - 1).toLong()).toString()
            val faite = seancesDeLaSemaine.any { it.soir == date && it.terminee }
            if (faite) null else semaine.motDuJour(n)
        }
        val tous = listOf(duJour) + aRevoir + oublies
        val vus = HashSet<String>()
        return tous.filter { vus.add(it.code) }
    }

    /**
     * Une semaine sans aucune faute : chaque soir qui avait un mot (du lundi au vendredi) a sa séance
     * terminée, et chaque mot de chaque séance a été trouvé du premier coup.
     */
    fun semaineParfaite(semaine: SemaineItalien, seancesDeLaSemaine: List<Seance>): Boolean {
        val lundi = LocalDate.parse(semaine.lundi)
        val jours = (1..5).filter { semaine.motDuJour(it) != null }
        if (jours.isEmpty()) return false
        return jours.all { n ->
            val date = lundi.plusDays((n - 1).toLong()).toString()
            val seance = seancesDeLaSemaine.firstOrNull { it.soir == date }
            seance != null && seance.terminee && seance.fautes == 0 && seance.resultats.isNotEmpty()
        }
    }

    /** Le dernier soir de la semaine qui a un mot : c'est ce soir-là que la carte peut être gagnée. */
    fun dernierSoir(semaine: SemaineItalien): String? {
        val lundi = LocalDate.parse(semaine.lundi)
        val n = (1..5).lastOrNull { semaine.motDuJour(it) != null } ?: return null
        return lundi.plusDays((n - 1).toLong()).toString()
    }

    /** Une carte au hasard parmi celles que l'enfant n'a pas encore. */
    fun tirerCarte(toutes: List<CartePostale>, gagnees: List<CarteGagnee>, hasard: Random = Random.Default): CartePostale? {
        val dejaLa = gagnees.map { it.idCarte }.toSet()
        return toutes.filter { it.id !in dejaLa }.randomOrNull(hasard)
    }
}
