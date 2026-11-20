package fr.courrierdesnuages.app.france

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import fr.courrierdesnuages.app.cartes.CarteAtlas
import fr.courrierdesnuages.app.cartes.EtatZoom
import fr.courrierdesnuages.app.cartes.Forme
import fr.courrierdesnuages.app.cartes.Repere
import fr.courrierdesnuages.app.cartes.TerreVierge
import fr.courrierdesnuages.app.cartes.chemin
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
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private val Trait = Color(0xFFD9D2C3)
private val EncreDuTampon = Color(0xFF3B4A6B)
private val Rate = Color(0xFFB8BEC9)

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
private fun DrawScope.dessinerForme(forme: Forme, couleur: Color, ombre: Boolean, bord: Dp): (Float, Float) -> Offset {
    val chemin = forme.chemin()
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
            if (forme != null) {
                val placer = dessinerForme(forme, couleur, ombre = true, bord = 2.dp)
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

/** Le timbre (la forme du département) et le coup de tampon de la ville, avec la date. */
@Composable
private fun TimbreDepartement(d: Departement, atlas: Atlas, date: String, modifier: Modifier = Modifier) {
    val forme = atlas.formes[d.code]
    val couleur = couleurDe(d.region)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(70.dp)
                .border(2.dp, EncreDuTampon, CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${d.ville.uppercase()}\n$date",
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
            if (forme != null) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 22.dp),
                ) {
                    dessinerForme(forme, Color.White.copy(alpha = 0.9f), ombre = false, bord = 0.5.dp)
                }
            }
            Text(
                "FRANCE",
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.White),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
            Text(
                d.code,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            )
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
                TimbreDepartement(d, atlas, dateDuTampon(gagneeLe))
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

/** L'enfant place sa nouvelle carte en touchant le bon département. S'il se trompe, l'appli le lui montre. */
@Composable
fun EcranEpinglerDepartement(vm: AppViewModel, profil: Profil, d: Departement) {
    val atlas = rememberAtlasFrance()
    val zoom = remember { EtatZoom() }
    var touche by remember { mutableStateOf<String?>(null) }
    var point by remember { mutableStateOf<Offset?>(null) }
    var bienPlacee by remember { mutableStateOf<Boolean?>(null) }
    val deja = vm.donnees(profil.id).cartesFrance.filter { it.epinglee && it.idCarte != d.code }.map { it.idCarte }.toSet()

    LaunchedEffect(bienPlacee) {
        val b = bienPlacee ?: return@LaunchedEffect
        vm.epinglerDepartement(profil, d.code, b)
    }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Place ta carte", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val consigne = when (bienPlacee) {
                null -> "Où se trouve ${d.leNom} ? Touche le bon département sur la carte."
                true -> "Bravo ! Tu as trouvé ${d.leNom}, ${d.region.dans}."
                false -> "Presque ! ${d.leNom.replaceFirstChar { it.uppercase() }} est ici, ${d.region.dans}."
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
            val (vx, vy) = atlas.position(d.latitude, d.longitude)
            val reperes = buildList {
                if (bienPlacee != null) add(Repere(vx, vy, Rouge))
            }
            val p = point
            CarteAtlas(
                atlas = atlas,
                zoom = zoom,
                remplissage = { id ->
                    when {
                        id == d.code && bienPlacee != null -> couleurDe(d.region)
                        id == touche && bienPlacee == false -> Rate
                        id in deja -> departement(id)?.let { couleurDe(it.region).copy(alpha = 0.45f) } ?: TerreVierge
                        else -> TerreVierge
                    }
                },
                contour = { id -> if (id == d.code && bienPlacee == false) RougeFonce else null },
                reperes = reperes,
                trait = if (bienPlacee == false && p != null) p to Offset(vx, vy) else null,
                surToucher = if (bienPlacee == null) { x, y, rayon, petite ->
                    point = Offset(x, y)
                    touche = atlas.formeEn(x, y)?.id
                    bienPlacee = atlas.bienPlace(d.code, x, y, rayon, petite)
                } else {
                    null
                },
            )
            Text(
                "Pince la carte avec deux doigts, ou touche +, pour l'agrandir.",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            if (bienPlacee != null) {
                BoutonPrincipal(texte = "Voir mon album", onClick = { vm.ouvrirAlbumFrance(profil) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** L'album : la France qui se colorie département après département, et les cartes gagnées. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranAlbumFrance(vm: AppViewModel, profil: Profil) {
    val atlas = rememberAtlasFrance()
    val zoom = remember { EtatZoom() }
    val gagnees = vm.donnees(profil.id).cartesFrance
    val cartes = gagnees.mapNotNull { g -> departement(g.idCarte)?.let { it to g } }.sortedBy { it.first.code }
    val placees = cartes.filter { it.second.epinglee }.map { it.first }
    val codesPlaces = placees.map { it.code }.toSet()
    var choisi by remember { mutableStateOf<Departement?>(null) }
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Mon album de France", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (cartes.size) {
                    0 -> "Pas encore de carte. Une dictée avec tous les points, et le Petit Avion t'en apportera une !"
                    DEPARTEMENTS.size -> "Bravo ! Tu as les ${DEPARTEMENTS.size} départements : tu as fait le tour de France !"
                    else -> "${cartes.size} département${if (cartes.size > 1) "s" else ""} sur ${DEPARTEMENTS.size}. Touche un département pour revoir sa carte."
                },
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            for ((d, _) in cartes.filter { !it.second.epinglee }) {
                BoutonPrincipal(
                    texte = "Placer la carte ${deLaVille(d.ville)}",
                    onClick = { vm.ouvrirEpingleDepartement(profil, d.code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            CarteAtlas(
                atlas = atlas,
                zoom = zoom,
                remplissage = { id -> if (id in codesPlaces) departement(id)?.let { couleurDe(it.region) } ?: TerreVierge else TerreVierge },
                contour = { id -> if (id == choisi?.code && id !in codesPlaces) EncreDouce else null },
                reperes = placees.map { d ->
                    val (x, y) = atlas.position(d.latitude, d.longitude)
                    Repere(x, y, Rouge)
                },
                etiquettes = placees.associate { it.code to it.code },
                surToucher = { x, y, rayon, _ ->
                    val f = atlas.formeEn(x, y)
                        ?: atlas.formes.values.filter { it.distance(x, y) <= rayon }.minByOrNull { it.distance(x, y) }
                    val d = f?.let { departement(it.id) }
                    if (d != null && d.code in codesPlaces) {
                        vm.ouvrirCarteDepartement(profil, d.code, nouvelle = false)
                    } else {
                        choisi = d
                    }
                },
            )
            val c = choisi
            Text(
                if (c != null && c.code !in codesPlaces) {
                    "${c.leNom.replaceFirstChar { it.uppercase() }} (${c.code}) : pas encore dans ton album."
                } else {
                    "Pince la carte avec deux doigts, ou touche +, pour l'agrandir."
                },
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
                for ((d, _) in cartes) {
                    Column(
                        Modifier
                            .width(150.dp)
                            .clickable { vm.ouvrirCarteDepartement(profil, d.code, nouvelle = false) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RectoDepartement(d, atlas, Modifier.border(BorderStroke(2.dp, Color.White), RoundedCornerShape(10.dp)))
                        Text("${d.code} · ${d.nom}", style = MaterialTheme.typography.labelMedium, color = Encre, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
