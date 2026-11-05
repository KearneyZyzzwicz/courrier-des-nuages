package fr.courrierdesnuages.app.ui.parent

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R
import fr.courrierdesnuages.app.calcul.Calcul
import fr.courrierdesnuages.app.calcul.FOIS
import fr.courrierdesnuages.app.calcul.PLUS
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.lecture.LecteurDeListe
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MOTS_MIN = 3
const val MOTS_MAX = 20

/** L'écran « Ce soir » d'un enfant : la liste de la semaine, les mots du jour, l'envoi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EcranCeSoir(vm: AppViewModel, profil: Profil) {
    val contexte = LocalContext.current
    val donnees = vm.donnees(profil.id)
    val aujourdhui = remember { Jours.aujourdhui() }
    val soirDuJour = donnees.soir(aujourdhui)
    val choisis = remember { mutableStateListOf<String>() }
    val tables = remember { mutableStateListOf<String>() }
    var initialise by remember { mutableStateOf(false) }
    var nouveauMot by remember { mutableStateOf("") }
    var probleme by remember { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }

    // Ce soir déjà envoyé : on repart des mots envoyés pour pouvoir les modifier.
    LaunchedEffect(donnees.soirsCharges) {
        if (donnees.soirsCharges && !initialise) {
            donnees.soir(aujourdhui)?.let { soir ->
                choisis.addAll(soir.mots)
                tables.addAll(soir.tables)
            }
            initialise = true
        }
    }

    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { reussi ->
        vm.finActiviteExterne()
        val u = photo
        if (reussi && u != null) vm.ouvrirLecture(profil, u)
    }
    val galerie = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.finActiviteExterne()
        if (uri != null) vm.ouvrirLecture(profil, uri.toString())
    }

    fun estChoisi(mot: String) = choisis.any { Mots.identiques(it, mot) }

    fun basculer(mot: String) {
        val i = choisis.indexOfFirst { Mots.identiques(it, mot) }
        if (i >= 0) {
            choisis.removeAt(i)
        } else if (choisis.size < MOTS_MAX) {
            choisis += mot
        }
    }

    fun ajouterALaMain() {
        val mot = Mots.normaliser(nouveauMot)
        if (mot.isEmpty()) return
        if (!estChoisi(mot) && choisis.size < MOTS_MAX) choisis += mot
        if (profil.liste.none { Mots.identiques(it, mot) }) vm.enregistrerListe(profil, profil.liste + mot, retour = false)
        nouveauMot = ""
    }

    // Les mots déjà donnés depuis que la liste de la semaine a été enregistrée.
    val dejaDonnes = donnees.soirs
        .filter { it.date != aujourdhui && it.envoyeLe >= profil.listeLe }
        .flatMap { it.mots }
        .map { Mots.normaliser(it) }
        .toSet()
    val tousLesMots = Mots.sansDoublons(profil.liste + choisis)
    val aRevoir = donnees.revisions.map { it.mot }.filter { r -> choisis.none { Mots.identiques(it, r) } }
    val dejaFaite = donnees.seance(aujourdhui, Matiere.FRANCAIS)?.terminee == true
    val mathsDejaFaites = donnees.seance(aujourdhui, Matiere.MATHS)?.terminee == true
    // Des mots (de 3 à 20) et/ou des tables : un soir peut n'avoir que l'un des deux.
    val motsValides = choisis.isEmpty() || choisis.size in MOTS_MIN..MOTS_MAX
    val peutEnvoyer = motsValides && (choisis.isNotEmpty() || tables.isNotEmpty())

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Ce soir · ${profil.prenom}", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImageAvatar(profil.avatar, taille = 56.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "${Jours.enClair(aujourdhui).replaceFirstChar { it.uppercase() }} · ${profil.classe}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EncreDouce,
                )
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Liste de la semaine", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (profil.liste.isEmpty()) {
                        "Pas encore de liste. Prends en photo la feuille de mots de l'école."
                    } else {
                        "${profil.liste.size} mots, enregistrée le ${dateCourte(profil.listeLe)}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                Spacer(Modifier.height(12.dp))
                BoutonPrincipal(
                    texte = "Photographier la liste",
                    onClick = {
                        val u = LecteurDeListe.nouvellePhoto(contexte)
                        photo = u.toString()
                        probleme = null
                        vm.debutActiviteExterne()
                        try {
                            appareil.launch(u)
                        } catch (e: ActivityNotFoundException) {
                            vm.finActiviteExterne()
                            probleme = "Aucun appareil photo trouvé sur ce téléphone."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                BoutonSecondaire(
                    texte = "Choisir une image",
                    onClick = {
                        vm.debutActiviteExterne()
                        galerie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (profil.liste.isNotEmpty()) {
                    TextButton(onClick = { vm.ouvrirLecture(profil, null) }) { Text("Corriger la liste à la main") }
                }
                val p = probleme
                if (p != null) Text(p, color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Mots du jour", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Touche les mots à travailler ce soir (de $MOTS_MIN à $MOTS_MAX, ou aucun). ✓ : déjà donné cette semaine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (mot in tousLesMots) {
                        PuceMot(
                            mot = mot,
                            choisi = estChoisi(mot),
                            dejaDonne = Mots.normaliser(mot) in dejaDonnes,
                            onClick = { basculer(mot) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nouveauMot,
                        onValueChange = { nouveauMot = it.take(40) },
                        label = { Text("Ajouter un mot") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { ajouterALaMain() }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { ajouterALaMain() }) { Text("Ajouter") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${choisis.size} mot${if (choisis.size > 1) "s" else ""} choisi${if (choisis.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (choisis.size in MOTS_MIN..MOTS_MAX) Vert else EncreDouce,
                )
                if (aRevoir.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "S'ajoutent d'office, à revoir : ${aRevoir.joinToString(", ")}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Tables de maths", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Touche les tables à faire ce soir : 10 calculs par table. Aucune table : pas de séance de maths.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                for ((operation, titre) in listOf(FOIS to "Multiplication", PLUS to "Addition")) {
                    Spacer(Modifier.height(10.dp))
                    Text(titre, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (n in 1..10) {
                            val code = Calcul.codeTable(operation, n)
                            Pastille(
                                texte = "$operation$n",
                                choisie = code in tables,
                                onClick = { if (code in tables) tables.remove(code) else tables += code },
                            )
                        }
                    }
                }
                if (tables.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val calculs = Calcul.pourLesTables(tables).size
                    Text(
                        "$calculs calculs" + if (donnees.revisionsMaths.isNotEmpty()) ", plus ${donnees.revisionsMaths.size} à revoir." else ".",
                        style = MaterialTheme.typography.titleMedium,
                        color = Vert,
                    )
                }
            }

            if (soirDuJour != null) {
                Text(
                    "Déjà envoyé ce soir à ${heure(soirDuJour.envoyeLe)}. Tu peux modifier puis renvoyer." +
                        (if (dejaFaite) " ${profil.prenom} a déjà fait la dictée : de nouveaux mots serviraient d'entraînement." else "") +
                        (if (mathsDejaFaites) " Les maths sont déjà faites : de nouvelles tables serviraient d'entraînement." else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
            }
            BoutonPrincipal(
                texte = "Envoyer à ${profil.prenom}",
                onClick = { vm.envoyerSoir(profil, choisis.toList(), tables.toList()) },
                actif = peutEnvoyer,
                couleur = Vert,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PuceMot(mot: String, choisi: Boolean, dejaDonne: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (choisi) Encre else Color.White,
        contentColor = if (choisi) Color.White else if (dejaDonne) EncreDouce else Encre,
        border = if (choisi) null else BorderStroke(1.5.dp, Encre.copy(alpha = 0.3f)),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dejaDonne && !choisi) {
                    Text("✓ ", style = MaterialTheme.typography.labelLarge, color = Vert)
                }
                Text(mot, style = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 18.sp))
            }
        }
    }
}

private fun dateCourte(millis: Long): String =
    if (millis <= 0L) "?" else SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(Date(millis))

private fun heure(millis: Long): String =
    if (millis <= 0L) "?" else SimpleDateFormat("HH'h'mm", Locale.FRENCH).format(Date(millis))

@Composable
fun IconePhoto(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_photo), contentDescription = null, modifier = modifier.size(20.dp))
}
