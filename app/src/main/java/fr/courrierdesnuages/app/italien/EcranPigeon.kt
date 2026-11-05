package fr.courrierdesnuages.app.italien

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.pointsEnTexte
import fr.courrierdesnuages.app.dictee.ClavierAzerty
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.TamponBravo
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import kotlin.math.roundToInt

val ACCENTS_ITALIENS = listOf("à", "è", "é", "ì", "ò", "ù")

private val Papier = Color(0xFFFFF6DE)
private val PapierFonce = Color(0xFFE9D9AF)

/** Le pigeon voyageur : il apporte le mot du soir, roulé dans le tube attaché à sa patte. */
@Composable
fun EcranPigeon(seance: SeanceItalien, sons: Sons, onQuitter: () -> Unit, onOuvrirCarte: () -> Unit) {
    var confirmerArret by remember { mutableStateOf(false) }
    val enCours = seance.etape != EtapeItalien.FIN
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
                        seance.enRattrapage -> "On revoit les mots ratés"
                        seance.compte -> "Le pigeon voyageur"
                        else -> "Entraînement"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Encre,
                )
                if (enCours && seance.total > 1) {
                    Text("Message ${seance.numero} sur ${seance.total}", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
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
        if (enCours) DeroulementItalien(seance, sons) else ResumeItalien(seance, onQuitter, onOuvrirCarte)
    }

    if (confirmerArret) {
        AlertDialog(
            onDismissRequest = { confirmerArret = false },
            title = { Text("Arrêter le pigeon voyageur ?") },
            text = {
                Text(
                    if (seance.compte && !seance.enRattrapage) {
                        "Tu pourras reprendre plus tard. Le mot en cours comptera 0 point."
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
private fun DeroulementItalien(seance: SeanceItalien, sons: Sons) {
    val etape = seance.etape
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (etape) {
                EtapeItalien.PIGEON -> ArriveeDuPigeon(seance.passage, onDerouler = { seance.deroule() })
                EtapeItalien.BRAVO -> TamponBravo(
                    texte = when {
                        seance.enRattrapage || !seance.compte -> "Bravissimo !"
                        seance.dernierPoint >= 1.0 -> "+1 point"
                        else -> "+½ point"
                    },
                    sons = sons,
                    onFini = { seance.apresBravo() },
                )
                else -> MessageDeroule(seance.mot.francais, italien = if (etape == EtapeItalien.RECOPIE) seance.mot.italien else null)
            }
        }

        val message = seance.message ?: when (etape) {
            EtapeItalien.PIGEON -> "Touche le tube pour dérouler le message."
            EtapeItalien.SAISIE -> "Écris ce mot en italien, puis appuie sur Valider."
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

        if (etape == EtapeItalien.SECONDE_CHANCE) {
            CasesItalien(seance)
            Spacer(Modifier.weight(1f))
            TuilesItalien(seance)
        } else {
            ArdoiseItalien(seance.saisie, juste = etape == EtapeItalien.BRAVO, actif = etape == EtapeItalien.SAISIE || etape == EtapeItalien.RECOPIE)
            Spacer(Modifier.weight(1f))
            ClavierAzerty(
                onTexte = { seance.taper(it) },
                onEffacer = { seance.effacer() },
                onValider = { seance.valider() },
                actif = etape == EtapeItalien.SAISIE || etape == EtapeItalien.RECOPIE,
                accents = ACCENTS_ITALIENS,
                accentsPlus = emptyList(),
            )
        }
    }
}

/** Le pigeon arrive en volant, se pose, et attend qu'on déroule le message de son tube. */
@Composable
private fun ArriveeDuPigeon(passage: Int, onDerouler: () -> Unit) {
    val densite = LocalDensity.current
    val vol = remember(passage) { Animatable(0f) }
    LaunchedEffect(passage) {
        vol.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
    }
    val battement = rememberInfiniteTransition(label = "tube")
    val pulsation = battement.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulsation",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largeur = constraints.maxWidth.toFloat()
        val taille = with(densite) { 150.dp.toPx() }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val depart = -taille
                    val arrivee = (largeur - taille) / 2f
                    val x = depart + (arrivee - depart) * vol.value
                    val y = (1f - vol.value) * -60f
                    IntOffset(x.roundToInt(), y.roundToInt())
                }
                .clickable(enabled = vol.value > 0.95f) { onDerouler() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.pigeon),
                contentDescription = "Le pigeon voyageur",
                modifier = Modifier.size(width = 150.dp, height = 132.dp),
            )
            // Le petit tube du message, attaché à la patte.
            Box(
                Modifier
                    .offset(y = (-14).dp)
                    .graphicsLayer {
                        val p = if (vol.value > 0.95f) pulsation.value else 1f
                        scaleX = p
                        scaleY = p
                        rotationZ = -8f
                    }
                    .size(width = 56.dp, height = 22.dp)
                    .background(Papier, RoundedCornerShape(11.dp))
                    .border(2.dp, PapierFonce, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 8.dp, height = 22.dp)
                        .background(Rouge),
                )
            }
        }
    }
}

/** Le message déroulé : le mot en français, qui reste affiché. */
@Composable
private fun MessageDeroule(francais: String, italien: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Papier,
            contentColor = Encre,
            border = BorderStroke(2.dp, PapierFonce),
            shadowElevation = 2.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("En français", style = MaterialTheme.typography.labelMedium, color = EncreDouce)
                Text(
                    francais,
                    style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Encre),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (italien != null) {
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Jaune.copy(alpha = 0.35f), contentColor = Encre) {
                Text(
                    "En italien : $italien",
                    style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArdoiseItalien(texte: String, juste: Boolean, actif: Boolean) {
    val clignote = rememberInfiniteTransition(label = "curseur")
    val visible = clignote.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "visible",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(3.dp, if (juste) Vert else Encre.copy(alpha = 0.18f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 72.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    texte,
                    style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = if (juste) Vert else Encre),
                    maxLines = 1,
                    softWrap = false,
                )
                if (actif) {
                    Box(
                        Modifier
                            .padding(start = 2.dp)
                            .width(3.dp)
                            .height(36.dp)
                            .graphicsLayer { alpha = visible.value }
                            .background(Rouge),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CasesItalien(seance: SeanceItalien) {
    val modele = Mots.normaliser(seance.mot.italien)
    val lettres = seance.posees.map { seance.tirage[it] }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var i = 0
        for (c in modele) {
            if (c == ' ') {
                Spacer(Modifier.width(16.dp))
            } else {
                val lettre = lettres.getOrNull(i)
                i++
                Box(
                    Modifier
                        .size(width = 34.dp, height = 46.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(2.dp, if (lettre != null) Encre else Encre.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (lettre != null) {
                        Text(lettre.toString(), style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Encre))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuilesItalien(seance: SeanceItalien) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seance.tirage.forEachIndexed { position, lettre ->
                val utilisee = position in seance.posees
                Surface(
                    onClick = { seance.poserTuile(position) },
                    enabled = !utilisee,
                    shape = RoundedCornerShape(12.dp),
                    color = if (utilisee) CielBas else Jaune,
                    contentColor = Encre,
                    shadowElevation = if (utilisee) 0.dp else 2.dp,
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            lettre.toString(),
                            style = TextStyle(
                                fontFamily = Andika,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = if (utilisee) Encre.copy(alpha = 0.25f) else Encre,
                            ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BoutonSecondaire(texte = "Effacer", onClick = { seance.retirerTuile() })
            BoutonPrincipal(
                texte = "Valider",
                onClick = { seance.validerTuiles() },
                actif = seance.posees.size == seance.tirage.size,
                couleur = Vert,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ResumeItalien(seance: SeanceItalien, onRetour: () -> Unit, onOuvrirCarte: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        val carte = seance.carteGagnee
        if (carte != null) {
            Image(painterResource(R.drawable.pigeon), contentDescription = null, modifier = Modifier.size(140.dp))
            Text(
                "Une semaine sans faute !",
                style = MaterialTheme.typography.headlineMedium,
                color = Encre,
                textAlign = TextAlign.Center,
            )
            Text(
                "Le pigeon t'apporte une carte postale d'Italie.",
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            BoutonPrincipal(texte = "Ouvrir la carte postale", onClick = onOuvrirCarte, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
        }
        if (seance.compte) {
            Text("${pointsEnTexte(seance.points)} / ${seance.ordre.size}", style = MaterialTheme.typography.displaySmall, color = Encre)
            Text(
                if (seance.fautes == 0) "Tout juste du premier coup, bravissimo !" else "Le mot pas su reviendra demain.",
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
        } else {
            Text("Entraînement terminé", style = MaterialTheme.typography.headlineMedium, color = Encre)
        }
        Spacer(Modifier.height(16.dp))
        Carte(Modifier.fillMaxWidth()) {
            for (r in seance.resultats) {
                val mot = MotItalien.lire(r.mot) ?: continue
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
                    Column(Modifier.weight(1f)) {
                        Text(mot.italien, style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Encre))
                        Text(mot.francais, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                    }
                    if (seance.compte) Text(pointsEnTexte(r.points), style = MaterialTheme.typography.titleMedium, color = EncreDouce)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (carte == null) {
            BoutonPrincipal(texte = "Retour à l'accueil", onClick = onRetour, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }
}
