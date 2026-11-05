package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.CreationCode
import fr.courrierdesnuages.app.ui.composants.SaisieCode
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce

@Composable
fun EcranCodeEnfant(vm: AppViewModel, profil: Profil) {
    Column(Modifier.fillMaxSize()) {
        BarreRetour("", onRetour = { vm.quitter() })
        SaisieCode(
            titre = "Tape ton code secret",
            modifier = Modifier.fillMaxWidth().weight(1f),
            entete = {
                AvatarAnime(profil.avatar, taille = 120.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bonjour ${profil.prenom} !",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Encre,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
            },
            pied = {
                Text(
                    "Code oublié ? Demande à papa ou maman.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                    textAlign = TextAlign.Center,
                )
            },
            surComplet = { code -> vm.verifierCodeEnfant(profil, code) },
        )
    }
}

@Composable
fun EcranCodeParent(vm: AppViewModel) {
    var oubli by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Espace parent", onRetour = { vm.quitter() })
        SaisieCode(
            titre = "Code parent",
            modifier = Modifier.fillMaxWidth().weight(1f),
            entete = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Encre, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(10.dp))
            },
            pied = {
                TextButton(onClick = { oubli = true }) { Text("Code parent oublié ?") }
            },
            surComplet = { code -> vm.verifierCodeParent(code) },
        )
    }
    if (oubli) {
        DialogueCodeOublie(
            vm = vm,
            onFermer = {
                oubli = false
                vm.effacerErreurMotDePasse()
            },
        )
    }
}

@Composable
private fun DialogueCodeOublie(vm: AppViewModel, onFermer: () -> Unit) {
    var motDePasse by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Code parent oublié") },
        text = {
            Column {
                Text(
                    "Tape le mot de passe du compte de la famille pour choisir un nouveau code parent.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motDePasse,
                    onValueChange = { motDePasse = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                val erreur = vm.erreurMotDePasse
                if (erreur != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(erreur, style = MaterialTheme.typography.bodyMedium, color = RougeFonce)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.codeOublie(motDePasse) }, enabled = !vm.verificationMotDePasse) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text("Annuler") }
        },
        containerColor = Color.White,
    )
}

@Composable
fun EcranNouveauCodeParent(vm: AppViewModel, premiereFois: Boolean) {
    Column(Modifier.fillMaxSize()) {
        BarreRetour(if (premiereFois) "Bienvenue !" else "Nouveau code parent", onRetour = { vm.retour() })
        // La toute première fois, on attend que la base confirme qu'aucun code n'existe encore.
        val pret = !premiereFois || (vm.codeParentCharge && vm.codeParent == null && vm.absenceCodeConfirmee)
        if (!pret) {
            Chargement(
                "Connexion à la base…\nPour la première configuration, le téléphone doit être connecté à Internet.",
                modifier = Modifier.weight(1f),
            )
        } else {
            CreationCode(
                titre = if (premiereFois) "Choisis ton code parent" else "Choisis un nouveau code",
                titreConfirmation = "Tape-le une deuxième fois",
                onCree = { code -> vm.enregistrerCodeParent(code) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                entete = {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Encre, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(10.dp))
                    if (premiereFois) {
                        Text(
                            "Ce code à 4 chiffres protège l'Espace parent : profils, saisie du soir et réglages.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = EncreDouce,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                },
            )
        }
    }
}
