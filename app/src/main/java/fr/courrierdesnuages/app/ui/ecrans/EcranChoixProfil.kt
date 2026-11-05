package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BanniereMiseAJour
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce

/** « Qui es-tu ? » : chaque enfant choisit sa séance, sur n'importe quel téléphone. */
@Composable
fun EcranChoixProfil(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.pigeon),
                contentDescription = null,
                modifier = Modifier.size(68.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Le Courrier des Nuages", style = MaterialTheme.typography.titleMedium, color = EncreDouce)
                Text("Qui es-tu ?", style = MaterialTheme.typography.displaySmall, color = Encre)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val erreur = vm.erreurDonnees
            when {
                !vm.profilsCharges && erreur != null -> MessageCentre(erreur, couleur = RougeFonce)
                !vm.profilsCharges -> Chargement("Ouverture du courrier…")
                vm.profils.isEmpty() -> Carte(Modifier.fillMaxWidth()) {
                    Text("Pas encore de profil", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Papa ou maman crée d'abord les profils dans l'Espace parent : prénom, classe, avatar et code secret.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EncreDouce,
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(vm.profils, key = { _, p -> p.id }) { rang, p ->
                        CarteProfil(p, rang) { vm.choisirProfil(p) }
                    }
                }
            }
        }
        BanniereMiseAJour(vm, Modifier.padding(bottom = 12.dp))
        BoutonSecondaire(
            texte = "Espace parent",
            onClick = { vm.ouvrirEspaceParent() },
            icone = Icons.Filled.Lock,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CarteProfil(profil: Profil, rang: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        contentColor = Encre,
        shadowElevation = 3.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarAnime(profil.avatar, taille = 112.dp, decalage = rang * 700)
            Spacer(Modifier.height(8.dp))
            Text(
                profil.prenom,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(profil.classe, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
        }
    }
}

@Composable
fun Chargement(texte: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Encre)
        Spacer(Modifier.height(16.dp))
        Text(texte, style = MaterialTheme.typography.bodyLarge, color = EncreDouce, textAlign = TextAlign.Center)
    }
}

@Composable
fun MessageCentre(texte: String, modifier: Modifier = Modifier, couleur: Color = EncreDouce) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texte, style = MaterialTheme.typography.bodyLarge, color = couleur, textAlign = TextAlign.Center)
    }
}
