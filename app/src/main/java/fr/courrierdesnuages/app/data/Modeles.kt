package fr.courrierdesnuages.app.data

import androidx.annotation.DrawableRes
import fr.courrierdesnuages.app.R

/** Les huit facteurs du ciel. La clé est ce qui est enregistré dans la base. */
enum class Avatar(val cle: String, val nom: String, val engin: String, @param:DrawableRes val image: Int) {
    HERISSON("herisson", "Le hérisson", "en montgolfière", R.drawable.avatar_herisson),
    SOURIS("souris", "La souris", "en avion de papier", R.drawable.avatar_souris),
    OURS("ours", "L'ours", "en dirigeable", R.drawable.avatar_ours),
    RENARD("renard", "Le renard", "en biplan", R.drawable.avatar_renard),
    CHOUETTE("chouette", "La chouette", "en planeur", R.drawable.avatar_chouette),
    LAPIN("lapin", "Le lapin", "en hélicoptère à pédales", R.drawable.avatar_lapin),
    TORTUE("tortue", "La tortue", "en fusée", R.drawable.avatar_tortue),
    ECUREUIL("ecureuil", "L'écureuil", "en parapente", R.drawable.avatar_ecureuil);

    companion object {
        fun depuisCle(cle: String?): Avatar = entries.firstOrNull { it.cle == cle } ?: HERISSON
    }
}

val CLASSES = listOf("CP", "CE1", "CE2", "CM1", "CM2", "6e", "5e", "4e", "3e")

data class Profil(
    val id: String,
    val prenom: String,
    val classe: String,
    val avatar: Avatar,
    val codeEmpreinte: String,
    val codeSel: String,
    val ordre: Long,
    /** La liste de mots de la semaine, lue sur la feuille de l'école. */
    val liste: List<String> = emptyList(),
    /** Moment où la liste a été enregistrée (millisecondes). */
    val listeLe: Long = 0L,
)

/** Le code parent n'est jamais enregistré en clair : seulement son empreinte. */
data class CodeParent(val empreinte: String, val sel: String)

/** Ce que le parent envoie un soir à un enfant. [date] au format 2026-11-05. */
data class Soir(
    val date: String,
    val mots: List<String>,
    val tables: List<String>,
    val envoyeLe: Long,
)

/** Résultat d'un mot dans une séance : 1, 0,5 ou 0 point. */
data class ResultatMot(val mot: String, val points: Double, val revision: Boolean)

object Matiere {
    const val FRANCAIS = "francais"
    const val MATHS = "maths"
    const val ITALIEN = "italien"
}

object EtatSeance {
    const val EN_COURS = "en_cours"
    const val TERMINEE = "terminee"
}

/** La séance qui compte pour un soir et une matière (la première de la journée). */
data class Seance(
    val soir: String,
    val matiere: String,
    val etat: String,
    val ordre: List<String>,
    val revisions: List<String>,
    val resultats: List<ResultatMot>,
    val debutLe: Long,
    val finLe: Long,
) {
    val id: String get() = idSeance(soir, matiere)
    val terminee: Boolean get() = etat == EtatSeance.TERMINEE
    val points: Double get() = resultats.sumOf { it.points }
    val fautes: Int get() = resultats.count { it.points < 1.0 }
    val medaille: Medaille get() = if (!terminee) Medaille.AUCUNE else Medaille.pour(fautes)

    companion object {
        fun idSeance(soir: String, matiere: String) = "${soir}_$matiere"
    }
}

enum class Medaille(val cle: String) {
    OR("or"),
    ARGENT("argent"),
    AUCUNE("");

    companion object {
        /** Or : aucune faute. Argent : une seule faute. */
        fun pour(fautes: Int): Medaille = when (fautes) {
            0 -> OR
            1 -> ARGENT
            else -> AUCUNE
        }
    }
}

/** Un mot raté qui revient jusqu'à être réussi du premier coup deux jours de suite. */
data class Revision(val id: String, val mot: String, val reussitesDeSuite: Int, val dernierJour: String)

/** Écrit un nombre de points à la française : 8 ou 8,5. */
fun pointsEnTexte(points: Double): String =
    if (points % 1.0 == 0.0) points.toLong().toString() else points.toString().replace('.', ',')
