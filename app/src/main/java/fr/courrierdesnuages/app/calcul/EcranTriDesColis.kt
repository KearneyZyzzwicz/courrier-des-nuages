package fr.courrierdesnuages.app.calcul

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.pointsEnTexte
import fr.courrierdesnuages.app.dictee.DessinMedaille
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.TamponBravo
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Fredoka
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import kotlin.math.roundToInt

private val Carton = Color(0xFFD9A86C)
private val CartonFonce = Color(0xFFB98549)
private val Tapis = Color(0xFF4A5D7A)
private val TapisClair = Color(0xFF6B7F9E)
private val Casier = Color(0xFF7FA7C9)
private val CasierFonce = Color(0xFF5B83A6)

private val styleCalcul = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Encre)
private val styleNumero = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Encre)

/** Le centre de tri : chaque calcul est l'étiquette d'un colis, le résultat est le numéro du casier. */
@Composable
fun EcranTriDesColis(seance: SeanceCalcul, sons: Sons, onQuitter: () -> Unit) {
    var confirmerArret by remember { mutableStateOf(false) }
    val enCours = seance.etape != EtapeCalcul.FIN
    BackHandler { if (enCours) confirmerArret = true else onQuitter() }

    val vue = LocalView.current
    DisposableEffect(vue) {
        vue.keepScreenOn = true
        onDispose { vue.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (enCours) confirmerArret = true else onQuitter() }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Arrêter", tint = Encre, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        seance.enRattrapage -> "On revoit les calculs ratés"
                        seance.compte -> "Le tri des colis"
                        else -> "Entraînement"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Encre,
                )
                if (enCours) {
                    Text("Colis ${seance.numero} sur ${seance.total}", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
            }
            IconButton(onClick = { sons.basculer() }, modifier = Modifier.size(52.dp)) {
                Icon(
                    painter = painterResource(if (sons.coupe) R.drawable.ic_son_coupe else R.drawable.ic_son),
                    contentDescription = if (sons.coupe) "Remettre le son" else "Couper le son",
                    tint = Encre,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        if (enCours) DeroulementCalcul(seance, sons) else ResumeCalculs(seance, onQuitter)
    }

    if (confirmerArret) {
        AlertDialog(
            onDismissRequest = { confirmerArret = false },
            title = { Text("Arrêter le tri des colis ?") },
            text = {
                Text(
                    if (seance.compte && !seance.enRattrapage) {
                        "Tu pourras le reprendre plus tard. Le calcul en cours comptera 0 point."
                    } else {
                        "Tu pourras recommencer quand tu veux."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerArret = false
                    onQuitter()
                }) { Text("Arrêter", color = RougeFonce) }
            },
            dismissButton = { TextButton(onClick = { confirmerArret = false }) { Text("Continuer") } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun DeroulementCalcul(seance: SeanceCalcul, sons: Sons) {
    val etape = seance.etape
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (etape) {
                EtapeCalcul.TAPIS -> TapisRoulant(seance.calcul.texte, seance.passage, onBout = { seance.tempsEcoule() })
                EtapeCalcul.CASIERS -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Colis(seance.calcul.texte)
                    Spacer(Modifier.height(14.dp))
                    Compteur(seance.numero, onFini = { seance.tempsEcoule() })
                }
                EtapeCalcul.RECOPIE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Colis(seance.calcul.texte)
                    Spacer(Modifier.height(10.dp))
                    Text("va dans le casier", style = MaterialTheme.typography.titleMedium, color = EncreDouce)
                    PorteDeCasier(seance.calcul.resultat, ouverte = true)
                }
                EtapeCalcul.BRAVO -> TamponBravo(
                    texte = when {
                        seance.enRattrapage || !seance.compte -> "Bien trié !"
                        seance.dernierPoint >= 1.0 -> "+1 point"
                        else -> "+½ point"
                    },
                    sons = sons,
                    onFini = { seance.apresBravo() },
                )
                EtapeCalcul.FIN -> Unit
            }
        }

        val message = seance.message ?: when (etape) {
            EtapeCalcul.TAPIS -> "Tape le numéro du casier avant que le colis arrive au bout !"
            else -> ""
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = if (seance.message != null) RougeFonce else EncreDouce,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )

        if (etape == EtapeCalcul.CASIERS) {
            Spacer(Modifier.weight(1f))
            CasiersOuverts(seance.propositions, onChoix = { seance.choisir(it) })
            Spacer(Modifier.height(16.dp))
        } else {
            AffichageNumero(seance.saisie, actif = etape == EtapeCalcul.TAPIS || etape == EtapeCalcul.RECOPIE)
            Spacer(Modifier.weight(1f))
            PaveCalcul(
                onChiffre = { seance.taper(it) },
                onEffacer = { seance.effacer() },
                onValider = { seance.valider() },
                actif = etape == EtapeCalcul.TAPIS || etape == EtapeCalcul.RECOPIE,
            )
        }
    }
}

/** Le tapis roulant sert de chrono : le colis doit être trié avant d'arriver au bout (10 s). */
@Composable
private fun TapisRoulant(texte: String, passage: Int, onBout: () -> Unit) {
    val bout by rememberUpdatedState(onBout)
    val avance = remember(passage) { Animatable(0f) }
    LaunchedEffect(passage) {
        avance.animateTo(1f, tween(durationMillis = CHRONO_MS, easing = LinearEasing))
        bout()
    }
    val densite = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largeur = constraints.maxWidth.toFloat()
        val largeurColis = with(densite) { 150.dp.toPx() }
        // Les casiers du centre de tri, au fond.
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(6) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 52.dp)
                        .background(CasierFonce, RoundedCornerShape(6.dp))
                        .padding(4.dp)
                        .background(Casier, RoundedCornerShape(4.dp)),
                )
            }
        }
        // Le tapis, dont les bandes défilent.
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val h = size.height
            drawRoundRect(Tapis, size = Size(size.width, h), cornerRadius = CornerRadius(h / 2f))
            val pas = 28.dp.toPx()
            val decalage = (avance.value * pas * 18f) % pas
            var x = -pas + decalage
            while (x < size.width + pas) {
                drawLine(TapisClair, Offset(x, h * 0.25f), Offset(x + pas * 0.5f, h * 0.75f), strokeWidth = 4.dp.toPx())
                x += pas
            }
            // Le bout du tapis : la zone rouge à ne pas atteindre.
            drawRoundRect(
                Rouge.copy(alpha = 0.55f),
                topLeft = Offset(size.width - h * 1.4f, 0f),
                size = Size(h * 1.4f, h),
                cornerRadius = CornerRadius(h / 2f),
            )
        }
        Colis(
            texte,
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 30.dp)
                .offset { IntOffset(((largeur - largeurColis) * avance.value).roundToInt(), 0) },
        )
    }
}

/** Un colis en carton, avec l'étiquette du calcul. */
@Composable
private fun Colis(texte: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 150.dp, height = 100.dp)
            .background(CartonFonce, RoundedCornerShape(10.dp))
            .padding(bottom = 5.dp)
            .background(Carton, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // le ruban adhésif
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 66.dp)
                .background(CartonFonce.copy(alpha = 0.55f)),
        )
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 1.dp) {
            Text(
                texte,
                style = styleCalcul,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

/** Les 10 secondes de la seconde chance. */
@Composable
private fun Compteur(cle: Int, onFini: () -> Unit) {
    val fini by rememberUpdatedState(onFini)
    val reste = remember(cle) { Animatable(1f) }
    LaunchedEffect(cle) {
        reste.animateTo(0f, tween(durationMillis = CHRONO_MS, easing = LinearEasing))
        fini()
    }
    LinearProgressIndicator(
        progress = { reste.value },
        modifier = Modifier
            .width(220.dp)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = if (reste.value > 0.3f) Vert else Rouge,
        trackColor = Color.White,
    )
}

/** La seconde chance : 4 casiers numérotés s'ouvrent, l'enfant touche le bon. */
@Composable
private fun CasiersOuverts(propositions: List<Int>, onChoix: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (rangee in propositions.chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (n in rangee) {
                    val vue = LocalView.current
                    Surface(
                        onClick = {
                            vue.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onChoix(n)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = CasierFonce,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                    ) {
                        Box(
                            Modifier
                                .padding(7.dp)
                                .background(CielBas, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(n.toString(), style = styleNumero)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PorteDeCasier(numero: Int, ouverte: Boolean) {
    Box(
        Modifier
            .size(width = 110.dp, height = 70.dp)
            .background(CasierFonce, RoundedCornerShape(12.dp))
            .padding(6.dp)
            .background(if (ouverte) Jaune.copy(alpha = 0.5f) else Casier, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(numero.toString(), style = styleNumero)
    }
}

@Composable
private fun AffichageNumero(saisie: String, actif: Boolean) {
    val clignote = rememberInfiniteTransition(label = "curseur")
    val visible = clignote.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "visible",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Casier n°", style = MaterialTheme.typography.titleLarge, color = EncreDouce)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(width = 130.dp, height = 70.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(3.dp, Encre.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(saisie, style = styleNumero)
                if (actif) {
                    Box(
                        Modifier
                            .padding(start = 2.dp)
                            .width(3.dp)
                            .height(40.dp)
                            .graphicsLayer { alpha = visible.value }
                            .background(Rouge),
                    )
                }
            }
        }
    }
}

/** Le pavé numérique du tri des colis, avec la touche Valider. */
@Composable
private fun PaveCalcul(onChiffre: (Char) -> Unit, onEffacer: () -> Unit, onValider: () -> Unit, actif: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .graphicsLayer { alpha = if (actif) 1f else 0.45f },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (ligne in listOf("123", "456", "789")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (c in ligne) {
                    ToucheCalcul(onClick = { if (actif) onChiffre(c) }) {
                        Text(c.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToucheCalcul(onClick = { if (actif) onEffacer() }, couleur = CielBas) {
                Icon(painterResource(R.drawable.ic_effacer), contentDescription = "Effacer", modifier = Modifier.size(28.dp))
            }
            ToucheCalcul(onClick = { if (actif) onChiffre('0') }) {
                Text("0", style = MaterialTheme.typography.headlineMedium)
            }
            ToucheCalcul(onClick = { if (actif) onValider() }, couleur = Vert, contenuCouleur = Color.White) {
                Text("OK", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun RowScope.ToucheCalcul(
    onClick: () -> Unit,
    couleur: Color = Color.White,
    contenuCouleur: Color = Encre,
    contenu: @Composable () -> Unit,
) {
    val vue = LocalView.current
    Surface(
        onClick = {
            vue.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = couleur,
        contentColor = contenuCouleur,
        shadowElevation = 2.dp,
        modifier = Modifier
            .weight(1f)
            .height(62.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { contenu() }
    }
}

@Composable
private fun ResumeCalculs(seance: SeanceCalcul, onRetour: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        val medaille = seance.medaille
        if (medaille != Medaille.AUCUNE) {
            DessinMedaille(medaille, Modifier.size(130.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (seance.compte) {
            Text("${pointsEnTexte(seance.points)} / ${seance.ordre.size}", style = MaterialTheme.typography.displaySmall, color = Encre)
            Text(
                when (medaille) {
                    Medaille.OR -> "Médaille d'or : aucune erreur !"
                    Medaille.ARGENT -> "Médaille d'argent : une seule erreur."
                    Medaille.AUCUNE -> "Les calculs ratés reviendront les jours suivants."
                },
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
        } else {
            val justes = seance.resultats.count { it.points >= 1.0 }
            Text("Entraînement terminé", style = MaterialTheme.typography.headlineMedium, color = Encre)
            Text(
                "$justes colis bien triés du premier coup sur ${seance.ordre.size}",
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Carte(Modifier.fillMaxWidth()) {
            for (r in seance.resultats) {
                val calcul = Calcul.lire(r.mot)
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (signe, couleur) = when {
                        r.points >= 1.0 -> "✓" to Vert
                        r.points > 0.0 -> "½" to Jaune
                        else -> "✗" to RougeFonce
                    }
                    Box(Modifier.size(32.dp).background(couleur, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text(signe, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (calcul != null) "${calcul.texte} = ${calcul.resultat}" else r.mot,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (seance.compte) Text(pointsEnTexte(r.points), style = MaterialTheme.typography.titleMedium, color = EncreDouce)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        BoutonPrincipal(texte = "Retour à l'accueil", onClick = onRetour, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
