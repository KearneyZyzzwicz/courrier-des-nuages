package fr.courrierdesnuages.app.cartes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.italien.CarteGagnee
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.theme.Creme
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import kotlin.math.hypot

private val Rate = Color(0xFFB8BEC9)
private val EncreDuTampon = Color(0xFF3B4A6B)

/** Ce que les écrans d'album savent d'une carte : la forme où elle se place, sa ville, sa couleur. */
data class Lieu(
    val id: String,
    val forme: String,
    val latitude: Double,
    val longitude: Double,
    val couleur: Color,
    /** Sous la vignette de l'album : « 29 · Finistère », « Londres ». */
    val nomCourt: String,
)

private const val AIDE_ZOOM = "Pince la carte avec deux doigts, ou touche +, pour l'agrandir."

/**
 * L'enfant place sa nouvelle carte en touchant la bonne forme (département, pays). S'il se trompe,
 * l'appli lui montre le bon endroit. [deja] : les cartes déjà placées, en couleurs pâles.
 */
@Composable
fun EcranPlacerCarte(
    atlas: Atlas,
    lieu: Lieu,
    deja: List<Lieu>,
    fondDe: (String) -> Color,
    question: String,
    bravo: String,
    presque: String,
    onPlacee: (Boolean) -> Unit,
    onAlbum: () -> Unit,
    onRetour: () -> Unit,
) {
    val zoom = remember { EtatZoom() }
    var touche by remember { mutableStateOf<String?>(null) }
    var point by remember { mutableStateOf<Offset?>(null) }
    var bienPlacee by remember { mutableStateOf<Boolean?>(null) }
    val formesDeja = deja.associate { it.forme to it.couleur }

    LaunchedEffect(bienPlacee) {
        val b = bienPlacee ?: return@LaunchedEffect
        onPlacee(b)
    }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Place ta carte", onRetour = onRetour)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (bienPlacee) {
                    null -> question
                    true -> bravo
                    false -> presque
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (bienPlacee) {
                    true -> Vert
                    false -> RougeFonce
                    null -> Encre
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val (vx, vy) = atlas.position(lieu.latitude, lieu.longitude)
            val p = point
            CarteAtlas(
                atlas = atlas,
                zoom = zoom,
                remplissage = { id ->
                    when {
                        id == lieu.forme && bienPlacee != null -> lieu.couleur
                        id == touche && bienPlacee == false -> Rate
                        id in formesDeja -> formesDeja.getValue(id).copy(alpha = 0.45f)
                        else -> fondDe(id)
                    }
                },
                contour = { id -> if (id == lieu.forme && bienPlacee == false) RougeFonce else null },
                reperes = if (bienPlacee != null) listOf(Repere(vx, vy, Rouge)) else emptyList(),
                trait = if (bienPlacee == false && p != null) p to Offset(vx, vy) else null,
                surToucher = if (bienPlacee == null) { x, y, rayon, petite ->
                    point = Offset(x, y)
                    touche = atlas.formeEn(x, y)?.id
                    bienPlacee = atlas.bienPlace(lieu.forme, x, y, rayon, petite)
                } else {
                    null
                },
            )
            Text(
                AIDE_ZOOM,
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            if (bienPlacee != null) {
                BoutonPrincipal(texte = "Voir mon album", onClick = onAlbum, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Un album sur une carte : les formes se colorient au fil des cartes placées, chaque ville a son
 * épingle, et les cartes gagnées sont rangées dessous. Toucher une forme coloriée ouvre sa carte ;
 * toucher une autre forme dit son nom.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranAlbumAtlas(
    titre: String,
    atlas: Atlas,
    gagnees: List<Pair<Lieu, CarteGagnee>>,
    total: Int,
    texteVide: String,
    texteComplet: String,
    texteBoutonPlacer: (Lieu) -> String,
    fondDe: (String) -> Color,
    nomDeForme: (String) -> String?,
    vignette: @Composable (Lieu, Modifier) -> Unit,
    onOuvrir: (Lieu) -> Unit,
    onPlacer: (Lieu) -> Unit,
    onRetour: () -> Unit,
    etiquettes: Map<String, String> = emptyMap(),
    credit: String? = null,
) {
    val zoom = remember { EtatZoom() }
    val placees = gagnees.filter { it.second.epinglee }.map { it.first }
    val couleurs = placees.associate { it.forme to it.couleur }
    var touchee by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour(titre, onRetour = onRetour)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (gagnees.size) {
                    0 -> texteVide
                    total -> texteComplet
                    else -> "${gagnees.size} carte${if (gagnees.size > 1) "s" else ""} sur $total. Touche la carte pour revoir une carte postale."
                },
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            for ((lieu, _) in gagnees.filter { !it.second.epinglee }) {
                BoutonPrincipal(
                    texte = texteBoutonPlacer(lieu),
                    onClick = { onPlacer(lieu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            CarteAtlas(
                atlas = atlas,
                zoom = zoom,
                remplissage = { id -> couleurs[id] ?: fondDe(id) },
                contour = { id -> if (id == touchee && id !in couleurs) EncreDouce else null },
                reperes = placees.map { l ->
                    val (x, y) = atlas.position(l.latitude, l.longitude)
                    Repere(x, y, Rouge)
                },
                etiquettes = etiquettes,
                surToucher = { x, y, rayon, _ ->
                    // D'abord l'épingle la plus proche, puis la carte de la forme touchée.
                    val proche = placees.minByOrNull { l ->
                        val (px, py) = atlas.position(l.latitude, l.longitude)
                        hypot(px - x, py - y)
                    }
                    val distance = proche?.let { l ->
                        val (px, py) = atlas.position(l.latitude, l.longitude)
                        hypot(px - x, py - y)
                    }
                    val forme = atlas.formeEn(x, y)?.id
                        ?: atlas.formes.values.filter { it.distance(x, y) <= rayon }.minByOrNull { it.distance(x, y) }?.id
                    val dansLaForme = placees.filter { it.forme == forme }.minByOrNull { l ->
                        val (px, py) = atlas.position(l.latitude, l.longitude)
                        hypot(px - x, py - y)
                    }
                    when {
                        proche != null && distance != null && distance <= rayon -> onOuvrir(proche)
                        dansLaForme != null -> onOuvrir(dansLaForme)
                        else -> touchee = forme
                    }
                },
            )
            val nom = touchee?.let { nomDeForme(it) }
            Text(
                nom ?: AIDE_ZOOM,
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for ((lieu, _) in gagnees) {
                    Column(
                        Modifier
                            .width(150.dp)
                            .clickable { onOuvrir(lieu) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        vignette(lieu, Modifier.border(BorderStroke(2.dp, Color.White), RoundedCornerShape(10.dp)))
                        Text(lieu.nomCourt, style = MaterialTheme.typography.labelMedium, color = Encre, maxLines = 1)
                    }
                }
            }
            if (credit != null) {
                Spacer(Modifier.height(16.dp))
                Text(credit, style = MaterialTheme.typography.bodySmall, color = EncreDouce, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Le coup de tampon de la ville (avec la date) et un timbre dentelé de la couleur de la carte ;
 * [centre] dessine le milieu du timbre.
 */
@Composable
fun TimbreEtTampon(
    ville: String,
    date: String,
    couleur: Color,
    haut: String,
    bas: String,
    modifier: Modifier = Modifier,
    centre: @Composable BoxScope.() -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(70.dp)
                .border(2.dp, EncreDuTampon, CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${ville.uppercase()}\n$date",
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 7.sp, lineHeight = 9.sp, color = EncreDuTampon),
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(width = 76.dp, height = 90.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                // Le papier du timbre et sa dentelure.
                drawRect(Color.White)
                val r = 2.6.dp.toPx()
                val pas = 7.dp.toPx()
                var x = pas / 2
                while (x < size.width) {
                    drawCircle(Creme, r, Offset(x, 0f))
                    drawCircle(Creme, r, Offset(x, size.height))
                    x += pas
                }
                var y = pas / 2
                while (y < size.height) {
                    drawCircle(Creme, r, Offset(0f, y))
                    drawCircle(Creme, r, Offset(size.width, y))
                    y += pas
                }
                val marge = 6.dp.toPx()
                drawRect(couleur, Offset(marge, marge), Size(size.width - 2 * marge, size.height - 2 * marge))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center,
                content = centre,
            )
            Text(
                haut,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
            Text(
                bas,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            )
        }
    }
}
