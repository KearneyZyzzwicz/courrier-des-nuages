package fr.courrierdesnuages.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.langues.Langue

/** Les soirs, séances, mots et calculs à revoir d'un enfant, tenus à jour en direct. */
class DonneesEnfant(val idProfil: String, depot: Depot) {

    var soirs by mutableStateOf<List<Soir>>(emptyList())
        private set
    var seances by mutableStateOf<List<Seance>>(emptyList())
        private set
    var revisions by mutableStateOf<List<Revision>>(emptyList())
        private set
    var revisionsMaths by mutableStateOf<List<Revision>>(emptyList())
        private set
    var revisionsItalien by mutableStateOf<List<Revision>>(emptyList())
        private set
    var revisionsAnglais by mutableStateOf<List<Revision>>(emptyList())
        private set

    /** Les cartes postales d'Italie. */
    var cartesGagnees by mutableStateOf<List<CarteGagnee>>(emptyList())
        private set

    /** Les cartes du tour du monde anglophone. */
    var cartesMonde by mutableStateOf<List<CarteGagnee>>(emptyList())
        private set

    /** Les cartes des départements de France. */
    var cartesFrance by mutableStateOf<List<CarteGagnee>>(emptyList())
        private set
    var soirsCharges by mutableStateOf(false)
        private set
    var seancesChargees by mutableStateOf(false)
        private set
    var revisionsChargees by mutableStateOf(false)
        private set
    var revisionsMathsChargees by mutableStateOf(false)
        private set
    var revisionsItalienChargees by mutableStateOf(false)
        private set
    var revisionsAnglaisChargees by mutableStateOf(false)
        private set
    var cartesChargees by mutableStateOf(false)
        private set
    var cartesMondeChargees by mutableStateOf(false)
        private set
    var cartesFranceChargees by mutableStateOf(false)
        private set
    var erreur by mutableStateOf<String?>(null)
        private set

    val chargees: Boolean
        get() = soirsCharges && seancesChargees && revisionsChargees && revisionsMathsChargees &&
            revisionsItalienChargees && revisionsAnglaisChargees && cartesChargees && cartesMondeChargees && cartesFranceChargees

    /** Les mots à revoir d'une langue. */
    fun revisionsDe(langue: Langue): List<Revision> = when (langue) {
        Langue.ITALIEN -> revisionsItalien
        Langue.ANGLAIS -> revisionsAnglais
    }

    /** Les cartes postales gagnées de l'album d'une langue. */
    fun cartesDe(langue: Langue): List<CarteGagnee> = when (langue) {
        Langue.ITALIEN -> cartesGagnees
        Langue.ANGLAIS -> cartesMonde
    }

    /** Le dernier soir envoyé par le parent : c'est la séance ouverte jusqu'au soir suivant. */
    val dernierSoir: Soir? get() = soirs.firstOrNull()

    fun soir(jour: String): Soir? = soirs.firstOrNull { it.date == jour }

    fun seance(jour: String, matiere: String): Seance? =
        seances.firstOrNull { it.soir == jour && it.matiere == matiere }

    private val ecoutes: List<ListenerRegistration> = listOf(
        depot.ecouterSoirs(idProfil, 21, { soirs = it; soirsCharges = true }, { erreur = it.message }),
        depot.ecouterSeances(idProfil, 40, { seances = it; seancesChargees = true }, { erreur = it.message }),
        depot.ecouterRevisions(idProfil, REVISIONS_FRANCAIS, { revisions = it; revisionsChargees = true }, { erreur = it.message }),
        depot.ecouterRevisions(idProfil, REVISIONS_MATHS, { revisionsMaths = it; revisionsMathsChargees = true }, { erreur = it.message }),
        depot.ecouterRevisions(idProfil, REVISIONS_ITALIEN, { revisionsItalien = it; revisionsItalienChargees = true }, { erreur = it.message }),
        depot.ecouterRevisions(idProfil, REVISIONS_ANGLAIS, { revisionsAnglais = it; revisionsAnglaisChargees = true }, { erreur = it.message }),
        depot.ecouterCartesGagnees(idProfil, CARTES_ITALIE, { cartesGagnees = it; cartesChargees = true }, { erreur = it.message }),
        depot.ecouterCartesGagnees(idProfil, CARTES_MONDE, { cartesMonde = it; cartesMondeChargees = true }, { erreur = it.message }),
        depot.ecouterCartesGagnees(idProfil, CARTES_FRANCE, { cartesFrance = it; cartesFranceChargees = true }, { erreur = it.message }),
    )

    fun arreter() {
        ecoutes.forEach { it.remove() }
    }
}
