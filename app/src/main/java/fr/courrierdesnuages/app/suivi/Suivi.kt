package fr.courrierdesnuages.app.suivi

import fr.courrierdesnuages.app.calcul.Calcul
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.data.Soir
import fr.courrierdesnuages.app.italien.MotItalien
import java.time.LocalDate

/** Le résultat d'une matière un jour donné, vu par le parent. */
sealed interface Case {
    /** Séance terminée. */
    data class Faite(val points: Double, val total: Int, val medaille: Medaille) : Case

    /** Séance commencée : encore possible si le soir est ouvert, comptée telle quelle sinon. */
    data class Commencee(val points: Double, val faits: Int, val total: Int, val ouverte: Boolean) : Case

    /** Le soir est encore ouvert et la séance n'a pas commencé. */
    data object AFaire : Case

    /** Le soir est passé sans séance : 0 point. */
    data object PasFaite : Case
}

/** Une ligne du suivi jour par jour ; une matière à null n'avait rien de programmé ce jour-là. */
data class JourDeSuivi(val jour: String, val francais: Case?, val maths: Case?, val italien: Case?)

/** Un mot ou un calcul raté, et le nombre de fois où il n'a pas été su du premier coup. */
data class Rate(val texte: String, val fois: Int)

object Suivi {

    /**
     * Les résultats jour par jour, du plus récent au plus ancien. [ouvert] est le dernier soir
     * envoyé : sa séance reste possible jusqu'au soir suivant. L'italien se fait le jour même : seule
     * la séance d'[aujourdhui] peut encore être finie.
     */
    fun jourParJour(
        soirs: List<Soir>,
        seances: List<Seance>,
        ouvert: String?,
        aujourdhui: String = LocalDate.now().toString(),
    ): List<JourDeSuivi> {
        val parSoir = soirs.associateBy { it.date }
        val parId = seances.associateBy { it.id }
        val jours = (parSoir.keys + seances.map { it.soir }).distinct().sortedDescending()
        return jours.map { jour ->
            val soir = parSoir[jour]
            val estOuvert = jour == ouvert
            JourDeSuivi(
                jour = jour,
                francais = caseProgrammee(soir?.mots?.isNotEmpty() == true, parId[Seance.idSeance(jour, Matiere.FRANCAIS)], estOuvert),
                maths = caseProgrammee(soir?.tables?.isNotEmpty() == true, parId[Seance.idSeance(jour, Matiere.MATHS)], estOuvert),
                italien = parId[Seance.idSeance(jour, Matiere.ITALIEN)]?.let { caseSeance(it, ouverte = jour == aujourdhui) },
            )
        }.filter { it.francais != null || it.maths != null || it.italien != null }
    }

    private fun caseProgrammee(programme: Boolean, s: Seance?, ouvert: Boolean): Case? = when {
        s != null -> caseSeance(s, ouvert)
        !programme -> null
        ouvert -> Case.AFaire
        else -> Case.PasFaite
    }

    private fun caseSeance(s: Seance, ouverte: Boolean): Case =
        if (s.terminee) {
            Case.Faite(s.points, s.ordre.size, if (s.matiere == Matiere.ITALIEN) Medaille.AUCUNE else s.medaille)
        } else {
            Case.Commencee(s.points, s.resultats.size, s.ordre.size, ouverte)
        }

    /**
     * Les mots (ou calculs) les moins sus : le nombre de fois où chacun n'a pas été réussi du premier
     * coup, dans les séances qui comptent. 4 × 7 et 7 × 4 sont le même calcul.
     */
    fun plusRates(seances: List<Seance>, matiere: String, combien: Int = 5): List<Rate> {
        val compte = LinkedHashMap<String, Int>()
        for (s in seances) {
            if (s.matiere != matiere) continue
            for (r in s.resultats) {
                if (r.points >= 1.0) continue
                val texte = when (matiere) {
                    Matiere.MATHS -> Calcul.lire(r.mot)?.canonique?.texte ?: r.mot
                    Matiere.ITALIEN -> MotItalien.lire(r.mot)?.let { "${it.italien} (${it.francais})" } ?: r.mot
                    else -> r.mot
                }
                compte[texte] = (compte[texte] ?: 0) + 1
            }
        }
        return compte.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .take(combien)
            .map { Rate(it.key, it.value) }
    }
}
