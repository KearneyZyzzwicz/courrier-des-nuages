package fr.courrierdesnuages.app.cartes

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.Fredoka
import kotlin.math.abs

/** La mer autour des cartes. */
val MerDesCartes = Color(0xFFBFE3F2)

/** Une terre pas encore gagnée. */
val TerreVierge = Color(0xFFF7F1E3)

/** Les atlas de l'appli, lus une seule fois dans res/raw. */
object Atlases {
    @Volatile
    private var france: Atlas? = null

    fun france(context: Context): Atlas = france ?: synchronized(this) {
        france ?: Atlas.lire(
            context.resources.openRawResource(R.raw.departements).bufferedReader().use { it.readText() },
        ).also { france = it }
    }
}

@Composable
fun rememberAtlasFrance(): Atlas {
    val contexte = LocalContext.current
    return remember { Atlases.france(contexte.applicationContext) }
}

/** Le contour d'une forme, en unités de carte. */
fun Forme.chemin(): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    for (a in anneaux) {
        moveTo(a[0], a[1])
        var i = 2
        while (i < a.size) {
            lineTo(a[i], a[i + 1])
            i += 2
        }
        close()
    }
}

/** Une épingle plantée sur la carte (position en unités de carte). */
data class Repere(val x: Float, val y: Float, val couleur: Color)

/** Le zoom d'une carte : 1 = la carte entière ; le décalage est en pixels. */
@Stable
class EtatZoom {
    var echelle by mutableFloatStateOf(1f)
        private set
    var decalage by mutableStateOf(Offset.Zero)
        private set

    fun transformer(centre: Offset, deplacement: Offset, zoom: Float, taille: IntSize) {
        val nouvelle = (echelle * zoom).coerceIn(1f, ECHELLE_MAX)
        val facteur = nouvelle / echelle
        val d = centre - (centre - decalage) * facteur + deplacement
        echelle = nouvelle
        decalage = Offset(
            d.x.coerceIn(taille.width * (1 - echelle), 0f),
            d.y.coerceIn(taille.height * (1 - echelle), 0f),
        )
    }

    fun zoomer(facteur: Float, taille: IntSize) =
        transformer(Offset(taille.width / 2f, taille.height / 2f), Offset.Zero, facteur, taille)

    companion object {
        const val ECHELLE_MAX = 8f
    }
}

/**
 * Pincer pour zoomer ; glisser un doigt déplace la carte une fois zoomée (sinon, c'est l'écran qui
 * défile, comme d'habitude).
 */
private suspend fun PointerInputScope.detecterZoom(etat: EtatZoom) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var zoomCumule = 1f
        var panCumule = Offset.Zero
        var parti = false
        do {
            val evenement = awaitPointerEvent()
            if (evenement.changes.any { it.isConsumed }) break
            val zoom = evenement.calculateZoom()
            val pan = evenement.calculatePan()
            if (!parti) {
                zoomCumule *= zoom
                panCumule += pan
                val mouvementZoom = abs(1 - zoomCumule) * evenement.calculateCentroidSize(useCurrent = false)
                if (mouvementZoom > viewConfiguration.touchSlop || panCumule.getDistance() > viewConfiguration.touchSlop) parti = true
            }
            val doigts = evenement.changes.count { it.pressed }
            if (parti && (doigts >= 2 || etat.echelle > 1.01f) && (zoom != 1f || pan != Offset.Zero)) {
                etat.transformer(evenement.calculateCentroid(useCurrent = false), pan, zoom, size)
                evenement.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (evenement.changes.any { it.pressed })
    }
}

/**
 * Une carte faite de formes (départements, pays), que l'on peut agrandir. [remplissage] donne la
 * couleur de chaque forme, [contour] un contour épais pour la mettre en valeur. [surToucher] reçoit
 * le point touché en unités de carte, avec la marge du doigt et la taille d'une forme trop petite
 * pour être touchée facilement, à ce niveau de zoom.
 */
@Composable
fun CarteAtlas(
    atlas: Atlas,
    remplissage: (String) -> Color,
    modifier: Modifier = Modifier,
    zoom: EtatZoom = remember { EtatZoom() },
    contour: (String) -> Color? = { null },
    reperes: List<Repere> = emptyList(),
    etiquettes: Map<String, String> = emptyMap(),
    trait: Pair<Offset, Offset>? = null,
    surToucher: ((x: Float, y: Float, rayon: Float, petite: Float) -> Unit)? = null,
) {
    val chemins = remember(atlas) { atlas.formes.mapValues { (_, f) -> f.chemin() } }
    val mesureur = rememberTextMeasurer()
    val styleEtiquette = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Encre)
    val textes = remember(etiquettes) { etiquettes.mapValues { (_, t) -> mesureur.measure(t, styleEtiquette) } }
    val rappel by rememberUpdatedState(surToucher)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(atlas.largeur / atlas.hauteur)
            .clip(RoundedCornerShape(16.dp))
            .background(MerDesCartes),
    ) {
        val taille = IntSize(constraints.maxWidth, (constraints.maxWidth * atlas.hauteur / atlas.largeur).toInt())
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(zoom) { detecterZoom(zoom) }
                .pointerInput(atlas, zoom) {
                    detectTapGestures { p ->
                        val r = rappel ?: return@detectTapGestures
                        val k = size.width / atlas.largeur * zoom.echelle
                        r((p.x - zoom.decalage.x) / k, (p.y - zoom.decalage.y) / k, 14.dp.toPx() / k, 28.dp.toPx() / k)
                    }
                },
        ) {
            val k = size.width / atlas.largeur * zoom.echelle
            val d = zoom.decalage
            withTransform({
                translate(d.x, d.y)
                scale(k, k, pivot = Offset.Zero)
            }) {
                val fin = 0.8.dp.toPx() / k
                for ((id, chemin) in chemins) {
                    drawPath(chemin, remplissage(id))
                    drawPath(chemin, Color.White, style = Stroke(width = fin, join = StrokeJoin.Round))
                }
                for ((id, chemin) in chemins) {
                    val c = contour(id) ?: continue
                    drawPath(chemin, c, style = Stroke(width = 2.5.dp.toPx() / k, join = StrokeJoin.Round))
                }
            }
            fun ecran(x: Float, y: Float) = Offset(d.x + x * k, d.y + y * k)
            if (trait != null) {
                drawLine(
                    Encre.copy(alpha = 0.55f),
                    ecran(trait.first.x, trait.first.y),
                    ecran(trait.second.x, trait.second.y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            for ((id, texte) in textes) {
                val f = atlas.formes[id] ?: continue
                // Seulement si le nom tient dans la forme, à ce niveau de zoom.
                if (texte.size.width > f.largeur * k * 0.8f || texte.size.height > f.hauteur * k * 0.6f) continue
                val c = ecran(f.etiquetteX, f.etiquetteY)
                drawText(texte, topLeft = Offset(c.x - texte.size.width / 2f, c.y - texte.size.height / 2f))
            }
            val r = 6.dp.toPx()
            for (e in reperes) {
                val c = ecran(e.x, e.y)
                drawCircle(Color.Black.copy(alpha = 0.18f), r * 1.4f, c + Offset(0f, 1.5.dp.toPx()))
                drawCircle(Color.White, r * 1.4f, c)
                drawCircle(e.couleur, r, c)
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
            BoutonDeZoom("+") { zoom.zoomer(1.8f, taille) }
            if (zoom.echelle > 1.01f) {
                Spacer(Modifier.height(6.dp))
                BoutonDeZoom("−") { zoom.zoomer(1 / 1.8f, taille) }
            }
        }
    }
}

@Composable
private fun BoutonDeZoom(texte: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        contentColor = Encre,
        shadowElevation = 2.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texte, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 22.sp))
        }
    }
}
