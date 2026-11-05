package fr.courrierdesnuages.app.suivi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.StatsMatiere
import fr.courrierdesnuages.app.dictee.DessinMedaille
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.Vert

/** Une matière telle qu'elle apparaît dans les classements. */
internal data class MatiereAffichee(val cle: String, val nom: String, val symbole: String, val couleur: Color)

internal val MATIERES_AFFICHEES = listOf(
    MatiereAffichee(Matiere.FRANCAIS, "Français", "Aa", Rouge),
    MatiereAffichee(Matiere.MATHS, "Maths", "+×", Vert),
    MatiereAffichee(Matiere.ITALIEN, "Italien", "Ciao", Jaune),
)

/** Les classements de l'année, et la collection de médailles de l'enfant. */
@Composable
fun EcranClassement(vm: AppViewModel, profil: Profil) {
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Le classement", onRetour = { vm.retourAccueil(profil) })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CollectionDeMedailles(profil)
            for (m in MATIERES_AFFICHEES) {
                Carte(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(m.couleur),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(m.symbole, style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(m.nom, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    for (place in Classement.classer(vm.profils) { it.stats.matiere(m.cle) }) {
                        LignePlace(place, m, estMoi = place.enfant.id == profil.id)
                    }
                }
            }
            Text(
                "Le pourcentage, ce sont les points gagnés sur les points possibles depuis la rentrée. " +
                    "Seuls les soirs prévus comptent, et un soir prévu sans séance compte 0.",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CollectionDeMedailles(profil: Profil) {
    Carte(Modifier.fillMaxWidth()) {
        Text("Ma collection de médailles", style = MaterialTheme.typography.titleLarge)
        val francais = profil.stats.francais
        val maths = profil.stats.maths
        if (francais.or + francais.argent + maths.or + maths.argent == 0L) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Pas encore de médaille cette année. Une séance sans faute rapporte l'or, une seule faute l'argent.",
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ColonneMedailles("Français", francais)
                ColonneMedailles("Maths", maths)
            }
        }
    }
}

@Composable
private fun ColonneMedailles(titre: String, stats: StatsMatiere) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(titre, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DessinMedaille(Medaille.OR, Modifier.size(46.dp))
            Text("× ${stats.or}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            DessinMedaille(Medaille.ARGENT, Modifier.size(46.dp))
            Text("× ${stats.argent}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LignePlace(place: Classement.Place<Profil>, matiere: MatiereAffichee, estMoi: Boolean) {
    val p = place.enfant
    val stats = p.stats.matiere(matiere.cle)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (estMoi) matiere.couleur.copy(alpha = 0.16f) else CielBas.copy(alpha = 0.6f),
        contentColor = Encre,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val rang = place.rang
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when (rang) {
                            1 -> Color(0xFFF2C14E)
                            2 -> Color(0xFFC9D2DE)
                            3 -> Color(0xFFE0A66B)
                            else -> Color.White
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (rang == null) "–" else rangEnTexte(rang),
                    style = MaterialTheme.typography.labelMedium,
                    color = Encre,
                )
            }
            Spacer(Modifier.width(8.dp))
            ImageAvatar(p.avatar, taille = 44.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.prenom,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (estMoi) FontWeight.Bold else null,
                )
                if (matiere.cle != Matiere.ITALIEN && stats.or + stats.argent > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DessinMedaille(Medaille.OR, Modifier.size(20.dp))
                        Text("${stats.or}", style = MaterialTheme.typography.bodySmall, color = EncreDouce)
                        Spacer(Modifier.width(6.dp))
                        DessinMedaille(Medaille.ARGENT, Modifier.size(20.dp))
                        Text("${stats.argent}", style = MaterialTheme.typography.bodySmall, color = EncreDouce)
                    }
                } else {
                    Text(p.classe, style = MaterialTheme.typography.bodySmall, color = EncreDouce)
                }
            }
            val pourcentage = place.pourcentage
            Text(
                if (pourcentage == null) "pas encore" else "$pourcentage\u00A0%",
                style = if (pourcentage == null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.headlineSmall,
                color = if (pourcentage == null) EncreDouce else Encre,
            )
        }
    }
}
