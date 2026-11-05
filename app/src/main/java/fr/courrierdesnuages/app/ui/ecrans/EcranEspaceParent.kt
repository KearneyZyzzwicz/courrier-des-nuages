package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BanniereMiseAJour
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert

@Composable
fun EcranEspaceParent(vm: AppViewModel) {
    var confirmerDelier by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Espace parent", onRetour = { vm.quitter() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BanniereMiseAJour(vm)

            val envoi = vm.messageParent
            if (envoi != null) {
                Surface(shape = RoundedCornerShape(18.dp), color = Vert, contentColor = Color.White) {
                    Text(
                        envoi,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Saisie du soir", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                if (vm.profils.isEmpty()) {
                    Text(
                        "Ajoute d'abord les enfants ci-dessous.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }
                val aujourdhui = Jours.aujourdhui()
                for (p in vm.profils) {
                    val envoye = vm.donnees(p.id).soir(aujourdhui) != null
                    LigneSoir(p, envoye) { vm.ouvrirCeSoir(p) }
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Les enfants", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (vm.profils.isEmpty()) {
                    Text(
                        "Ajoute le profil de chaque enfant : prénom, classe, avatar et code secret.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }
                for (p in vm.profils) {
                    LigneEnfant(p) { vm.ouvrirFiche(p) }
                }
                Spacer(Modifier.height(10.dp))
                BoutonPrincipal(
                    texte = "Ajouter un enfant",
                    onClick = { vm.ouvrirFiche(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Réglages", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Changer le code parent",
                    onClick = { vm.changerCodeParent() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Rechercher une mise à jour",
                    onClick = { vm.verifierMiseAJour(silencieux = false) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = vm.messageMaj
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Délier ce téléphone",
                    onClick = { confirmerDelier = true },
                    couleur = RougeFonce,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val email = vm.emailCompte
            Text(
                if (email != null) "Version ${vm.nomVersion} · $email" else "Version ${vm.nomVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmerDelier) {
        AlertDialog(
            onDismissRequest = { confirmerDelier = false },
            title = { Text("Délier ce téléphone ?") },
            text = {
                Text(
                    "Les profils et les résultats restent en sécurité dans la base. " +
                        "Pour relier à nouveau ce téléphone, il faudra le mot de passe du compte de la famille.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerDelier = false
                    vm.delierTelephone()
                }) { Text("Délier", color = RougeFonce) }
            },
            dismissButton = {
                TextButton(onClick = { confirmerDelier = false }) { Text("Annuler") }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun LigneEnfant(profil: Profil, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profil.prenom, style = MaterialTheme.typography.titleMedium)
                Text(profil.classe, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = EncreDouce)
        }
    }
}

@Composable
private fun LigneSoir(profil: Profil, envoye: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profil.prenom, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (envoye) "Envoyé ce soir ✓" else "À préparer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (envoye) Vert else EncreDouce,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = Encre, contentColor = Color.White) {
                Text(
                    "Ce soir",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
