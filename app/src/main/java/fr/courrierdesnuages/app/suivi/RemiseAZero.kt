package fr.courrierdesnuages.app.suivi

import fr.courrierdesnuages.app.calcul.Calcul
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Revision
import fr.courrierdesnuages.app.data.RevisionAvant
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.italien.Semaines
import java.time.LocalDate

/**
 * La remise à zéro d'une séance du jour, toujours décidée par le parent (après un souci de
 * téléphone, par exemple) : la séance est effacée avec tout ce qu'elle avait changé, pour que
 * l'enfant la recommence comme si elle n'avait jamais eu lieu.
 */
object RemiseAZero {

    /** Ce qu'il faut défaire. */
    data class Plan(
        val idSeance: String,
        /** Les mots (ou calculs) à revoir remis dans leur état d'avant la séance. */
        val aRestaurer: List<Revision>,
        /** Les mots (ou calculs) à revoir que la séance avait ajoutés. */
        val aSupprimer: List<String>,
        /** Ce que la séance avait ajouté aux compteurs de l'année (séance terminée seulement). */
        val retrait: Ajout?,
        /** La carte postale de la semaine, si c'est cette séance qui l'a rapportée. */
        val idCarte: String?,
    )

    /**
     * La trace gardée dans chaque séance terminée : l'état d'avant de chaque mot à revoir qu'elle
     * modifie ou supprime (null : il n'existait pas).
     */
    fun trace(aEcrire: List<Revision>, aSupprimer: List<String>, actuelles: Map<String, Revision>): List<RevisionAvant> =
        (aEcrire.map { it.id } + aSupprimer).distinct().map { cle -> RevisionAvant(cle, actuelles[cle]) }

    /** La clé d'un mot à revoir, comme à la fin d'une séance (4 × 5 et 5 × 4 : la même). */
    fun cleRevision(matiere: String, mot: String): String = when (matiere) {
        Matiere.MATHS -> Mots.cle(Calcul.lire(mot)?.canonique?.texte ?: mot)
        else -> Mots.cle(mot)
    }

    fun preparer(seance: Seance, cartesGagnees: List<CarteGagnee>): Plan {
        val aRestaurer = ArrayList<Revision>()
        val aSupprimer = ArrayList<String>()
        if (seance.terminee) {
            val trace = seance.revisionsAvant
            if (trace != null) {
                for (r in trace) {
                    val avant = r.avant
                    if (avant != null) aRestaurer += avant else aSupprimer += r.cle
                }
            } else {
                // Séance finie avant que cette trace soit gardée : on retire au moins les mots ratés
                // qu'elle avait ajoutés aux mots à revoir.
                for (r in seance.resultats) {
                    if (r.points < 1.0 && !r.revision) aSupprimer += cleRevision(seance.matiere, r.mot)
                }
            }
        }
        val carte = if (seance.terminee && seance.matiere == Matiere.ITALIEN) {
            val lundi = runCatching { Semaines.lundi(LocalDate.parse(seance.soir)).toString() }.getOrNull()
            cartesGagnees.firstOrNull { it.semaine == lundi && it.gagneeLe >= seance.debutLe }
        } else {
            null
        }
        return Plan(
            idSeance = seance.id,
            aRestaurer = aRestaurer,
            aSupprimer = aSupprimer.distinct(),
            retrait = if (seance.terminee) Classement.pourSeance(seance) else null,
            idCarte = carte?.idCarte,
        )
    }
}
