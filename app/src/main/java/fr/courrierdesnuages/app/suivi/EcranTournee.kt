package fr.courrierdesnuages.app.suivi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Creme
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Nuage
import fr.courrierdesnuages.app.ui.theme.Rouge
import kotlin.math.roundToInt

/** La carte des îles : le chemin parcouru depuis la rentrée, toutes matières réunies. */
@Composable
fun EcranTournee(vm: AppViewModel, profil: Profil) {
    val livraisons = profil.stats.livraisons
    val suivante = Tournee.ileAtteinte(livraisons) + 1
    val reste = Tournee.resteAvantLaSuivante(livraisons)
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Ma tournée des îles", onRetour = { vm.retourAccueil(profil) })
        Carte(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                when (livraisons) {
                    0L -> "La tournée commence !"
                    1L -> "1 livraison depuis la rentrée"
                    else -> "${nombreEnTexte(livraisons)} livraisons depuis la rentrée"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Chaque mot juste est une lettre livrée, chaque calcul juste un colis trié.",
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
            )
            Spacer(Modifier.height(10.dp))
            Text("Prochaine escale : ${Tournee.nom(suivante)}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { Tournee.avancee(livraisons) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Rouge,
                trackColor = CielBas,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Encore ${nombreEnTexte(reste)} livraison${if (reste > 1) "s" else ""} avant d'y arriver.",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
            )
        }
        Spacer(Modifier.height(8.dp))
        CarteDesIles(
            profil = profil,
            livraisons = livraisons,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/** Les îles déjà atteintes, la prochaine, et une encore dans la brume, du départ (en bas) vers le haut. */
@Composable
private fun CarteDesIles(profil: Profil, livraisons: Long, modifier: Modifier = Modifier) {
    val atteinte = Tournee.ileAtteinte(livraisons)
    val avancee = Tournee.avancee(livraisons)
    val nombre = atteinte + 3
    val densite = LocalDensity.current
    BoxWithConstraints(modifier.clipToBounds()) {
        val largeur = constraints.maxWidth.toFloat()
        val hauteurVue = constraints.maxHeight.toFloat()
        val ecart = with(densite) { 200.dp.toPx() }
        val marge = with(densite) { 140.dp.toPx() }
        val hauteur = marge * 2f + ecart * (nombre - 1)
        val rayon = largeur * 0.17f
        val centres = remember(largeur, nombre, ecart, marge) {
            List(nombre) { k -> Offset(largeur * positionX(k), hauteur - marge - k * ecart) }
        }
        val route = remember(centres) { tracerRoute(centres, ecart) }
        val position = remember(route, atteinte, avancee) {
            val mesure = PathMeasure()
            mesure.setPath(route[atteinte], false)
            mesure.getPosition(mesure.length * avancee)
        }
        val defilement = rememberScrollState(
            (position.y - hauteurVue * 0.55f).coerceIn(0f, (hauteur - hauteurVue).coerceAtLeast(0f)).roundToInt(),
        )
        val tailleAvatar = 76.dp
        val tailleAvatarPx = with(densite) { tailleAvatar.toPx() }
        val largeurEtiquette = largeur * 0.5f

        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(defilement),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(with(densite) { hauteur.toDp() }),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    for (k in 0 until nombre - 1) {
                        petitNuage(
                            Offset(largeur * (if (k % 2 == 0) 0.84f else 0.16f), centres[k].y - ecart * 0.5f),
                            largeur * 0.045f,
                            Nuage.copy(alpha = 0.9f),
                        )
                    }
                    // Toute la route en pointillés, puis le chemin déjà parcouru.
                    val pointilles = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 14.dp.toPx())),
                    )
                    for (segment in route) drawPath(segment, Color.White, style = pointilles)
                    val trait = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    for (k in 0 until atteinte) drawPath(route[k], Rouge, style = trait)
                    val parcouru = Path()
                    val mesure = PathMeasure()
                    mesure.setPath(route[atteinte], false)
                    if (avancee > 0f) mesure.getSegment(0f, mesure.length * avancee, parcouru, true)
                    drawPath(parcouru, Rouge, style = trait)
                    // Les îles par-dessus la route.
                    for (k in 0 until nombre) {
                        dessinerIle(k, centres[k], rayon, atteinte = k <= atteinte, brume = k > atteinte + 1)
                    }
                }
                for (k in 0 until nombre) {
                    val c = centres[k]
                    Text(
                        if (k > atteinte + 1) "Dans la brume…" else Tournee.nom(k),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (k <= atteinte) Encre else EncreDouce,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset { IntOffset((c.x - largeurEtiquette / 2f).roundToInt(), (c.y + rayon * 0.95f).roundToInt()) }
                            .width(with(densite) { largeurEtiquette.toDp() }),
                    )
                }
                AvatarAnime(
                    avatar = profil.avatar,
                    taille = tailleAvatar,
                    modifier = Modifier.offset {
                        IntOffset(
                            (position.x - tailleAvatarPx / 2f).roundToInt(),
                            (position.y - tailleAvatarPx * 0.8f).roundToInt(),
                        )
                    },
                )
            }
        }
    }
}

/** Les îles vont de gauche à droite en zigzag ; le départ est au milieu. */
private fun positionX(k: Int): Float {
    if (k == 0) return 0.5f
    val zigzag = if (k % 2 == 1) 0.68f else 0.32f
    val decalage = ((k * 7919) % 9 - 4) * 0.01f
    return zigzag + decalage
}

private fun tracerRoute(centres: List<Offset>, ecart: Float): List<Path> = (0 until centres.size - 1).map { k ->
    val a = centres[k]
    val b = centres[k + 1]
    Path().apply {
        moveTo(a.x, a.y)
        cubicTo(a.x, a.y - ecart * 0.6f, b.x, b.y + ecart * 0.6f, b.x, b.y)
    }
}

private val HERBES = listOf(
    Color(0xFF7BC67E),
    Color(0xFFA8D86E),
    Color(0xFFF2A7C3),
    Color(0xFF8ED1C6),
    Color(0xFFF4D06F),
    Color(0xFFB9A7E8),
)
private val ROCHE = Color(0xFFC0916A)
private val ROCHE_FONCEE = Color(0xFF94694A)
private val FEUILLAGE = Color(0xFF4E9F5B)
private val BOIS = Color(0xFF8B5E3C)

/** Une île volante : dessus herbeux, dessous rocheux, et un petit décor. Pas encore atteinte, elle est pâle. */
private fun DrawScope.dessinerIle(k: Int, c: Offset, r: Float, atteinte: Boolean, brume: Boolean) {
    if (!atteinte) {
        val couche = Paint().apply { alpha = 0.55f }
        drawIntoCanvas { it.saveLayer(Rect(c.x - r * 1.6f, c.y - r * 1.3f, c.x + r * 1.6f, c.y + r * 1.1f), couche) }
    }
    // Des nuages sous l'île, pour qu'elle flotte.
    petitNuage(Offset(c.x - r * 0.75f, c.y + r * 0.42f), r * 0.2f, Nuage)
    petitNuage(Offset(c.x + r * 0.7f, c.y + r * 0.5f), r * 0.17f, Nuage)
    val dessous = Path().apply {
        moveTo(c.x - r, c.y)
        cubicTo(c.x - r * 0.85f, c.y + r * 0.45f, c.x - r * 0.3f, c.y + r * 0.5f, c.x, c.y + r * 0.85f)
        cubicTo(c.x + r * 0.3f, c.y + r * 0.5f, c.x + r * 0.85f, c.y + r * 0.45f, c.x + r, c.y)
        close()
    }
    drawPath(dessous, ROCHE)
    drawLine(ROCHE_FONCEE, Offset(c.x - r * 0.6f, c.y + r * 0.22f), Offset(c.x + r * 0.45f, c.y + r * 0.28f), strokeWidth = r * 0.05f, cap = StrokeCap.Round)
    drawLine(ROCHE_FONCEE, Offset(c.x - r * 0.25f, c.y + r * 0.45f), Offset(c.x + r * 0.2f, c.y + r * 0.5f), strokeWidth = r * 0.045f, cap = StrokeCap.Round)
    drawOval(HERBES[k % HERBES.size], topLeft = Offset(c.x - r, c.y - r * 0.28f), size = Size(2f * r, r * 0.56f))
    drawOval(Color.White.copy(alpha = 0.3f), topLeft = Offset(c.x - r * 0.75f, c.y - r * 0.22f), size = Size(r * 0.9f, r * 0.16f))
    // Le décor est sur la gauche de l'île : le facteur se pose au milieu.
    val pied = Offset(c.x - r * 0.6f, c.y - r * 0.04f)
    when {
        k == 0 -> bureauDePoste(pied, r)
        k % 4 == 1 -> arbres(pied, r)
        k % 4 == 2 -> maison(pied, r)
        k % 4 == 3 -> phare(pied, r)
        else -> boiteAuxLettres(pied, r)
    }
    if (atteinte && k > 0) drapeau(Offset(c.x + r * 0.6f, c.y - r * 0.02f), r)
    if (!atteinte) drawIntoCanvas { it.restore() }
    if (brume) {
        petitNuage(Offset(c.x - r * 0.2f, c.y + r * 0.05f), r * 0.42f, Nuage.copy(alpha = 0.85f))
        petitNuage(Offset(c.x + r * 0.5f, c.y + r * 0.32f), r * 0.3f, Nuage.copy(alpha = 0.8f))
    }
}

private fun DrawScope.maison(pied: Offset, r: Float) {
    val l = r * 0.4f
    val h = r * 0.3f
    drawRect(Creme, topLeft = Offset(pied.x - l / 2f, pied.y - h), size = Size(l, h))
    val toit = Path().apply {
        moveTo(pied.x - l * 0.65f, pied.y - h)
        lineTo(pied.x + l * 0.65f, pied.y - h)
        lineTo(pied.x, pied.y - h - r * 0.26f)
        close()
    }
    drawPath(toit, Rouge)
    drawRect(BOIS, topLeft = Offset(pied.x - l * 0.12f, pied.y - h * 0.55f), size = Size(l * 0.24f, h * 0.55f))
    drawRect(Jaune, topLeft = Offset(pied.x + l * 0.2f, pied.y - h * 0.8f), size = Size(l * 0.18f, l * 0.18f))
}

private fun DrawScope.bureauDePoste(pied: Offset, r: Float) {
    val l = r * 0.55f
    val h = r * 0.36f
    drawRect(Creme, topLeft = Offset(pied.x - l / 2f, pied.y - h), size = Size(l, h))
    val toit = Path().apply {
        moveTo(pied.x - l * 0.6f, pied.y - h)
        lineTo(pied.x + l * 0.6f, pied.y - h)
        lineTo(pied.x, pied.y - h - r * 0.28f)
        close()
    }
    drawPath(toit, Encre)
    // L'enveloppe, enseigne du Courrier des Nuages.
    val e = Size(l * 0.42f, l * 0.28f)
    val coin = Offset(pied.x - e.width / 2f, pied.y - h * 0.82f)
    drawRect(Color.White, topLeft = coin, size = e)
    drawRect(Rouge, topLeft = coin, size = e, style = Stroke(width = r * 0.025f))
    val rabat = Path().apply {
        moveTo(coin.x, coin.y)
        lineTo(coin.x + e.width / 2f, coin.y + e.height * 0.6f)
        lineTo(coin.x + e.width, coin.y)
    }
    drawPath(rabat, Rouge, style = Stroke(width = r * 0.025f))
    drawRect(BOIS, topLeft = Offset(pied.x - l * 0.1f, pied.y - h * 0.32f), size = Size(l * 0.2f, h * 0.32f))
}

private fun DrawScope.arbres(pied: Offset, r: Float) {
    for ((dx, taille) in listOf(-0.16f to 1f, 0.14f to 0.75f)) {
        val x = pied.x + r * dx
        val t = r * taille
        drawRect(BOIS, topLeft = Offset(x - t * 0.04f, pied.y - t * 0.3f), size = Size(t * 0.08f, t * 0.3f))
        val feuillage = Path().apply {
            addOval(Rect(center = Offset(x, pied.y - t * 0.42f), radius = t * 0.2f))
            addOval(Rect(center = Offset(x - t * 0.12f, pied.y - t * 0.32f), radius = t * 0.14f))
            addOval(Rect(center = Offset(x + t * 0.12f, pied.y - t * 0.32f), radius = t * 0.14f))
        }
        drawPath(feuillage, FEUILLAGE)
    }
}

private fun DrawScope.phare(pied: Offset, r: Float) {
    val h = r * 0.62f
    val bas = r * 0.11f
    val haut = r * 0.07f
    val tour = Path().apply {
        moveTo(pied.x - bas, pied.y)
        lineTo(pied.x + bas, pied.y)
        lineTo(pied.x + haut, pied.y - h)
        lineTo(pied.x - haut, pied.y - h)
        close()
    }
    drawPath(tour, Color.White)
    for (i in 0 until 3) {
        val y1 = pied.y - h * (0.18f + i * 0.3f)
        val y2 = y1 - h * 0.13f
        val l1 = bas - (bas - haut) * ((pied.y - y1) / h)
        val l2 = bas - (bas - haut) * ((pied.y - y2) / h)
        val bande = Path().apply {
            moveTo(pied.x - l1, y1)
            lineTo(pied.x + l1, y1)
            lineTo(pied.x + l2, y2)
            lineTo(pied.x - l2, y2)
            close()
        }
        drawPath(bande, Rouge)
    }
    drawCircle(Jaune, radius = haut * 1.1f, center = Offset(pied.x, pied.y - h - haut))
    val chapeau = Path().apply {
        moveTo(pied.x - haut * 1.4f, pied.y - h - haut * 1.8f)
        lineTo(pied.x + haut * 1.4f, pied.y - h - haut * 1.8f)
        lineTo(pied.x, pied.y - h - haut * 3.4f)
        close()
    }
    drawPath(chapeau, Encre)
}

private fun DrawScope.boiteAuxLettres(pied: Offset, r: Float) {
    drawRect(BOIS, topLeft = Offset(pied.x - r * 0.025f, pied.y - r * 0.3f), size = Size(r * 0.05f, r * 0.3f))
    val l = r * 0.3f
    val h = r * 0.2f
    drawRoundRect(Rouge, topLeft = Offset(pied.x - l / 2f, pied.y - r * 0.3f - h), size = Size(l, h), cornerRadius = CornerRadius(h * 0.45f))
    drawRect(Encre, topLeft = Offset(pied.x - l * 0.3f, pied.y - r * 0.3f - h * 0.62f), size = Size(l * 0.6f, h * 0.12f))
    drawRect(Jaune, topLeft = Offset(pied.x + l / 2f, pied.y - r * 0.3f - h * 1.25f), size = Size(r * 0.03f, h * 0.7f))
    drawRect(Jaune, topLeft = Offset(pied.x + l / 2f, pied.y - r * 0.3f - h * 1.25f), size = Size(r * 0.1f, h * 0.25f))
}

/** Le drapeau planté sur chaque île déjà visitée. */
private fun DrawScope.drapeau(pied: Offset, r: Float) {
    val h = r * 0.42f
    drawLine(Encre, pied, Offset(pied.x, pied.y - h), strokeWidth = r * 0.03f, cap = StrokeCap.Round)
    val toile = Path().apply {
        moveTo(pied.x, pied.y - h)
        lineTo(pied.x + r * 0.24f, pied.y - h * 0.85f)
        lineTo(pied.x, pied.y - h * 0.68f)
        close()
    }
    drawPath(toile, Rouge)
}

/** Un nuage d'un seul tenant (les bulles qui se chevauchent ne foncent pas la couleur). */
private fun DrawScope.petitNuage(c: Offset, r: Float, couleur: Color) {
    val nuage = Path().apply {
        addOval(Rect(center = c, radius = r))
        addOval(Rect(center = Offset(c.x - r * 1.0f, c.y + r * 0.28f), radius = r * 0.75f))
        addOval(Rect(center = Offset(c.x + r * 0.95f, c.y + r * 0.22f), radius = r * 0.8f))
        addOval(Rect(offset = Offset(c.x - r * 1.7f, c.y + r * 0.15f), size = Size(r * 3.4f, r * 0.9f)))
    }
    drawPath(nuage, couleur)
}
