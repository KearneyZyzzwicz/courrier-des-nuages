package fr.courrierdesnuages.app.data

import androidx.annotation.DrawableRes
import fr.courrierdesnuages.app.R

/** Les huit facteurs du ciel. La clé est ce qui est enregistré dans la base. */
enum class Avatar(val cle: String, val nom: String, val engin: String, @DrawableRes val image: Int) {
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
)

/** Le code parent n'est jamais enregistré en clair : seulement son empreinte. */
data class CodeParent(val empreinte: String, val sel: String)
