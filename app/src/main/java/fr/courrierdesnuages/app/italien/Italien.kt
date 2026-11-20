package fr.courrierdesnuages.app.italien

import androidx.annotation.DrawableRes
import java.time.DayOfWeek
import java.time.LocalDate

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
