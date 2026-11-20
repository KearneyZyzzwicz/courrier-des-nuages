package fr.courrierdesnuages.app.hangar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.data.Avatar
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.CielHaut
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Vert

@DrawableRes
private fun arriereDe(e: Engin): Int = when (e.cle) {
    "tapis" -> R.drawable.engin_tapis_arriere
    "baignoire" -> R.drawable.engin_baignoire_arriere
    "montgolfiere" -> R.drawable.engin_montgolfiere_arriere
    "velo" -> R.drawable.engin_velo_arriere
    "zeppelin" -> R.drawable.engin_zeppelin_arriere
    "theiere" -> R.drawable.engin_theiere_arriere
    "hydravion" -> R.drawable.engin_hydravion_arriere
    "cerf_volant" -> R.drawable.engin_cerf_volant_arriere
    "nuage" -> R.drawable.engin_nuage_arriere
    "parapluie" -> R.drawable.engin_parapluie_arriere
    "autogire" -> R.drawable.engin_autogire_arriere
    "traineau" -> R.drawable.engin_traineau_arriere
    "boite_aux_lettres" -> R.drawable.engin_boite_aux_lettres_arriere
    "bateau" -> R.drawable.engin_bateau_arriere
    else -> R.drawable.engin_soucoupe_arriere
}

@DrawableRes
private fun avantDe(e: Engin): Int = when (e.cle) {
    "tapis" -> R.drawable.engin_tapis_avant
    "baignoire" -> R.drawable.engin_baignoire_avant
    "montgolfiere" -> R.drawable.engin_montgolfiere_avant
    "velo" -> R.drawable.engin_velo_avant
    "zeppelin" -> R.drawable.engin_zeppelin_avant
    "theiere" -> R.drawable.engin_theiere_avant
    "hydravion" -> R.drawable.engin_hydravion_avant
    "cerf_volant" -> R.drawable.engin_cerf_volant_avant
    "nuage" -> R.drawable.engin_nuage_avant
    "parapluie" -> R.drawable.engin_parapluie_avant
    "autogire" -> R.drawable.engin_autogire_avant
    "traineau" -> R.drawable.engin_traineau_avant
    "boite_aux_lettres" -> R.drawable.engin_boite_aux_lettres_avant
    "bateau" -> R.drawable.engin_bateau_avant
    else -> R.drawable.engin_soucoupe_avant
}

@DrawableRes
private fun piloteDe(a: Avatar): Int = when (a) {
    Avatar.HERISSON -> R.drawable.pilote_herisson
    Avatar.SOURIS -> R.drawable.pilote_souris
    Avatar.OURS -> R.drawable.pilote_ours
    Avatar.RENARD -> R.drawable.pilote_renard
    Avatar.CHOUETTE -> R.drawable.pilote_chouette
    Avatar.LAPIN -> R.drawable.pilote_lapin
    Avatar.TORTUE -> R.drawable.pilote_tortue
    Avatar.ECUREUIL -> R.drawable.pilote_ecureuil
}

/** La couleur du tour de badge de chaque avatar (comme sur son dessin). */
private fun couleurDuBadge(a: Avatar): Color = when (a) {
    Avatar.HERISSON -> Color(0xFFE4573D)
    Avatar.SOURIS -> Color(0xFFF2C14E)
    Avatar.OURS -> Color(0xFF2F9C9A)
    Avatar.RENARD -> Color(0xFFE8913A)
    Avatar.CHOUETTE -> Color(0xFF3C78B5)
    Avatar.LAPIN -> Color(0xFF8A6BB8)
    Avatar.TORTUE -> Color(0xFF3E9A6A)
    Avatar.ECUREUIL -> Color(0xFFE77D9A)
}

/** L'engin et son pilote (sans pilote : l'engin seul), dans un carré. */
@Composable
fun PiloteEnEngin(avatar: Avatar?, engin: Engin, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        Image(painterResource(arriereDe(engin)), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (avatar != null) {
            // Le pilote : son buste fait 160 sur 160, visage en (80, 84), posé sur le siège de l'engin (sur 320).
            val u = maxWidth / 320
            val k = engin.siegeEchelle
            Image(
                painterResource(piloteDe(avatar)),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = u * (engin.siegeX - 80 * k), y = u * (engin.siegeY - 84 * k))
                    .size(u * 160 * k),
            )
        }
        Image(painterResource(avantDe(engin)), contentDescription = "${avatar?.nom ?: ""} ${engin.en}".trim(), modifier = Modifier.fillMaxSize())
    }
}

/** Le badge rond du facteur : son engin du hangar, ou le dessin de départ de l'avatar. */
@Composable
fun BadgeFacteur(avatar: Avatar, engin: Engin?, modifier: Modifier = Modifier) {
    if (engin == null) {
        Image(painterResource(avatar.image), contentDescription = "${avatar.nom} ${avatar.engin}", modifier = modifier.aspectRatio(1f))
        return
    }
    val anneau = couleurDuBadge(avatar)
    Box(modifier.aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(anneau, r * 0.99f)
            drawCircle(Brush.verticalGradient(listOf(CielHaut, CielBas)), r * 0.94f)
            // Deux petits nuages.
            for ((cx, cy, k) in listOf(Triple(0.22f, 0.8f, 0.07f), Triple(0.8f, 0.76f, 0.06f))) {
                val c = Offset(size.width * cx, size.height * cy)
                drawCircle(Color.White, size.width * k, c)
                drawCircle(Color.White, size.width * k * 0.75f, c + Offset(-size.width * k, size.width * k * 0.25f))
                drawCircle(Color.White, size.width * k * 0.8f, c + Offset(size.width * k, size.width * k * 0.2f))
            }
        }
        PiloteEnEngin(
            avatar,
            engin,
            Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(CircleShape),
        )
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White, size.minDimension / 2 * 0.94f, style = Stroke(width = size.minDimension * 0.019f))
        }
    }
}

private val gris = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** L'engin en construction : grisé, avec la part déjà construite en couleur (du bas vers le haut). */
@Composable
private fun EnginEnConstruction(engin: Engin, pieces: Int, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f)) {
        Image(painterResource(arriereDe(engin)), null, Modifier.fillMaxSize(), alpha = 0.28f, colorFilter = gris)
        Image(painterResource(avantDe(engin)), null, Modifier.fillMaxSize(), alpha = 0.28f, colorFilter = gris)
        val part = pieces / Hangar.PIECES_PAR_ENGIN.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent { clipRect(top = size.height * (1 - part)) { this@drawWithContent.drawContent() } },
        ) {
            Image(painterResource(arriereDe(engin)), null, Modifier.fillMaxSize())
            Image(painterResource(avantDe(engin)), contentDescription = engin.nom, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Les cinq pièces de l'engin en construction : celles déjà gagnées sont cochées. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PiecesDe(engin: Engin, gagnees: Int) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        engin.pieces.forEachIndexed { i, nom ->
            val ok = i < gagnees
            Surface(shape = RoundedCornerShape(50), color = if (ok) Vert else CielBas) {
                Text(
                    (if (ok) "✓ " else "${i + 1}. ") + nom,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ok) Color.White else EncreDouce,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** À la fin d'une séance de maths à 100 % : la pièce gagnée pour le hangar. */
@Composable
fun CarteNouvellePiece(np: NouvellePiece, avatar: Avatar, onHangar: () -> Unit) {
    Carte(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (np.termine) "Ta dernière pièce, ${np.piece}, est arrivée : ${np.engin.nom.replaceFirstChar { it.lowercase() }} est prêt${if (np.engin.nom.startsWith("La ")) "e" else ""} !"
                else "Une nouvelle pièce pour ton hangar : ${np.piece} (${np.numero} sur ${Hangar.PIECES_PAR_ENGIN}).",
                style = MaterialTheme.typography.titleMedium,
                color = Encre,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            if (np.termine) {
                BadgeFacteur(avatar, np.engin, Modifier.width(150.dp))
            } else {
                EnginEnConstruction(np.engin, np.numero, Modifier.width(150.dp))
            }
            Spacer(Modifier.height(10.dp))
            BoutonPrincipal(texte = "Voir mon hangar", onClick = onHangar, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Le hangar : l'engin du facteur, celui en construction, et tous les engins déjà construits. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranHangar(vm: AppViewModel, profil: Profil) {
    val pieces = profil.piecesDuHangar
    val construits = Hangar.construits(pieces)
    val choisi = profil.enginDuFacteur
    val enCours = Hangar.enConstruction(pieces)
    Column(Modifier.fillMaxSize()) {
        BarreRetour("Mon hangar", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarAnime(profil.avatar, taille = 150.dp, engin = choisi)
            Text(
                "${profil.avatar.nom} ${choisi?.en ?: profil.avatar.engin}",
                style = MaterialTheme.typography.titleLarge,
                color = Encre,
                textAlign = TextAlign.Center,
            )
            Text(
                "Chaque médaille d'or en maths t'apporte une pièce. Avec ${Hangar.PIECES_PAR_ENGIN} pièces, un nouvel engin sort du hangar !",
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (enCours != null) {
                val (engin, n) = enCours
                Carte(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("En construction : ${engin.nom.replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.titleMedium, color = Encre, textAlign = TextAlign.Center)
                        EnginEnConstruction(engin, n, Modifier.width(190.dp))
                        PiecesDe(engin, n)
                        Spacer(Modifier.height(6.dp))
                        val reste = Hangar.PIECES_PAR_ENGIN - n
                        Text(
                            "Encore $reste médaille${if (reste > 1) "s" else ""} d'or en maths.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EncreDouce,
                        )
                    }
                }
            } else {
                Text(
                    "Bravo ! Ton hangar est complet : tu as construit les ${ENGINS.size} engins !",
                    style = MaterialTheme.typography.titleMedium,
                    color = Vert,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Choisis l'engin de ta tournée",
                style = MaterialTheme.typography.titleLarge,
                color = Encre,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (e in listOf<Engin?>(null) + construits) {
                    val estChoisi = e?.cle == choisi?.cle
                    Column(
                        Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(if (estChoisi) 3.dp else 0.dp, if (estChoisi) Vert else Color.Transparent, RoundedCornerShape(18.dp))
                            .clickable { vm.choisirEngin(profil, e?.cle) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BadgeFacteur(profil.avatar, e, Modifier.fillMaxWidth())
                        Text(
                            e?.nom ?: "L'engin de départ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Encre,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        if (estChoisi) Text("✓ Mon engin", style = MaterialTheme.typography.labelMedium, color = Vert)
                    }
                }
            }
            val aDecouvrir = ENGINS.size - construits.size - (if (enCours != null) 1 else 0)
            if (aDecouvrir > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Et encore $aDecouvrir engin${if (aDecouvrir > 1) "s" else ""} mystère${if (aDecouvrir > 1) "s" else ""} à découvrir…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
