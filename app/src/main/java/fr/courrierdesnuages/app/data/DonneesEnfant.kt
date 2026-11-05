package fr.courrierdesnuages.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration

/** Les soirs, séances et mots à revoir d'un enfant, tenus à jour en direct. */
class DonneesEnfant(val idProfil: String, depot: Depot) {

    var soirs by mutableStateOf<List<Soir>>(emptyList())
        private set
    var seances by mutableStateOf<List<Seance>>(emptyList())
        private set
    var revisions by mutableStateOf<List<Revision>>(emptyList())
        private set
    var soirsCharges by mutableStateOf(false)
        private set
    var seancesChargees by mutableStateOf(false)
        private set
    var revisionsChargees by mutableStateOf(false)
        private set
    var erreur by mutableStateOf<String?>(null)
        private set

    val chargees: Boolean get() = soirsCharges && seancesChargees && revisionsChargees

    /** Le dernier soir envoyé par le parent : c'est la séance ouverte jusqu'au soir suivant. */
    val dernierSoir: Soir? get() = soirs.firstOrNull()

    fun soir(jour: String): Soir? = soirs.firstOrNull { it.date == jour }

    fun seance(jour: String, matiere: String): Seance? =
        seances.firstOrNull { it.soir == jour && it.matiere == matiere }

    private val ecoutes: List<ListenerRegistration> = listOf(
        depot.ecouterSoirs(idProfil, 21, { soirs = it; soirsCharges = true }, { erreur = it.message }),
        depot.ecouterSeances(idProfil, 40, { seances = it; seancesChargees = true }, { erreur = it.message }),
        depot.ecouterRevisions(idProfil, { revisions = it; revisionsChargees = true }, { erreur = it.message }),
    )

    fun arreter() {
        ecoutes.forEach { it.remove() }
    }
}
