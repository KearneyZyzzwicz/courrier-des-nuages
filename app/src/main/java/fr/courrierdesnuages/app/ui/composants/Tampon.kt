package fr.courrierdesnuages.app.ui.composants

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.ui.theme.Rouge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Le coup de tampon « BRAVO ! » quand la réponse est juste (dictée, tri des colis…). */
@Composable
fun TamponBravo(texte: String, sons: Sons, onFini: () -> Unit, modifier: Modifier = Modifier) {
    val fini by rememberUpdatedState(onFini)
    val echelle = remember { Animatable(1.9f) }
    val opacite = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { opacite.animateTo(1f, tween(110)) }
        echelle.animateTo(1f, tween(230, easing = FastOutLinearInEasing))
        sons.coupDeTampon()
        delay(950)
        fini()
    }
    Box(
        modifier
            .graphicsLayer {
                scaleX = echelle.value
                scaleY = echelle.value
                alpha = opacite.value
                rotationZ = -9f
            }
            .border(4.dp, Rouge, RoundedCornerShape(16.dp))
            .padding(horizontal = 26.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRAVO !", style = MaterialTheme.typography.displaySmall, color = Rouge)
            Text(texte, style = MaterialTheme.typography.titleLarge, color = Rouge)
        }
    }
}
