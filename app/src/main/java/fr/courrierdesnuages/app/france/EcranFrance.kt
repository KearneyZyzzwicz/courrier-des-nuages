package fr.courrierdesnuages.app.france

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.cartes.Atlas
import fr.courrierdesnuages.app.cartes.EcranAlbumAtlas
import fr.courrierdesnuages.app.cartes.EcranPlacerCarte
import fr.courrierdesnuages.app.cartes.Forme
import fr.courrierdesnuages.app.cartes.Lieu
import fr.courrierdesnuages.app.cartes.TerreVierge
import fr.courrierdesnuages.app.cartes.TimbreEtTampon
import fr.courrierdesnuages.app.cartes.cheminsDe
import fr.courrierdesnuages.app.cartes.rememberAtlasFrance
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.CielHaut
import fr.courrierdesnuages.app.ui.theme.Creme
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Rouge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private val Trait = Color(0xFFD9D2C3)

/** La couleur des départements de chaque région sur la carte. */
fun couleurDe(region: Region): Color = when (region) {
    Region.BRETAGNE -> Color(0xFF5B9BD5)
    Region.NORMANDIE -> Color(0xFFF2C14E)
    Region.HAUTS_DE_FRANCE -> Color(0xFF8CC084)
    Region.ILE_DE_FRANCE -> Color(0xFFE57373)
    Region.GRAND_EST -> Color(0xFFA98BD4)
    Region.PAYS_DE_LA_LOIRE -> Color(0xFF6FBF73)
    Region.CENTRE -> Color(0xFF9FA8DA)
    Region.BOURGOGNE -> Color(0xFFE79AC0)
    Region.NOUVELLE_AQUITAINE -> Color(0xFFF4A6A6)
    Region.AUVERGNE -> Color(0xFF4DB6AC)
    Region.OCCITANIE -> Color(0xFFF5B041)
    Region.PROVENCE -> Color(0xFFEC8FB5)
    Region.CORSE -> Color(0xFFC39BD3)
}

private fun dateDuTampon(millis: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.FRENCH).format(Date(if (millis > 0) millis else System.currentTimeMillis()))

/** « de » et « Nancy », « d' » et « Albi », « du » et « Mans », « des » et « Sables-d'Olonne ». */
fun prepositionEtVille(ville: String): Pair<String, String> = when {
    ville.startsWith("Le ") -> "du" to ville.removePrefix("Le ")
    ville.startsWith("Les ") -> "des" to ville.removePrefix("Les ")
    ville.first().uppercaseChar() in "AEIOUYÉÈÊÂÎÔH" -> "d'" to ville
    else -> "de" to ville
}

/** « de Nancy », « d'Albi », « du Mans », « des Sables-d'Olonne ». */
fun deLaVille(ville: String): String {
    val (de, nom) = prepositionEtVille(ville)
    return if (de.endsWith("'")) de + nom else "$de $nom"
}

/** Dessine une forme pour qu'elle remplisse au mieux la zone, et renvoie la façon de placer un point. */
private fun DrawScope.dessinerForme(forme: Forme, chemin: Path, couleur: Color, ombre: Boolean, bord: Dp): (Float, Float) -> Offset {
    val k = min(size.width / forme.largeur, size.height / forme.hauteur)
    val dx = (size.width - forme.largeur * k) / 2 - forme.gauche * k
    val dy = (size.height - forme.hauteur * k) / 2 - forme.haut * k
    if (ombre) {
        withTransform({
            translate(dx + 1.5.dp.toPx(), dy + 2.5.dp.toPx())
            scale(k, k, pivot = Offset.Zero)
        }) { drawPath(chemin, Color.Black.copy(alpha = 0.13f)) }
    }
    withTransform({
        translate(dx, dy)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(chemin, couleur)
        drawPath(chemin, Color.White, style = Stroke(width = bord.toPx() / k, join = StrokeJoin.Round))
    }
    return { x, y -> Offset(dx + x * k, dy + y * k) }
}

// ---------- La carte postale d'un département ----------

/** Le recto : la forme du département, sa ville, son numéro et un motif. */
@Composable
fun RectoDepartement(d: Departement, atlas: Atlas, modifier: Modifier = Modifier) {
    val forme = atlas.formes[d.code]
    val chemin = remember(atlas) { cheminsDe(atlas)[d.code] }
    val couleur = couleurDe(d.region)
    val densite = LocalDensity.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(900f / 586f)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(CielHaut.copy(alpha = 0.55f), Creme))),
    ) {
        val u = maxWidth / 100
        fun taille(n: Float): TextUnit = with(densite) { (u * n).toSp() }
        // Deux petits nuages.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            for ((cx, cy, r) in listOf(Triple(0.12f, 0.16f, 0.035f), Triple(0.88f, 0.1f, 0.03f))) {
                val c = Offset(w * cx, size.height * cy)
                drawCircle(Color.White.copy(alpha = 0.9f), w * r, c)
                drawCircle(Color.White.copy(alpha = 0.9f), w * r * 0.8f, c + Offset(-w * r * 1.1f, w * r * 0.25f))
                drawCircle(Color.White.copy(alpha = 0.9f), w * r * 0.85f, c + Offset(w * r * 1.1f, w * r * 0.2f))
            }
        }
        Canvas(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .padding(u * 5),
        ) {
            if (forme != null && chemin != null) {
                val placer = dessinerForme(forme, chemin, couleur, ombre = true, bord = 2.dp)
                val (vx, vy) = atlas.position(d.latitude, d.longitude)
                val c = placer(vx, vy)
                drawCircle(Color.White, u.toPx() * 2.1f, c)
                drawCircle(Rouge, u.toPx() * 1.4f, c)
            }
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.47f)
                .fillMaxHeight()
                .padding(top = u * 5, bottom = u * 5, end = u * 4),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val (de, nom) = prepositionEtVille(d.ville)
            Text(
                "Bons baisers $de",
                style = TextStyle(fontFamily = Andika, fontStyle = FontStyle.Italic, fontSize = taille(4.2f), color = EncreDouce),
                maxLines = 1,
            )
            BasicText(
                nom,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, color = Encre, textAlign = TextAlign.Center),
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = taille(3f), maxFontSize = taille(8f), stepSize = 0.5.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(u * 2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(couleur, RoundedCornerShape(u * 1.6f))
                        .padding(horizontal = u * 1.6f, vertical = u * 0.5f),
                ) {
                    Text(d.code, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = taille(4.2f), color = Color.White))
                }
                Spacer(Modifier.width(u * 1.5f))
                BasicText(
                    d.nom,
                    style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, color = EncreDouce),
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = taille(2.4f), maxFontSize = taille(4.2f), stepSize = 0.5.sp),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(u * 2))
            Text(d.motif, fontSize = taille(11f))
        }
    }
}

/** Le verso : le petit mot du Petit Avion, le timbre et l'adresse. */
@Composable
fun VersoDepartement(d: Departement, atlas: Atlas, prenom: String, gagneeLe: Long, modifier: Modifier = Modifier) {
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
                    Text("Bonjour $prenom !", style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Rouge))
                }
                val forme = atlas.formes[d.code]
                val chemin = remember(atlas) { cheminsDe(atlas)[d.code] }
                TimbreEtTampon(d.ville, dateDuTampon(gagneeLe), couleurDe(d.region), "FRANCE", d.code) {
                    if (forme != null && chemin != null) {
                        Canvas(Modifier.fillMaxSize()) { dessinerForme(forme, chemin, Color.White.copy(alpha = 0.9f), ombre = false, bord = 0.5.dp) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            for (p in d.paragraphes) {
                Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (d.habitants != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF1D6)) {
                    Text(
                        "Le sais-tu ? Les habitants ${deLaVille(d.ville)} s'appellent les ${d.habitants}.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Text("Ton ami,\nle Petit Avion", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(d.legende, style = MaterialTheme.typography.labelMedium, color = EncreDouce)
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

/** La carte d'un département, que l'on retourne pour lire le verso ; une carte qui arrive se place ensuite sur la France. */
@Composable
fun EcranCarteDepartement(vm: AppViewModel, profil: Profil, d: Departement, nouvelle: Boolean) {
    val atlas = rememberAtlasFrance()
    val gagnee = vm.donnees(profil.id).cartesFrance.firstOrNull { it.idCarte == d.code }
    var verso by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour(if (nouvelle) "Une carte pour toi !" else d.nom, onRetour = { vm.retour() })
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
                    VersoDepartement(d, atlas, profil.prenom, gagnee?.gagneeLe ?: 0L, Modifier.clickable { verso = false })
                } else {
                    RectoDepartement(d, atlas, Modifier.clickable { verso = true })
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
                    texte = "Placer la carte sur la France",
                    onClick = { vm.ouvrirEpingleDepartement(profil, d.code) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                BoutonPrincipal(texte = "Mon album de France", onClick = { vm.ouvrirAlbumFrance(profil) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Ce que les écrans d'album savent d'un département. */
fun Departement.lieu(): Lieu = Lieu(code, code, latitude, longitude, couleurDe(region), "$code · $nom")

/** L'enfant place sa nouvelle carte en touchant le bon département. S'il se trompe, l'appli le lui montre. */
@Composable
fun EcranEpinglerDepartement(vm: AppViewModel, profil: Profil, d: Departement) {
    val deja = vm.donnees(profil.id).cartesFrance.filter { it.epinglee && it.idCarte != d.code }.mapNotNull { departement(it.idCarte)?.lieu() }
    EcranPlacerCarte(
        atlas = rememberAtlasFrance(),
        lieu = d.lieu(),
        deja = deja,
        fondDe = { TerreVierge },
        question = "Où se trouve ${d.leNom} ? Touche le bon département sur la carte.",
        bravo = "Bravo ! Tu as trouvé ${d.leNom}, ${d.region.dans}.",
        presque = "Presque ! ${d.leNom.replaceFirstChar { it.uppercase() }} est ici, ${d.region.dans}.",
        onPlacee = { vm.epinglerDepartement(profil, d.code, it) },
        onAlbum = { vm.ouvrirAlbumFrance(profil) },
        onRetour = { vm.retour() },
    )
}

/** L'album : la France qui se colorie département après département, et les cartes gagnées. */
@Composable
fun EcranAlbumFrance(vm: AppViewModel, profil: Profil) {
    val atlas = rememberAtlasFrance()
    val gagnees = vm.donnees(profil.id).cartesFrance
        .mapNotNull { g -> departement(g.idCarte)?.let { it to g } }
        .sortedBy { it.first.code }
    EcranAlbumAtlas(
        titre = "Mon album de France",
        atlas = atlas,
        gagnees = gagnees.map { (d, g) -> d.lieu() to g },
        total = DEPARTEMENTS.size,
        texteVide = "Pas encore de carte. Une dictée avec tous les points, et le Petit Avion t'en apportera une !",
        texteComplet = "Bravo ! Tu as les ${DEPARTEMENTS.size} départements : tu as fait le tour de France !",
        texteBoutonPlacer = { l -> departement(l.id)?.let { "Placer la carte ${deLaVille(it.ville)}" } ?: "Placer la carte" },
        fondDe = { TerreVierge },
        nomDeForme = { code -> departement(code)?.let { "${it.leNom.replaceFirstChar { c -> c.uppercase() }} (${it.code}) : pas encore dans ton album." } },
        vignette = { l, m -> departement(l.id)?.let { RectoDepartement(it, atlas, m) } },
        onOuvrir = { l -> vm.ouvrirCarteDepartement(profil, l.id, nouvelle = false) },
        onPlacer = { l -> vm.ouvrirEpingleDepartement(profil, l.id) },
        onRetour = { vm.retour() },
        etiquettes = gagnees.filter { it.second.epinglee }.associate { it.first.code to it.first.code },
        credit = "Carte des départements : IGN, Admin Express 2018 (Licence ouverte Etalab), via france-geojson.",
    )
}
