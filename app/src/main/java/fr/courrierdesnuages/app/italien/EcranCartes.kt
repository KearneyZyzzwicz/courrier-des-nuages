package fr.courrierdesnuages.app.italien

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.lecture.Images
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

private val Mer = Color(0xFFBFE3F2)
private val Creme = Color(0xFFFFFDF8)
private val Trait = Color(0xFFD9D2C3)
private val BleuTimbre = Color(0xFF3B78B8)

/** Distance (en unités de la carte, 1000 de large) en dessous de laquelle l'épingle est bien placée. */
private const val TOLERANCE = 90f

private fun dateDuTampon(millis: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.FRENCH).format(Date(if (millis > 0) millis else System.currentTimeMillis()))

// ---------- La carte postale ----------

/** Le recto : l'illustration (ou la photo du parent). */
@Composable
fun RectoCarte(carte: CartePostale, modifier: Modifier = Modifier) {
    val photo = remember(carte.id) { carte.image?.let { Images.decoder(it)?.asImageBitmap() } }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(900f / 586f)
            .clip(RoundedCornerShape(10.dp))
            .background(Mer),
    ) {
        if (carte.recto != 0) {
            Image(painterResource(carte.recto), contentDescription = carte.legende, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else if (photo != null) {
            Image(photo, contentDescription = carte.ville, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Le timbre et le coup de tampon, avec la date où la carte a été gagnée. */
@Composable
private fun Timbre(carte: CartePostale, date: String, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(330f / 204f)) {
        if (carte.timbre != 0) {
            Image(painterResource(carte.timbre), contentDescription = "Timbre oblitéré à ${carte.ville}", modifier = Modifier.fillMaxSize())
            // La date au centre du tampon (emplacement repris du dessin du timbre : x 150, y 126 sur 330 × 204).
            Text(
                date,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color(0xFF3B4A6B)),
                modifier = Modifier.align(BiasAlignment(-0.09f, 0.18f)),
            )
        } else {
            // Timbre simple pour les cartes créées par le parent.
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.48f)
                    .fillMaxSize()
                    .padding(6.dp)
                    .background(BleuTimbre, RoundedCornerShape(4.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "COURRIER\nDES NUAGES\n1 NUAGE",
                    style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White),
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(64.dp)
                    .border(2.dp, Color(0xFF3B4A6B), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${carte.ville.uppercase()}\n$date",
                    style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color(0xFF3B4A6B)),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Le verso : le petit mot du pigeon voyageur, le timbre et l'adresse. */
@Composable
fun VersoCarte(carte: CartePostale, prenom: String, gagneeLe: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Creme,
        contentColor = Encre,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CARTE POSTALE", style = MaterialTheme.typography.labelMedium, color = EncreDouce)
                    Spacer(Modifier.height(4.dp))
                    Text("Ciao $prenom !", style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Rouge))
                }
                Timbre(carte, dateDuTampon(gagneeLe), Modifier.width(170.dp))
            }
            Spacer(Modifier.height(10.dp))
            for (p in carte.paragraphes) {
                Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (carte.motFrancais.isNotBlank() && carte.motItalien.isNotBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF1D6)) {
                    Text(
                        "En italien, « ${carte.motFrancais} » se dit ${carte.motItalien}.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Text("Ton ami,\nle Pigeon voyageur", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(carte.legende, style = MaterialTheme.typography.labelMedium, color = EncreDouce)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Trait, thickness = 2.dp)
            Spacer(Modifier.height(10.dp))
            for (ligne in listOf("Pour : $prenom", "Au Courrier des Nuages", "Île des petits facteurs")) {
                Text(ligne, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
                HorizontalDivider(color = Trait, thickness = 1.dp)
            }
        }
    }
}

/** Une carte postale, que l'on retourne pour lire le verso. Une carte qui vient d'arriver s'épingle ensuite. */
@Composable
fun EcranCartePostale(vm: AppViewModel, profil: Profil, carte: CartePostale, nouvelle: Boolean) {
    val gagnee = vm.donnees(profil.id).cartesGagnees.firstOrNull { it.idCarte == carte.id }
    var verso by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour(if (nouvelle) "Une carte pour toi !" else carte.ville, onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = verso,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "carte",
            ) { faceVerso ->
                if (faceVerso) {
                    VersoCarte(carte, profil.prenom, gagnee?.gagneeLe ?: 0L, Modifier.clickable { verso = false })
                } else {
                    RectoCarte(carte, Modifier.clickable { verso = true })
                }
            }
            Spacer(Modifier.height(12.dp))
            BoutonSecondaire(
                texte = if (verso) "Voir l'image" else "Retourner la carte",
                onClick = { verso = !verso },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            if (gagnee != null && !gagnee.epinglee) {
                BoutonPrincipal(
                    texte = "Épingler la carte sur l'Italie",
                    onClick = { vm.ouvrirEpingle(profil, carte.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                BoutonPrincipal(texte = "Mon album", onClick = { vm.ouvrirAlbum(profil) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------- La carte de l'Italie ----------

/** Une épingle sur la carte : position de 0 à 1, et sa couleur. */
data class Epingle(val x: Float, val y: Float, val couleur: Color)

/** La carte de l'Italie avec ses épingles. [surToucher] reçoit la position touchée (de 0 à 1). */
@Composable
fun CarteDItalie(
    epingles: List<Epingle>,
    modifier: Modifier = Modifier,
    trait: Pair<Offset, Offset>? = null,
    surToucher: ((Float, Float) -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CarteItalie.LARGEUR / CarteItalie.HAUTEUR)
            .clip(RoundedCornerShape(16.dp))
            .background(Mer)
            .then(
                if (surToucher != null) {
                    Modifier.pointerInput(surToucher) {
                        detectTapGestures { p -> surToucher(p.x / size.width, p.y / size.height) }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Image(
            painterResource(R.drawable.carte_italie),
            contentDescription = "Carte de l'Italie",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            val r = 9.dp.toPx()
            if (trait != null) {
                drawLine(
                    Encre.copy(alpha = 0.5f),
                    Offset(trait.first.x * size.width, trait.first.y * size.height),
                    Offset(trait.second.x * size.width, trait.second.y * size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            for (e in epingles) {
                val c = Offset(e.x * size.width, e.y * size.height)
                drawCircle(Color.White, r * 1.35f, c)
                drawCircle(e.couleur, r, c)
                drawCircle(Encre.copy(alpha = 0.35f), r * 1.35f, c, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** L'enfant épingle sa nouvelle carte sur la bonne ville. S'il se trompe, l'appli montre le bon endroit. */
@Composable
fun EcranEpingler(vm: AppViewModel, profil: Profil, carte: CartePostale) {
    val ville = villeNommee(carte.ville)
    val juste = ville?.let { CarteItalie.position(it) }
    var essai by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var bienPlacee by remember { mutableStateOf<Boolean?>(null) }
    val deja = vm.donnees(profil.id).cartesGagnees.filter { it.epinglee && it.idCarte != carte.id }

    LaunchedEffect(bienPlacee) {
        val b = bienPlacee ?: return@LaunchedEffect
        vm.epingler(profil, carte.id, b)
    }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Épingle ta carte", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val consigne = when (bienPlacee) {
                null -> "Où se trouve ${carte.ville} ? Touche la carte à l'endroit de la ville."
                true -> "Bravo ! ${carte.ville} est bien là, ${regionAvecArticle(carte.region)}."
                false -> "Presque ! ${carte.ville} est ici, ${regionAvecArticle(carte.region)}."
            }
            Text(
                consigne,
                style = MaterialTheme.typography.titleMedium,
                color = when (bienPlacee) {
                    true -> Vert
                    false -> RougeFonce
                    null -> Encre
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val epingles = buildList {
                for (g in deja) {
                    val v = vm.carte(g.idCarte)?.let { villeNommee(it.ville) } ?: continue
                    val (x, y) = CarteItalie.position(v)
                    add(Epingle(x, y, Jaune))
                }
                val e = essai
                if (e != null && bienPlacee == false) add(Epingle(e.first, e.second, EncreDouce))
                if (bienPlacee != null && juste != null) add(Epingle(juste.first, juste.second, Rouge))
            }
            val toucher = essai
            val trait = if (bienPlacee == false && toucher != null && juste != null) {
                Offset(toucher.first, toucher.second) to Offset(juste.first, juste.second)
            } else {
                null
            }
            CarteDItalie(
                epingles = epingles,
                trait = trait,
                surToucher = if (bienPlacee == null && juste != null) { x, y ->
                    essai = x to y
                    val distance = hypot((x - juste.first) * CarteItalie.LARGEUR, (y - juste.second) * CarteItalie.HAUTEUR)
                    bienPlacee = distance <= TOLERANCE
                } else {
                    null
                },
            )
            Spacer(Modifier.height(14.dp))
            if (bienPlacee != null || juste == null) {
                BoutonPrincipal(texte = "Voir mon album", onClick = { vm.ouvrirAlbum(profil) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun regionAvecArticle(region: String): String = when {
    region.isEmpty() -> "en Italie"
    region.startsWith("Vallée") -> "dans la $region"
    region in listOf("Pouilles", "Marches", "Abruzzes") -> "dans les $region"
    region in listOf("Sicile", "Sardaigne") -> "en $region"
    region.first() in "AEIOUÉÈÊ" -> "en $region"
    region in listOf("Ligurie", "Lombardie", "Vénétie", "Toscane", "Ombrie", "Campanie", "Calabre", "Basilicate", "Frioul-Vénétie Julienne") -> "en $region"
    else -> "dans le $region"
}

/** L'album : la carte de l'Italie avec les cartes postales épinglées, et la liste des cartes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranAlbum(vm: AppViewModel, profil: Profil) {
    val gagnees = vm.donnees(profil.id).cartesGagnees
    val cartes = gagnees.mapNotNull { g -> vm.carte(g.idCarte)?.let { it to g } }
    val total = vm.toutesLesCartes.size
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Mon album d'Italie", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (cartes.isEmpty()) {
                    "Pas encore de carte. Une semaine d'italien sans faute, et le pigeon t'en apportera une !"
                } else {
                    "${cartes.size} carte${if (cartes.size > 1) "s" else ""} sur $total. Touche une épingle pour revoir la carte."
                },
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            for ((carte, _) in cartes.filter { !it.second.epinglee }) {
                BoutonPrincipal(
                    texte = "Épingler la carte de ${carte.ville}",
                    onClick = { vm.ouvrirEpingle(profil, carte.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            val placees = cartes.filter { it.second.epinglee }.mapNotNull { (carte, _) ->
                villeNommee(carte.ville)?.let { v -> carte to CarteItalie.position(v) }
            }
            CarteDItalie(
                epingles = placees.map { (_, p) -> Epingle(p.first, p.second, Rouge) },
                surToucher = { x, y ->
                    val proche = placees.minByOrNull { (_, p) ->
                        hypot((x - p.first) * CarteItalie.LARGEUR, (y - p.second) * CarteItalie.HAUTEUR)
                    }
                    if (proche != null) {
                        val (carte, p) = proche
                        val d = hypot((x - p.first) * CarteItalie.LARGEUR, (y - p.second) * CarteItalie.HAUTEUR)
                        if (d <= TOLERANCE) vm.ouvrirCarte(profil, carte.id, nouvelle = false)
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for ((carte, _) in cartes) {
                    Column(
                        Modifier
                            .width(150.dp)
                            .clickable { vm.ouvrirCarte(profil, carte.id, nouvelle = false) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RectoCarte(carte, Modifier.border(BorderStroke(2.dp, Color.White), RoundedCornerShape(10.dp)))
                        Text(carte.ville, style = MaterialTheme.typography.titleMedium, color = Encre)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
