package fr.courrierdesnuages.app.dictee

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Vert

val ACCENTS_FRANCAIS = listOf("é", "è", "ê", "à", "ç", "ù")
val ACCENTS_FRANCAIS_PLUS = listOf("â", "ë", "î", "ï", "ô", "û", "œ")

private val RANGEE_1 = "azertyuiop".map { it.toString() }
private val RANGEE_2 = "qsdfghjklm".map { it.toString() }
private val RANGEE_3 = "wxcvbn".map { it.toString() } + listOf("'", "-")

private val styleTouche = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 22.sp)
private val styleSpeciale = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

/**
 * Le clavier AZERTY de l'appli : jamais celui du téléphone, qui proposerait la réponse.
 * Maj met la lettre suivante en majuscule. La touche « âîô » affiche les accents plus rares.
 */
@Composable
fun ClavierAzerty(
    onTexte: (String) -> Unit,
    onEffacer: () -> Unit,
    onValider: () -> Unit,
    modifier: Modifier = Modifier,
    actif: Boolean = true,
    accents: List<String> = ACCENTS_FRANCAIS,
    accentsPlus: List<String> = ACCENTS_FRANCAIS_PLUS,
) {
    var majuscule by remember { mutableStateOf(false) }
    var plus by remember { mutableStateOf(false) }

    fun ecrire(t: String) {
        if (!actif) return
        onTexte(if (majuscule) t.uppercase() else t)
        majuscule = false
    }

    fun libelle(t: String) = if (majuscule) t.uppercase() else t

    Column(
        modifier
            .fillMaxWidth()
            .alpha(if (actif) 1f else 0.45f)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Rangee {
            for (t in RANGEE_1) Touche(libelle(t), onClick = { ecrire(t) })
        }
        Rangee {
            for (t in RANGEE_2) Touche(libelle(t), onClick = { ecrire(t) })
        }
        Rangee {
            Touche(
                texte = "Maj",
                poids = 1.5f,
                style = styleSpeciale,
                couleur = if (majuscule) Jaune else CielBas,
                onClick = { if (actif) majuscule = !majuscule },
            )
            for (t in RANGEE_3) Touche(libelle(t), onClick = { ecrire(t) })
            ToucheIcone(poids = 1.5f, onClick = { if (actif) onEffacer() })
        }
        Rangee {
            val visibles = if (plus && accentsPlus.isNotEmpty()) accentsPlus else accents
            for (t in visibles) Touche(libelle(t), couleur = Color(0xFFFFF4D6), onClick = { ecrire(t) })
            if (accentsPlus.isNotEmpty()) {
                Touche(
                    texte = if (plus) "éèê" else "âîô",
                    poids = 1.6f,
                    style = styleSpeciale,
                    couleur = CielBas,
                    onClick = { if (actif) plus = !plus },
                )
            }
        }
        Rangee {
            Touche(texte = "espace", poids = 5f, style = styleSpeciale, couleur = Color.White, onClick = { ecrire(" ") })
            Touche(
                texte = "Valider",
                poids = 3f,
                style = styleSpeciale.copy(color = Color.White),
                couleur = Vert,
                onClick = { if (actif) onValider() },
            )
        }
    }
}

@Composable
private fun Rangee(contenu: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = contenu,
    )
}

@Composable
private fun RowScope.Touche(
    texte: String,
    onClick: () -> Unit,
    poids: Float = 1f,
    style: TextStyle = styleTouche,
    couleur: Color = Color.White,
) {
    val vue = LocalView.current
    Surface(
        onClick = {
            vue.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = Modifier
            .weight(poids)
            .fillMaxSize(),
        shape = RoundedCornerShape(10.dp),
        color = couleur,
        contentColor = Encre,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texte, style = style, maxLines = 1)
        }
    }
}

@Composable
private fun RowScope.ToucheIcone(poids: Float, onClick: () -> Unit) {
    val vue = LocalView.current
    Surface(
        onClick = {
            vue.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = Modifier
            .weight(poids)
            .fillMaxSize(),
        shape = RoundedCornerShape(10.dp),
        color = CielBas,
        contentColor = Encre,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_effacer),
                contentDescription = "Effacer",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
