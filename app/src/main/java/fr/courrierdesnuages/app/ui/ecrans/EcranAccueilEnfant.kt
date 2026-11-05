package fr.courrierdesnuages.app.ui.ecrans

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.Vert

/** L'accueil de l'enfant une fois son code tapé. */
@Composable
fun EcranAccueilEnfant(vm: AppViewModel, profil: Profil) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            BoutonSecondaire(
                texte = "Quitter",
                onClick = { vm.quitter() },
                icone = Icons.AutoMirrored.Filled.ExitToApp,
            )
        }
        AvatarAnime(profil.avatar, taille = 150.dp)
        Text(
            "Bonjour ${profil.prenom} !",
            style = MaterialTheme.typography.displaySmall,
            color = Encre,
            textAlign = TextAlign.Center,
        )
        Text(
            "${profil.avatar.nom} ${profil.avatar.engin} t'attend pour la tournée.",
            style = MaterialTheme.typography.bodyLarge,
            color = EncreDouce,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        TuileMatiere(matiere = "Français", titre = "La dictée de l'avion", couleur = Rouge, symbole = "Aa")
        TuileMatiere(matiere = "Maths", titre = "Le tri des colis", couleur = Vert, symbole = "+×")
        TuileMatiere(matiere = "Italien", titre = "Le pigeon voyageur", couleur = Jaune, symbole = "Ciao")
        Spacer(Modifier.height(12.dp))
        Text(
            "Les exercices arrivent dans la prochaine version !",
            style = MaterialTheme.typography.bodyMedium,
            color = EncreDouce,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TuileMatiere(matiere: String, titre: String, couleur: Color, symbole: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        contentColor = Encre,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(couleur),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbole, style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(matiere, style = MaterialTheme.typography.titleLarge)
                Text(titre, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            Surface(shape = RoundedCornerShape(50), color = CielBas) {
                Text(
                    "Bientôt",
                    style = MaterialTheme.typography.labelMedium,
                    color = EncreDouce,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
