package fr.courrierdesnuages.app.suivi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.DonneesEnfant
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.data.Soir
import fr.courrierdesnuages.app.data.pointsEnTexte
import fr.courrierdesnuages.app.dictee.DessinMedaille
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.SEMAINE_OFFERTE
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import java.time.Instant
import java.time.ZoneId

/** Le suivi d'un enfant pour le parent : aujourd'hui, l'année, les plus ratés, les cartes et chaque jour. */
@Composable
fun EcranSuivi(vm: AppViewModel, profil: Profil) {
    val d = vm.donnees(profil.id)
    val annee = vm.anneeSuivie?.takeIf { it.idProfil == profil.id }
    // L'année lue à l'ouverture, complétée par les derniers jours suivis en direct.
    val seances = remember(annee, d.seances) { fusionSeances(annee?.seances, d.seances) }
    val soirs = remember(annee, d.soirs) { fusionSoirs(annee?.soirs, d.soirs) }
    val ouvert = d.dernierSoir?.date
    val jours = remember(seances, soirs, ouvert) { Suivi.jourParJour(soirs, seances, ouvert) }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Suivi de ${profil.prenom}", onRetour = { vm.retour() })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CarteAujourdhui(vm, profil, d) }
            item { CarteAnnee(vm, profil) }
            item { CartePlusRates(seances, complet = annee != null) }
            item { CarteCartesPostales(vm, d) }
            item {
                Text("Jour par jour", style = MaterialTheme.typography.titleLarge, color = Encre)
                if (annee == null) {
                    Text(
                        "Les derniers jours s'affichent ; le reste de l'année arrive dès que possible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EncreDouce,
                    )
                }
            }
            if (d.chargees && jours.isEmpty()) {
                item {
                    Text("Pas encore de séance cette année.", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
            }
            items(jours, key = { it.jour }) { j -> LigneJour(j) }
        }
    }
}

private fun fusionSeances(annee: List<Seance>?, recentes: List<Seance>): List<Seance> {
    val parId = LinkedHashMap<String, Seance>()
    annee?.forEach { parId[it.id] = it }
    recentes.forEach { parId[it.id] = it }
    return parId.values.toList()
}

private fun fusionSoirs(annee: List<Soir>?, recents: List<Soir>): List<Soir> {
    val parDate = LinkedHashMap<String, Soir>()
    annee?.forEach { parDate[it.date] = it }
    recents.forEach { parDate[it.date] = it }
    return parDate.values.toList()
}

@Composable
private fun CarteAujourdhui(vm: AppViewModel, profil: Profil, d: DonneesEnfant) {
    Carte(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Aujourd'hui", style = MaterialTheme.typography.titleLarge)
                Text("${profil.prenom} · ${profil.classe}", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (d.chargees) {
            EtatsDuJour(vm, profil, d)
        } else {
            Text("Chargement…", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
        }
    }
}

@Composable
private fun EtatsDuJour(vm: AppViewModel, profil: Profil, d: DonneesEnfant) {
    val soir = d.dernierSoir
    val aujourdhui = Jours.aujourdhui()
    val duSoir = when {
        soir == null -> null
        soir.date == aujourdhui -> null
        else -> "liste du ${Jours.enClair(soir.date)}"
    }
    LigneEtat(
        "Français",
        when {
            soir == null -> EtatDuJour("Aucune liste envoyée", EncreDouce)
            soir.mots.isEmpty() -> EtatDuJour("Pas de mots ce soir", EncreDouce)
            else -> etatSeance(d.seance(soir.date, Matiere.FRANCAIS), "mot", duSoir)
        },
    )
    LigneEtat(
        "Maths",
        when {
            soir == null -> EtatDuJour("Aucune liste envoyée", EncreDouce)
            soir.tables.isEmpty() -> EtatDuJour("Pas de tables ce soir", EncreDouce)
            else -> etatSeance(d.seance(soir.date, Matiere.MATHS), "calcul", duSoir)
        },
    )
    val italien = d.seance(aujourdhui, Matiere.ITALIEN)
    LigneEtat(
        "Italien",
        when {
            italien != null -> etatSeance(italien, "mot", null)
            vm.motsDuSoirItalien(profil).isEmpty() -> EtatDuJour("Pas de mot aujourd'hui", EncreDouce)
            else -> EtatDuJour("Pas encore faite", RougeFonce)
        },
    )
}

private data class EtatDuJour(val texte: String, val couleur: Color, val medaille: Medaille = Medaille.AUCUNE)

private fun etatSeance(s: Seance?, unite: String, duSoir: String?): EtatDuJour {
    val precision = if (duSoir != null) " ($duSoir)" else ""
    return when {
        s == null -> EtatDuJour("Pas encore faite$precision", RougeFonce)
        !s.terminee -> EtatDuJour("Commencée : ${s.resultats.size} $unite${if (s.resultats.size > 1) "s" else ""} sur ${s.ordre.size}$precision", EncreDouce)
        else -> EtatDuJour(
            "Faite : ${pointsEnTexte(s.points)} / ${s.ordre.size}$precision",
            Vert,
            if (s.matiere == Matiere.ITALIEN) Medaille.AUCUNE else s.medaille,
        )
    }
}

@Composable
private fun LigneEtat(matiere: String, etat: EtatDuJour) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(matiere, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(84.dp))
        Text(etat.texte, style = MaterialTheme.typography.bodyMedium, color = etat.couleur, modifier = Modifier.weight(1f))
        if (etat.medaille != Medaille.AUCUNE) DessinMedaille(etat.medaille, Modifier.size(28.dp))
    }
}

@Composable
private fun CarteAnnee(vm: AppViewModel, profil: Profil) {
    Carte(Modifier.fillMaxWidth()) {
        Text("Depuis la rentrée", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        for (m in MATIERES_AFFICHEES) {
            val stats = profil.stats.matiere(m.cle)
            val place = Classement.classer(vm.profils) { it.stats.matiere(m.cle) }.firstOrNull { it.enfant.id == profil.id }
            val pourcentage = place?.pourcentage
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.nom, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(84.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (pourcentage == null) "Pas encore de jour compté"
                        else "$pourcentage\u00A0% · ${pointsEnTexte(arrondi(stats.points))} points sur ${pointsEnTexte(arrondi(stats.possibles))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val rang = place?.rang
                    if (rang != null && vm.profils.size > 1) {
                        Text(
                            "${rangEnTexte(rang)} du classement sur ${vm.profils.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EncreDouce,
                        )
                    }
                }
                if (m.cle != Matiere.ITALIEN) {
                    DessinMedaille(Medaille.OR, Modifier.size(24.dp))
                    Text("${stats.or}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(4.dp))
                    DessinMedaille(Medaille.ARGENT, Modifier.size(24.dp))
                    Text("${stats.argent}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val livraisons = profil.stats.livraisons
        Text(
            "Tournée des îles : ${nombreEnTexte(livraisons)} livraison${if (livraisons > 1) "s" else ""}, " +
                "escale actuelle : ${Tournee.nom(Tournee.ileAtteinte(livraisons))}.",
            style = MaterialTheme.typography.bodySmall,
            color = EncreDouce,
        )
    }
}

/** Les compteurs additionnent des demi-points : on évite d'afficher 245,49999. */
private fun arrondi(x: Double): Double = Math.round(x * 2.0) / 2.0

@Composable
private fun CartePlusRates(seances: List<Seance>, complet: Boolean) {
    Carte(Modifier.fillMaxWidth()) {
        Text("Les plus ratés", style = MaterialTheme.typography.titleLarge)
        Text(
            if (complet) "Pas sus du premier coup, depuis la rentrée." else "Pas sus du premier coup, ces derniers jours.",
            style = MaterialTheme.typography.bodySmall,
            color = EncreDouce,
        )
        for (m in MATIERES_AFFICHEES) {
            Spacer(Modifier.height(8.dp))
            Text(m.nom, style = MaterialTheme.typography.titleMedium)
            val rates = remember(seances, m.cle) { Suivi.plusRates(seances, m.cle) }
            if (rates.isEmpty()) {
                Text("Aucun raté pour l'instant.", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            for (r in rates) {
                Row(Modifier.fillMaxWidth()) {
                    Text(r.texte, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${r.fois} fois", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
            }
        }
    }
}

@Composable
private fun CarteCartesPostales(vm: AppViewModel, d: DonneesEnfant) {
    Carte(Modifier.fillMaxWidth()) {
        val cartes = d.cartesGagnees.sortedByDescending { it.gagneeLe }
        Text(
            when (cartes.size) {
                0 -> "Cartes postales"
                1 -> "1 carte postale gagnée"
                else -> "${cartes.size} cartes postales gagnées"
            },
            style = MaterialTheme.typography.titleLarge,
        )
        if (cartes.isEmpty()) {
            Text(
                "Une semaine d'italien sans faute rapporte une carte.",
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
            )
        }
        for (c in cartes) {
            val carte = vm.carte(c.idCarte)
            val jour = Instant.ofEpochMilli(c.gagneeLe).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(carte?.ville ?: "Carte retirée", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Jours.enClair(jour) + if (c.semaine == SEMAINE_OFFERTE) " · offerte" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = EncreDouce,
                    )
                }
                Text(
                    when {
                        !c.epinglee -> "pas encore épinglée"
                        c.bienPlacee -> "bien placée ✓"
                        else -> "mal placée"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        !c.epinglee -> EncreDouce
                        c.bienPlacee -> Vert
                        else -> RougeFonce
                    },
                )
            }
        }
    }
}

@Composable
private fun LigneJour(j: JourDeSuivi) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        contentColor = Encre,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                Jours.enClair(j.jour).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CaseDuJour("Français", j.francais, Modifier.weight(1f))
                CaseDuJour("Maths", j.maths, Modifier.weight(1f))
                CaseDuJour("Italien", j.italien, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CaseDuJour(matiere: String, case: Case?, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = CielBas, contentColor = Encre, modifier = modifier) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(matiere, style = MaterialTheme.typography.labelMedium, color = EncreDouce)
            when (case) {
                null -> Text("—", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                is Case.Faite -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${pointsEnTexte(case.points)}/${case.total}", style = MaterialTheme.typography.bodyMedium)
                    if (case.medaille != Medaille.AUCUNE) DessinMedaille(case.medaille, Modifier.size(22.dp))
                }
                is Case.Commencee -> Text(
                    if (case.ouverte) "en cours" else "pas finie : ${pointsEnTexte(case.points)}/${case.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (case.ouverte) EncreDouce else RougeFonce,
                    textAlign = TextAlign.Center,
                )
                Case.AFaire -> Text("à faire", style = MaterialTheme.typography.bodySmall, color = EncreDouce)
                Case.PasFaite -> Text("pas faite", style = MaterialTheme.typography.bodySmall, color = RougeFonce)
            }
        }
    }
}
