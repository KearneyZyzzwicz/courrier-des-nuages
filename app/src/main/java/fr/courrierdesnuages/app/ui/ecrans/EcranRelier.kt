package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce

/** Première ouverture sur un téléphone : on le relie au compte de la famille. */
@Composable
fun EcranRelier(vm: AppViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var voir by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val valider = {
        focus.clearFocus()
        vm.relier(email, motDePasse)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.pigeon),
            contentDescription = null,
            modifier = Modifier.size(150.dp),
        )
        Text(
            "Le Courrier des Nuages",
            style = MaterialTheme.typography.headlineMedium,
            color = Encre,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Première fois sur ce téléphone ? Relie-le au compte de la famille.",
            style = MaterialTheme.typography.bodyLarge,
            color = EncreDouce,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Carte(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Adresse e-mail de la famille") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = if (voir) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { valider() }),
                trailingIcon = {
                    TextButton(onClick = { voir = !voir }) { Text(if (voir) "Cacher" else "Voir") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            val erreur = vm.erreurRelier
            if (erreur != null) {
                Spacer(Modifier.height(12.dp))
                Text(erreur, style = MaterialTheme.typography.bodyMedium, color = RougeFonce)
            }
            Spacer(Modifier.height(20.dp))
            BoutonPrincipal(
                texte = "Relier ce téléphone",
                onClick = valider,
                enCours = vm.relierEnCours,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "À faire une seule fois par téléphone. Les enfants n'ont pas besoin de ce mot de passe.",
            style = MaterialTheme.typography.bodySmall,
            color = EncreDouce,
            textAlign = TextAlign.Center,
        )
    }
}
