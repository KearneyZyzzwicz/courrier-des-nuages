package fr.courrierdesnuages.app.suivi

import fr.courrierdesnuages.app.calcul.Calcul
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.data.Soir
import fr.courrierdesnuages.app.data.StatsAnnee
import fr.courrierdesnuages.app.data.StatsMatiere
import kotlin.math.roundToInt

/** Ce qu'une séance (ou un soir passé) ajoute aux compteurs de l'année. */
data class Ajout(
    val matiere: String,
    val points: Double,
    val possibles: Double,
    val livraisons: Long = 0,
    val medaille: Medaille = Medaille.AUCUNE,
)

/**
 * Le classement : pourcentage de réussite (points obtenus / points possibles) depuis la rentrée.
 * Seuls les jours programmés comptent ; un jour programmé sans séance compte 0. Un soir est passé
 * quand le parent envoie le suivant : sa séance n'est alors plus possible.
 */
object Classement {

    /** Ce que rapporte une séance terminée. Pas de médaille en italien : la récompense, c'est la carte postale. */
    fun pourSeance(s: Seance): Ajout = Ajout(
        matiere = s.matiere,
        points = s.points,
        possibles = s.ordre.size.toDouble(),
        livraisons = s.resultats.count { it.points > 0.0 }.toLong(),
        medaille = if (s.matiere == Matiere.ITALIEN) Medaille.AUCUNE else s.medaille,
    )

    /**
     * Un soir passé : pour le français et les maths programmés ce soir-là, une séance jamais faite
     * compte 0 sur le nombre de mots ou de calculs, une séance pas finie compte ce qui a été fait sur
     * la totalité. Une séance terminée est comptée à part (avec [pourSeance]). L'italien n'est jamais
     * pénalisé : un soir sans séance n'est pas une faute, son mot attend la séance suivante.
     */
    fun soirPasse(soir: Soir, seances: List<Seance>): List<Ajout> {
        val ajouts = ArrayList<Ajout>()
        val programme = listOf(
            Matiere.FRANCAIS to soir.mots.size,
            Matiere.MATHS to Calcul.pourLesTables(soir.tables).size,
        )
        for ((matiere, nombre) in programme) {
            if (nombre == 0) continue
            val s = seances.firstOrNull { it.soir == soir.date && it.matiere == matiere }
            when {
                s == null -> ajouts += Ajout(matiere, 0.0, nombre.toDouble())
                !s.terminee -> ajouts += Ajout(
                    matiere = matiere,
                    points = s.points,
                    possibles = s.ordre.size.toDouble(),
                    livraisons = s.resultats.count { it.points > 0.0 }.toLong(),
                )
                else -> Unit
            }
        }
        return ajouts
    }

    /** Ajoute des séances ou des soirs passés aux compteurs. */
    fun ajouter(stats: StatsAnnee, ajouts: List<Ajout>): StatsAnnee {
        var r = stats
        for (a in ajouts) {
            val m = r.matiere(a.matiere)
            val nouveau = m.copy(
                points = m.points + a.points,
                possibles = m.possibles + a.possibles,
                or = m.or + if (a.medaille == Medaille.OR) 1 else 0,
                argent = m.argent + if (a.medaille == Medaille.ARGENT) 1 else 0,
            )
            r = when (a.matiere) {
                Matiere.MATHS -> r.copy(maths = nouveau)
                Matiere.ITALIEN -> r.copy(italien = nouveau)
                else -> r.copy(francais = nouveau)
            }
            r = r.copy(livraisons = r.livraisons + a.livraisons)
        }
        return r
    }

    /**
     * Les compteurs de l'année, recalculés à partir de toutes les séances et de tous les soirs depuis
     * la rentrée. Les soirs d'avant [ouvert] sont passés ; le soir [ouvert] (le dernier envoyé) reste
     * ouvert jusqu'au suivant. Recalculer chaque soir rattrape ce que plusieurs téléphones hors ligne
     * auraient pu compter de travers.
     */
    fun recalculer(seances: List<Seance>, soirs: List<Soir>, ouvert: String): StatsAnnee {
        var stats = StatsAnnee(calcule = true)
        stats = ajouter(stats, seances.filter { it.terminee }.map { pourSeance(it) })
        for (soir in soirs.distinctBy { it.date }) {
            if (soir.date < ouvert) stats = ajouter(stats, soirPasse(soir, seances))
        }
        return stats
    }

    /** Le pourcentage tel qu'il est affiché (arrondi), ou null tant qu'aucun jour n'a compté. */
    fun pourcentageAffiche(m: StatsMatiere): Int? = m.pourcentage?.roundToInt()

    /** Une place du classement : les ex aequo (même pourcentage affiché) partagent la même place. */
    data class Place<T>(val enfant: T, val rang: Int?, val pourcentage: Int?)

    /** Les enfants classés pour une matière, du meilleur pourcentage au plus petit (sans jour compté : à la fin). */
    fun <T> classer(enfants: List<T>, stats: (T) -> StatsMatiere): List<Place<T>> {
        val avecPourcentage = enfants.map { it to pourcentageAffiche(stats(it)) }
        val tries = avecPourcentage.sortedWith(compareByDescending<Pair<T, Int?>> { it.second ?: -1 })
        return tries.map { (enfant, p) ->
            Place(
                enfant = enfant,
                rang = if (p == null) null else 1 + tries.count { (it.second ?: -1) > p },
                pourcentage = p,
            )
        }
    }
}

/** Somme les ajouts d'une même matière, pour n'écrire qu'une fois chaque compteur. */
fun List<Ajout>.parMatiere(): Map<String, Ajout> = groupBy { it.matiere }.mapValues { (matiere, liste) ->
    Ajout(
        matiere = matiere,
        points = liste.sumOf { it.points },
        possibles = liste.sumOf { it.possibles },
        livraisons = liste.sumOf { it.livraisons },
    )
}

/** « 1er », « 2e », « 3e »… */
internal fun rangEnTexte(rang: Int): String = if (rang == 1) "1er" else "${rang}e"
