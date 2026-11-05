package fr.courrierdesnuages.app.dictee

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.Creme
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Rouge
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val LARGEUR_AVION = 116.dp
private val HAUTEUR_AVION = 58.dp
private val CORDE = 30.dp
private val MARGE_BANDEROLE = 22.dp
private val TAILLE_MOT = 38.sp

/** Durées du passage : l'avion arrive vite, ralentit au milieu pendant 1 s, puis repart vite. */
private const val ARRIVEE_MS = 700
private const val LECTURE_MS = 1000
private const val DEPART_MS = 700

/**
 * L'avion traverse le ciel de droite à gauche en tirant la banderole du mot. Il ne repasse pas :
 * un nouveau [passage] relance l'animation.
 */
@Composable
fun PassageAvion(
    mot: String,
    passage: Int,
    onDebut: () -> Unit,
    onFini: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val debut by rememberUpdatedState(onDebut)
    val fini by rememberUpdatedState(onFini)
    val densite = LocalDensity.current
    val mesureur = rememberTextMeasurer()

    BoxWithConstraints(modifier.fillMaxWidth().height(190.dp).clipToBounds()) {
        val largeur = constraints.maxWidth.toFloat()
        val styleMot = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = TAILLE_MOT)
        val largeurTexte = remember(mot) {
            mesureur.measure(mot, style = styleMot, softWrap = false, maxLines = 1).size.width.toFloat().coerceAtLeast(1f)
        }
        val marges = with(densite) { (MARGE_BANDEROLE * 2).toPx() }
        // Un mot très long est écrit un peu plus petit pour que la banderole tienne dans l'écran.
        val echelle = min(1f, (largeur * 0.9f - marges) / largeurTexte)
        val tailleMot = TAILLE_MOT * echelle
        val largeurBanderole = largeurTexte * echelle + marges
        val avantBanderole = with(densite) { (LARGEUR_AVION + CORDE).toPx() }

        val xDepart = largeur
        val xMilieu = largeur / 2f - avantBanderole - largeurBanderole / 2f
        val xFin = -(avantBanderole + largeurBanderole) - 20f
        val derive = largeur * 0.03f
        val x = remember(passage) { Animatable(xDepart) }

        LaunchedEffect(passage) {
            debut()
            x.snapTo(xDepart)
            x.animateTo(xMilieu + derive, tween(ARRIVEE_MS, easing = LinearOutSlowInEasing))
            x.animateTo(xMilieu - derive, tween(LECTURE_MS, easing = LinearEasing))
            x.animateTo(xFin, tween(DEPART_MS, easing = FastOutLinearInEasing))
            fini()
        }

        val vol = rememberInfiniteTransition(label = "vol")
        val roulis = vol.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
            label = "roulis",
        )

        Row(
            Modifier
                .align(Alignment.CenterStart)
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .graphicsLayer { translationY = roulis.value * 5.dp.toPx() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.avion),
                contentDescription = null,
                modifier = Modifier.size(width = LARGEUR_AVION, height = HAUTEUR_AVION),
            )
            Spacer(Modifier.width(CORDE))
            Banderole(mot, tailleMot, CORDE)
        }
    }
}

@Composable
private fun Banderole(mot: String, taille: TextUnit, corde: Dp) {
    val vent = rememberInfiniteTransition(label = "vent")
    val phase = vent.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "vague",
    )
    Box(
        Modifier
            .drawBehind {
                val w = size.width
                val h = size.height
                val a = h * 0.07f
                val p = phase.value
                fun onde(x: Float) = a * sin(p + x / w * 3f * PI.toFloat())
                val pas = w / 28f
                val chemin = Path().apply {
                    moveTo(0f, onde(0f))
                    var x = pas
                    while (x < w) {
                        lineTo(x, onde(x))
                        x += pas
                    }
                    lineTo(w, onde(w))
                    lineTo(w, h + onde(w))
                    x = w - pas
                    while (x > 0f) {
                        lineTo(x, h + onde(x))
                        x -= pas
                    }
                    lineTo(0f, h + onde(0f))
                    close()
                }
                drawPath(chemin, Creme)
                drawPath(chemin, Rouge, style = Stroke(width = 2.5.dp.toPx()))
                // Les deux cordes qui relient la banderole à la queue de l'avion.
                val queue = Offset(-(corde + 4.dp).toPx(), h / 2f)
                drawLine(EncreDouce, queue, Offset(0f, onde(0f) + 2f), strokeWidth = 1.6.dp.toPx())
                drawLine(EncreDouce, queue, Offset(0f, h + onde(0f) - 2f), strokeWidth = 1.6.dp.toPx())
            }
            .padding(horizontal = MARGE_BANDEROLE, vertical = 12.dp),
    ) {
        Text(
            mot,
            style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = taille, color = Encre),
            maxLines = 1,
            softWrap = false,
        )
    }
}
