package fr.courrierdesnuages.app.monde

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.cartes.Atlas
import fr.courrierdesnuages.app.cartes.EcranAlbumAtlas
import fr.courrierdesnuages.app.cartes.EcranPlacerCarte
import fr.courrierdesnuages.app.cartes.Lieu
import fr.courrierdesnuages.app.cartes.MerDesCartes
import fr.courrierdesnuages.app.cartes.TerreVierge
import fr.courrierdesnuages.app.cartes.TimbreEtTampon
import fr.courrierdesnuages.app.cartes.cheminsDe
import fr.courrierdesnuages.app.cartes.rememberAtlasMonde
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.Creme
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Rouge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val Trait = Color(0xFFD9D2C3)

/** Les pays du tour du monde, en couleur pâle sur la carte : c'est là que le bus de Mister Owl se pose. */
private val TerreAnglophone = Color(0xFFE6EEF7)

fun couleurDe(continent: Continent): Color = when (continent) {
    Continent.EUROPE -> Color(0xFF5B9BD5)
    Continent.AMERIQUE -> Color(0xFFE57373)
    Continent.CARAIBES -> Color(0xFFF5B041)
    Continent.AFRIQUE -> Color(0xFF8CC084)
    Continent.ASIE -> Color(0xFFA98BD4)
    Continent.OCEANIE -> Color(0xFF4DB6AC)
}

/** La couleur d'un pays : celle de sa première carte (les États-Unis sont rouges, même à Hawaï). */
fun couleurDuPays(pays: String): Color =
    TOUR_DU_MONDE.firstOrNull { it.pays == pays }?.let { couleurDe(it.continent) } ?: TerreVierge

/** « au Royaume-Uni », « aux États-Unis », « en Inde », « à Malte ». */
fun auPays(lePays: String): String = when {
    lePays.startsWith("le ") -> "au " + lePays.removePrefix("le ")
    lePays.startsWith("les ") -> "aux " + lePays.removePrefix("les ")
    lePays.startsWith("la ") -> "en " + lePays.removePrefix("la ")
    lePays.startsWith("l'") -> "en " + lePays.removePrefix("l'")
    else -> "à $lePays"
}

private fun dateDuTampon(millis: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.FRENCH).format(Date(if (millis > 0) millis else System.currentTimeMillis()))

fun CarteMonde.lieu(): Lieu = Lieu(id, pays, latitude, longitude, couleurDuPays(pays), ville)

// ---------- La carte postale ----------

/** Le recto : un morceau de la carte du monde autour de la ville, son pays en couleur. */
@Composable
fun RectoMonde(c: CarteMonde, atlas: Atlas, modifier: Modifier = Modifier) {
    val densite = LocalDensity.current
    val chemins = remember(atlas) { cheminsDe(atlas) }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(900f / 586f)
            .clip(RoundedCornerShape(10.dp))
            .background(Creme),
    ) {
        val u = maxWidth / 100
        fun taille(n: Float): TextUnit = with(densite) { (u * n).toSp() }
        Canvas(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .padding(u * 3.5f)
                .clip(RoundedCornerShape(u * 3))
                .background(MerDesCartes),
        ) {
            val pays = atlas.formes[c.pays] ?: return@Canvas
            val (vx, vy) = atlas.position(c.latitude, c.longitude)
            // La fenêtre : centrée sur la ville, à la taille de sa terre (l'île d'Hawaï plutôt que
            // tous les États-Unis), ni trop petite ni trop grande.
            val anneau = pays.anneauPres(vx, vy)
            var lMin = Float.MAX_VALUE
            var lMax = -Float.MAX_VALUE
            var hMin = Float.MAX_VALUE
            var hMax = -Float.MAX_VALUE
            for (i in anneau.indices step 2) {
                lMin = min(lMin, anneau[i])
                lMax = max(lMax, anneau[i])
                hMin = min(hMin, anneau[i + 1])
                hMax = max(hMax, anneau[i + 1])
            }
            val cote = (max(lMax - lMin, hMax - hMin) * 1.1f).coerceIn(30f, 110f)
            val k = min(size.width, size.height) / cote
            val dx = size.width / 2 - vx * k
            val dy = size.height / 2 - vy * k
            val gauche = vx - size.width / 2 / k
            val droite = vx + size.width / 2 / k
            val haut = vy - size.height / 2 / k
            val bas = vy + size.height / 2 / k
            clipRect {
                withTransform({
                    translate(dx, dy)
                    scale(k, k, pivot = Offset.Zero)
                }) {
                    for ((id, f) in atlas.formes) {
                        if (f.droite < gauche || f.gauche > droite || f.bas < haut || f.haut > bas) continue
                        val chemin = chemins.getValue(id)
                        drawPath(chemin, if (id == c.pays) couleurDuPays(id) else TerreVierge)
                        drawPath(chemin, Color.White, style = Stroke(width = 1.dp.toPx() / k, join = StrokeJoin.Round))
                    }
                }
            }
            val centre = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.White, u.toPx() * 2.1f, centre)
            drawCircle(Rouge, u.toPx() * 1.4f, centre)
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.45f)
                .fillMaxHeight()
                .padding(top = u * 5, bottom = u * 5, end = u * 4),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Greetings from",
                style = TextStyle(fontFamily = Andika, fontStyle = FontStyle.Italic, fontSize = taille(4.2f), color = EncreDouce),
                maxLines = 1,
            )
            BasicText(
                c.villeAnglaise,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, color = Encre, textAlign = TextAlign.Center),
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = taille(3f), maxFontSize = taille(8f), stepSize = 0.5.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(u * 2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.drapeau, fontSize = taille(5f))
                Spacer(Modifier.width(u * 1.5f))
                BasicText(
                    c.nomPays,
                    style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, color = EncreDouce),
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = taille(2.4f), maxFontSize = taille(4.2f), stepSize = 0.5.sp),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(u * 2))
            Text(c.motif, fontSize = taille(11f))
        }
    }
}

/** Le verso : le petit mot de Mister Owl, le mot d'anglais, le timbre et l'adresse. */
@Composable
fun VersoMonde(c: CarteMonde, prenom: String, gagneeLe: Long, modifier: Modifier = Modifier) {
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
                    Text("POSTCARD", style = MaterialTheme.typography.labelMedium, color = EncreDouce)
                    Spacer(Modifier.height(4.dp))
                    Text("Hello $prenom !", style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Rouge))
                }
                TimbreEtTampon(c.ville, dateDuTampon(gagneeLe), couleurDuPays(c.pays), "AIR MAIL", c.pays) {
                    Text(c.drapeau, fontSize = 30.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            for (p in c.paragraphes) {
                Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF1D6)) {
                Text(
                    "En anglais, « ${c.motFrancais} » se dit ${c.motAnglais}.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Your friend,\nMister Owl", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(c.legende, style = MaterialTheme.typography.labelMedium, color = EncreDouce)
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

/** Une carte du tour du monde, que l'on retourne ; une carte qui arrive se place ensuite sur le monde. */
@Composable
fun EcranCarteMonde(vm: AppViewModel, profil: Profil, c: CarteMonde, nouvelle: Boolean) {
    val atlas = rememberAtlasMonde()
    val gagnee = vm.donnees(profil.id).cartesMonde.firstOrNull { it.idCarte == c.id }
    var verso by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour(if (nouvelle) "Une carte pour toi !" else c.ville, onRetour = { vm.retour() })
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
                    VersoMonde(c, profil.prenom, gagnee?.gagneeLe ?: 0L, Modifier.clickable { verso = false })
                } else {
                    RectoMonde(c, atlas, Modifier.clickable { verso = true })
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
                    texte = "Placer la carte sur le monde",
                    onClick = { vm.ouvrirEpingleMonde(profil, c.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                BoutonPrincipal(texte = "Mon tour du monde", onClick = { vm.ouvrirAlbumMonde(profil) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** L'enfant place sa carte en touchant le bon pays. */
@Composable
fun EcranEpinglerMonde(vm: AppViewModel, profil: Profil, c: CarteMonde) {
    val deja = vm.donnees(profil.id).cartesMonde.filter { it.epinglee && it.idCarte != c.id }.mapNotNull { carteMonde(it.idCarte)?.lieu() }
    EcranPlacerCarte(
        atlas = rememberAtlasMonde(),
        lieu = c.lieu(),
        deja = deja,
        fondDe = { id -> if (id in PAYS_DU_TOUR) TerreAnglophone else TerreVierge },
        question = "Où se trouve${if (c.lePays.startsWith("les ")) "nt" else ""} ${c.lePays} ? Touche le bon pays sur la carte.",
        bravo = "Bravo ! Tu as trouvé ${c.lePays} !",
        presque = if (c.ville == c.lePays) "Presque ! ${c.ville} est ici." else "Presque ! ${c.ville} est ici, ${auPays(c.lePays)}.",
        onPlacee = { vm.epinglerMonde(profil, c.id, it) },
        onAlbum = { vm.ouvrirAlbumMonde(profil) },
        onRetour = { vm.retour() },
    )
}

/** Le tour du monde des pays anglophones : les pays se colorient au fil des cartes placées. */
@Composable
fun EcranAlbumMonde(vm: AppViewModel, profil: Profil) {
    val atlas = rememberAtlasMonde()
    val ordre = TOUR_DU_MONDE.map { it.id }
    val gagnees = vm.donnees(profil.id).cartesMonde
        .mapNotNull { g -> carteMonde(g.idCarte)?.let { it to g } }
        .sortedBy { ordre.indexOf(it.first.id) }
    EcranAlbumAtlas(
        titre = "Mon tour du monde",
        sousTitre = "Le tour du monde des pays anglophones",
        atlas = atlas,
        gagnees = gagnees.map { (c, g) -> c.lieu() to g },
        total = TOUR_DU_MONDE.size,
        texteVide = "Pas encore de carte. Une semaine d'anglais sans faute, et Mister Owl t'en apportera une !",
        texteComplet = "Bravo ! Tu as les ${TOUR_DU_MONDE.size} cartes : tu as fait le tour du monde des pays anglophones !",
        texteBoutonPlacer = { l -> "Placer la carte de ${carteMonde(l.id)?.ville ?: ""}".trim() },
        fondDe = { id -> if (id in PAYS_DU_TOUR) TerreAnglophone else TerreVierge },
        nomDeForme = { id ->
            if (id in PAYS_DU_TOUR) {
                TOUR_DU_MONDE.first { it.pays == id }.let { "${it.drapeau} ${it.nomPays} : Mister Owl n'y est pas encore allé avec toi." }
            } else {
                null
            }
        },
        vignette = { l, m -> carteMonde(l.id)?.let { RectoMonde(it, atlas, m) } },
        onOuvrir = { l -> vm.ouvrirCarteMonde(profil, l.id, nouvelle = false) },
        onPlacer = { l -> vm.ouvrirEpingleMonde(profil, l.id) },
        onRetour = { vm.retour() },
    )
}
