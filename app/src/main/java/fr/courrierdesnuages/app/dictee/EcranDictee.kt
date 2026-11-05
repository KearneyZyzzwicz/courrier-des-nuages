package fr.courrierdesnuages.app.dictee

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.pointsEnTexte
import fr.courrierdesnuages.app.son.Sons
import fr.courrierdesnuages.app.son.Voix
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** L'écran de la dictée de l'avion, du premier mot au résumé. */
@Composable
fun EcranDictee(
    seance: SeanceDictee,
    sons: Sons,
    voix: Voix,
    onQuitter: () -> Unit,
) {
    var confirmerArret by remember { mutableStateOf(false) }
    val enCours = seance.phase != Phase.FIN
    BackHandler { if (enCours) confirmerArret = true else onQuitter() }

    // L'écran reste allumé : sinon, il s'éteindrait pendant que l'enfant réfléchit.
    val vue = LocalView.current
    DisposableEffect(vue) {
        vue.keepScreenOn = true
        onDispose { vue.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize()) {
        BarreDictee(
            seance = seance,
            sons = sons,
            onArreter = { if (enCours) confirmerArret = true else onQuitter() },
        )
        if (!enCours) {
            ResumeDictee(seance, onRetour = onQuitter)
        } else {
            DeroulementMot(seance, sons, voix)
        }
    }

    if (confirmerArret) {
        AlertDialog(
            onDismissRequest = { confirmerArret = false },
            title = { Text("Arrêter la dictée ?") },
            text = {
                Text(
                    if (seance.compte && !seance.enRattrapage) {
                        "Tu pourras la reprendre plus tard. Le mot en cours comptera 0 point."
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
            dismissButton = {
                TextButton(onClick = { confirmerArret = false }) { Text("Continuer") }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun BarreDictee(seance: SeanceDictee, sons: Sons, onArreter: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onArreter, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Arrêter", tint = Encre, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    seance.enRattrapage -> "On revoit les mots ratés"
                    seance.compte -> "La dictée de l'avion"
                    else -> "Entraînement"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Encre,
            )
            if (seance.phase != Phase.FIN) {
                Text(
                    "Mot ${seance.numero} sur ${seance.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
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
}

@Composable
private fun DeroulementMot(seance: SeanceDictee, sons: Sons, voix: Voix) {
    val phase = seance.phase
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (phase) {
                Phase.AVION -> PassageAvion(
                    mot = seance.mot,
                    passage = seance.passage,
                    onDebut = { sons.demarrerMoteur() },
                    onFini = {
                        sons.arreterMoteur()
                        seance.avionPasse()
                        voix.dire(seance.mot)
                    },
                )
                Phase.RECOPIE -> ModeleARecopier(seance.mot, onEcouter = { voix.dire(seance.mot) })
                Phase.BRAVO -> TamponBravo(
                    texte = when {
                        seance.enRattrapage || !seance.compte -> "Juste !"
                        seance.dernierPoint >= 1.0 -> "+1 point"
                        else -> "+½ point"
                    },
                    sons = sons,
                    onFini = { seance.apresBravo() },
                )
                else -> BoutonReecouter(onClick = { voix.dire(seance.mot) })
            }
        }

        val message = seance.message ?: voix.probleme ?: when (phase) {
            Phase.AVION -> "Lis bien le mot sur la banderole…"
            Phase.SAISIE -> "Écris le mot, puis appuie sur Valider."
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

        if (phase == Phase.SECONDE_CHANCE) {
            CasesSecondeChance(seance)
        } else {
            Ardoise(
                texte = seance.saisie,
                mot = seance.mot,
                juste = phase == Phase.BRAVO,
                actif = phase == Phase.SAISIE || phase == Phase.RECOPIE,
            )
        }

        Spacer(Modifier.weight(1f))

        if (phase == Phase.SECONDE_CHANCE) {
            TuilesSecondeChance(seance)
        } else {
            ClavierAzerty(
                onTexte = { seance.taper(it) },
                onEffacer = { seance.effacer() },
                onValider = { seance.valider() },
                actif = phase == Phase.SAISIE || phase == Phase.RECOPIE,
            )
        }
    }
}

/** Taille du texte selon la longueur du mot, pour qu'il tienne sur une ligne. */
private fun tailleSelon(mot: String) = when {
    mot.length <= 11 -> 34.sp
    mot.length <= 15 -> 29.sp
    mot.length <= 19 -> 25.sp
    else -> 21.sp
}

@Composable
private fun Ardoise(texte: String, mot: String, juste: Boolean, actif: Boolean) {
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
            .heightIn(min = 76.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    texte,
                    style = TextStyle(
                        fontFamily = Andika,
                        fontWeight = FontWeight.Bold,
                        fontSize = tailleSelon(mot),
                        color = if (juste) Vert else Encre,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                if (actif) {
                    Box(
                        Modifier
                            .padding(start = 2.dp)
                            .width(3.dp)
                            .height(38.dp)
                            .graphicsLayer { alpha = visible.value }
                            .background(Rouge),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoutonReecouter(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White,
        contentColor = Encre,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_ecouter), contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Text("Réécouter", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ModeleARecopier(mot: String, onEcouter: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Le bon mot :", style = MaterialTheme.typography.titleMedium, color = EncreDouce)
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = Jaune.copy(alpha = 0.35f), contentColor = Encre) {
            Text(
                mot,
                style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = tailleSelon(mot)),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
        TextButton(onClick = onEcouter) {
            Icon(painterResource(R.drawable.ic_ecouter), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("Réécouter")
        }
    }
}

// ---------- Seconde chance : les lettres mélangées ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CasesSecondeChance(seance: SeanceDictee) {
    val modele = Mots.normaliser(seance.mot)
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
                        Text(
                            lettre.toString(),
                            style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Encre),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuilesSecondeChance(seance: SeanceDictee) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
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

// ---------- Fin de séance ----------

@Composable
private fun ResumeDictee(seance: SeanceDictee, onRetour: () -> Unit) {
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
            Text(
                "${pointsEnTexte(seance.points)} / ${seance.ordre.size}",
                style = MaterialTheme.typography.displaySmall,
                color = Encre,
            )
            Text(
                when (medaille) {
                    Medaille.OR -> "Médaille d'or : aucune faute !"
                    Medaille.ARGENT -> "Médaille d'argent : une seule faute."
                    Medaille.AUCUNE -> "Les mots ratés reviendront les jours suivants."
                },
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
        } else {
            val justes = seance.resultats.count { it.points >= 1.0 }
            Text("Entraînement terminé", style = MaterialTheme.typography.headlineMedium, color = Encre)
            Text(
                "$justes mot${if (justes > 1) "s" else ""} juste${if (justes > 1) "s" else ""} du premier coup sur ${seance.ordre.size}",
                style = MaterialTheme.typography.titleMedium,
                color = EncreDouce,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Carte(Modifier.fillMaxWidth()) {
            for (r in seance.resultats) {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (signe, couleur) = when {
                        r.points >= 1.0 -> "✓" to Vert
                        r.points > 0.0 -> "½" to Jaune
                        else -> "✗" to RougeFonce
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(couleur, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(signe, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        r.mot,
                        style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Encre),
                        modifier = Modifier.weight(1f),
                    )
                    if (seance.compte) {
                        Text(pointsEnTexte(r.points), style = MaterialTheme.typography.titleMedium, color = EncreDouce)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        BoutonPrincipal(texte = "Retour à l'accueil", onClick = onRetour, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

/** Une médaille dessinée : ruban rouge, disque d'or ou d'argent, étoile au centre. */
@Composable
fun DessinMedaille(medaille: Medaille, modifier: Modifier = Modifier) {
    val (clair, fonce) = when (medaille) {
        Medaille.OR -> Color(0xFFF7D774) to Color(0xFFD9A41E)
        else -> Color(0xFFE6EBF2) to Color(0xFFA9B4C4)
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val centre = Offset(w / 2f, h * 0.62f)
        val r = w * 0.3f
        // ruban
        val ruban = Path().apply {
            moveTo(w * 0.30f, 0f)
            lineTo(w * 0.45f, 0f)
            lineTo(w * 0.56f, centre.y - r * 0.6f)
            lineTo(w * 0.44f, centre.y - r * 0.4f)
            close()
            moveTo(w * 0.70f, 0f)
            lineTo(w * 0.55f, 0f)
            lineTo(w * 0.44f, centre.y - r * 0.6f)
            lineTo(w * 0.56f, centre.y - r * 0.4f)
            close()
        }
        drawPath(ruban, Rouge)
        drawCircle(fonce, r, centre)
        drawCircle(clair, r * 0.82f, centre)
        // étoile à cinq branches
        val etoile = Path()
        for (k in 0 until 10) {
            val angle = -Math.PI / 2 + k * Math.PI / 5
            val rayon = if (k % 2 == 0) r * 0.52f else r * 0.22f
            val px = centre.x + (rayon * cos(angle)).toFloat()
            val py = centre.y + (rayon * sin(angle)).toFloat()
            if (k == 0) etoile.moveTo(px, py) else etoile.lineTo(px, py)
        }
        etoile.close()
        drawPath(etoile, fonce)
    }
}
