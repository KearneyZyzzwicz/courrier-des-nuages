package fr.courrierdesnuages.app.ui.composants

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import kotlinx.coroutines.delay

private val TAILLE_TOUCHE = 76.dp

@Composable
fun ToucheRonde(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    couleur: Color = Color.White,
    content: @Composable () -> Unit,
) {
    val vue = LocalView.current
    Surface(
        onClick = {
            vue.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = modifier.size(TAILLE_TOUCHE),
        shape = CircleShape,
        color = couleur,
        contentColor = Encre,
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** Pavé de chiffres de l'appli (jamais le clavier du téléphone). */
@Composable
fun PaveNumerique(
    onChiffre: (Char) -> Unit,
    onEffacer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val styleChiffre = MaterialTheme.typography.headlineMedium
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (ligne in listOf("123", "456", "789")) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                for (c in ligne) {
                    ToucheRonde(onClick = { onChiffre(c) }) { Text(c.toString(), style = styleChiffre) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.size(TAILLE_TOUCHE))
            ToucheRonde(onClick = { onChiffre('0') }) { Text("0", style = styleChiffre) }
            ToucheRonde(onClick = onEffacer, couleur = CielBas) {
                Icon(
                    painter = painterResource(R.drawable.ic_effacer),
                    contentDescription = "Effacer",
                    tint = Encre,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
fun PointsCode(remplis: Int, modifier: Modifier = Modifier, total: Int = 4) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (i < remplis) Encre else Color.White)
                    .border(2.5.dp, Encre, CircleShape),
            )
        }
    }
}

/**
 * Saisie d'un code à 4 chiffres. [surComplet] reçoit le code complet et renvoie vrai s'il est
 * accepté ; sinon les points tremblent et on recommence. Après 5 erreurs, il faut attendre 30 s.
 */
@Composable
fun SaisieCode(
    titre: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    entete: @Composable ColumnScope.() -> Unit = {},
    pied: @Composable ColumnScope.() -> Unit = {},
    surComplet: (String) -> Boolean,
) {
    var code by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf<String?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var echecs by remember { mutableIntStateOf(0) }
    val secousse = remember { Animatable(0f) }
    val verifier by rememberUpdatedState(surComplet)

    LaunchedEffect(enCours) {
        if (!enCours) return@LaunchedEffect
        delay(200) // le temps de voir le 4e point se remplir
        if (!verifier(code)) {
            echecs++
            erreur = "Ce n'est pas le bon code."
            for (x in listOf(16f, -14f, 10f, -6f, 3f, 0f)) secousse.animateTo(x, tween(durationMillis = 50))
            code = ""
            if (echecs >= 5) {
                erreur = "Trop d'essais. Attends 30 secondes."
                delay(30_000)
                echecs = 0
                erreur = null
            }
        }
        enCours = false
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entete()
        Text(titre, style = MaterialTheme.typography.headlineSmall, color = Encre, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = EncreDouce, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(22.dp))
        PointsCode(
            remplis = code.length,
            modifier = Modifier.graphicsLayer { translationX = secousse.value.dp.toPx() },
        )
        Box(Modifier.heightIn(min = 44.dp).padding(top = 10.dp), contentAlignment = Alignment.Center) {
            val e = erreur
            if (e != null) {
                Text(e, style = MaterialTheme.typography.bodyLarge, color = RougeFonce, textAlign = TextAlign.Center)
            }
        }
        PaveNumerique(
            onChiffre = { c ->
                if (!enCours && code.length < 4) {
                    code += c
                    erreur = null
                    if (code.length == 4) enCours = true
                }
            },
            onEffacer = { if (!enCours) code = code.dropLast(1) },
        )
        Spacer(Modifier.height(16.dp))
        pied()
    }
}

/** Choisir un nouveau code : on le tape deux fois pour éviter les fautes de frappe. */
@Composable
fun CreationCode(
    titre: String,
    titreConfirmation: String,
    onCree: (String) -> Unit,
    modifier: Modifier = Modifier,
    entete: @Composable ColumnScope.() -> Unit = {},
) {
    var premier by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var tour by remember { mutableIntStateOf(0) }
    key(tour) {
        SaisieCode(
            titre = if (premier == null) titre else titreConfirmation,
            modifier = modifier,
            message = message,
            entete = entete,
            surComplet = { code ->
                val p = premier
                when {
                    p == null -> {
                        premier = code
                        message = null
                    }
                    p == code -> onCree(code)
                    else -> {
                        premier = null
                        message = "Les deux codes ne sont pas pareils. Recommence."
                    }
                }
                tour++
                true
            },
        )
    }
}
