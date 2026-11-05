package fr.courrierdesnuages.app.ui.composants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge

@Composable
fun BoutonPrincipal(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actif: Boolean = true,
    enCours: Boolean = false,
    couleur: Color = Rouge,
) {
    Button(
        onClick = onClick,
        enabled = actif && !enCours,
        modifier = modifier.heightIn(min = 58.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = couleur,
            contentColor = Color.White,
            disabledContainerColor = couleur.copy(alpha = 0.4f),
            disabledContentColor = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (enCours) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
        } else {
            Text(texte, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BoutonSecondaire(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    couleur: Color = Encre,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, couleur.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.9f),
            contentColor = couleur,
        ),
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texte, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun BarreRetour(titre: String, onRetour: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onRetour, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Encre,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            titre,
            style = MaterialTheme.typography.headlineSmall,
            color = Encre,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Carte(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        contentColor = Encre,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun Pastille(texte: String, choisie: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(50),
        color = if (choisie) Encre else Color.White,
        contentColor = if (choisie) Color.White else Encre,
        border = if (choisie) null else BorderStroke(1.5.dp, Encre.copy(alpha = 0.35f)),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(texte, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Bandeau jaune quand une nouvelle version de l'appli est disponible. */
@Composable
fun BanniereMiseAJour(vm: AppViewModel, modifier: Modifier = Modifier) {
    val version = vm.nouvelleVersion ?: return
    val progression = vm.progressionMaj
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Jaune,
        contentColor = Encre,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Nouvelle version ${version.nom} disponible", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            if (progression != null) {
                LinearProgressIndicator(
                    progress = { progression },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Encre,
                    trackColor = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Téléchargement… ${(progression * 100).toInt()} %",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
            } else {
                BoutonPrincipal(
                    texte = "Installer",
                    onClick = { vm.installerMiseAJour() },
                    couleur = Encre,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
