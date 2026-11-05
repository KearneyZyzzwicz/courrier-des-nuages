package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.data.CLASSES
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.RougeFonce

/** Créer ou modifier le profil d'un enfant (espace parent). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranFicheEnfant(vm: AppViewModel, profil: Profil?) {
    val nouveau = profil == null
    var prenom by rememberSaveable { mutableStateOf(profil?.prenom.orEmpty()) }
    var classe by rememberSaveable { mutableStateOf(profil?.classe ?: CLASSES.first()) }
    var cleAvatar by rememberSaveable { mutableStateOf(profil?.avatar?.cle ?: Avatar.HERISSON.cle) }
    var code by remember { mutableStateOf("") }
    var confirmerSuppression by remember { mutableStateOf(false) }

    val avatar = Avatar.depuisCle(cleAvatar)
    val codeValide = if (nouveau) code.length == 4 else code.isEmpty() || code.length == 4
    val peutEnregistrer = prenom.isNotBlank() && codeValide

    Column(Modifier.fillMaxSize()) {
        BarreRetour(profil?.prenom ?: "Nouvel enfant", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AvatarAnime(avatar, taille = 120.dp)
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Prénom", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it.take(20) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Classe", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (c in CLASSES) {
                        Pastille(texte = c, choisie = c == classe, onClick = { classe = c })
                    }
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Avatar", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${avatar.nom} ${avatar.engin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                Spacer(Modifier.height(10.dp))
                for (ligne in Avatar.entries.chunked(4)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (a in ligne) {
                            ChoixAvatar(
                                avatar = a,
                                choisi = a == avatar,
                                onClick = { cleAvatar = a.cle },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text(
                    if (nouveau) "Code secret" else "Changer le code secret",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (nouveau) "4 chiffres que l'enfant tapera pour ouvrir sa séance."
                    else "Laisse vide pour garder le code actuel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    placeholder = { Text("4 chiffres") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.width(180.dp),
                )
            }

            BoutonPrincipal(
                texte = "Enregistrer",
                onClick = { vm.enregistrerProfil(profil, prenom, classe, avatar, code) },
                actif = peutEnregistrer,
                modifier = Modifier.fillMaxWidth(),
            )
            if (profil != null) {
                BoutonSecondaire(
                    texte = "Supprimer ce profil",
                    onClick = { confirmerSuppression = true },
                    couleur = RougeFonce,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmerSuppression && profil != null) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text("Supprimer le profil de ${profil.prenom} ?") },
            text = { Text("Son avatar, son code et ses résultats seront effacés sur tous les téléphones.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmerSuppression = false
                    vm.supprimerProfil(profil)
                }) { Text("Supprimer", color = RougeFonce) }
            },
            dismissButton = {
                TextButton(onClick = { confirmerSuppression = false }) { Text("Annuler") }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun ChoixAvatar(avatar: Avatar, choisi: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        color = if (choisi) Jaune.copy(alpha = 0.3f) else Color.Transparent,
        border = if (choisi) BorderStroke(3.dp, Jaune) else null,
    ) {
        Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            ImageAvatar(avatar, taille = 64.dp)
        }
    }
}
