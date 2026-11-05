package fr.courrierdesnuages.app.ui.composants

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.CielHaut
import fr.courrierdesnuages.app.ui.theme.Nuage

/** Le ciel de fond, avec des nuages qui passent lentement. */
@Composable
fun FondCiel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "ciel")
    val avance = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 90_000, easing = LinearEasing)),
        label = "nuages",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CielHaut, CielBas)))
            .drawBehind { dessinerNuages(avance.value) },
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), content = content)
    }
}

private class NuageDuFond(val depart: Float, val hauteur: Float, val rayon: Float, val vitesse: Int)

private val NUAGES = listOf(
    NuageDuFond(depart = 0.05f, hauteur = 0.07f, rayon = 0.065f, vitesse = 1),
    NuageDuFond(depart = 0.55f, hauteur = 0.21f, rayon = 0.05f, vitesse = 2),
    NuageDuFond(depart = 0.30f, hauteur = 0.47f, rayon = 0.045f, vitesse = 1),
    NuageDuFond(depart = 0.80f, hauteur = 0.70f, rayon = 0.06f, vitesse = 2),
    NuageDuFond(depart = 0.15f, hauteur = 0.90f, rayon = 0.05f, vitesse = 1),
)

private fun DrawScope.dessinerNuages(avance: Float) {
    val largeur = size.width
    for (n in NUAGES) {
        val r = n.rayon * largeur
        // Chaque nuage traverse l'écran puis revient par la gauche.
        val parcours = largeur + 6f * r
        val x = ((n.depart + avance * n.vitesse) % 1f) * parcours - 3f * r
        nuage(x, n.hauteur * size.height, r)
    }
}

private fun DrawScope.nuage(cx: Float, cy: Float, r: Float) {
    drawCircle(Nuage, r, Offset(cx, cy))
    drawCircle(Nuage, r * 0.72f, Offset(cx - r * 1.05f, cy + r * 0.3f))
    drawCircle(Nuage, r * 0.8f, Offset(cx + r * 1.0f, cy + r * 0.22f))
    drawRoundRect(
        color = Nuage,
        topLeft = Offset(cx - r * 1.75f, cy + r * 0.2f),
        size = Size(r * 3.6f, r * 0.85f),
        cornerRadius = CornerRadius(r * 0.42f),
    )
}

@Composable
fun ImageAvatar(avatar: Avatar, taille: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(avatar.image),
        contentDescription = "${avatar.nom} ${avatar.engin}",
        modifier = modifier.size(taille),
    )
}

/** L'avatar flotte doucement, comme porté par le vent. */
@Composable
fun AvatarAnime(avatar: Avatar, taille: Dp, modifier: Modifier = Modifier, decalage: Int = 0) {
    val transition = rememberInfiniteTransition(label = "avatar")
    val t = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(decalage),
        ),
        label = "flotte",
    )
    ImageAvatar(
        avatar = avatar,
        taille = taille,
        modifier = modifier.graphicsLayer {
            translationY = t.value * taille.toPx() * 0.035f
            rotationZ = t.value * 2.5f
        },
    )
}
