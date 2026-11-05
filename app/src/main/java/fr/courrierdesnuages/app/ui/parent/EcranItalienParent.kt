package fr.courrierdesnuages.app.ui.parent

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.italien.MotItalien
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.italien.VILLES
import fr.courrierdesnuages.app.italien.Ville
import fr.courrierdesnuages.app.lecture.Images
import fr.courrierdesnuages.app.lecture.LecteurDeListe
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import kotlinx.coroutines.launch
import java.time.LocalDate

private val JOURS = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi")

/** Le parent saisit les 5 mots d'italien de la semaine (les mêmes pour les trois enfants). */
@Composable
fun EcranMotsItalien(vm: AppViewModel) {
    val lundi = remember { Semaines.semaineAPreparer(LocalDate.now()).toString() }
    val francais = remember { mutableStateListOf("", "", "", "", "") }
    val italien = remember { mutableStateListOf("", "", "", "", "") }
    var initialise by remember { mutableStateOf(false) }
    val charge = vm.semaineChargee(lundi)

    LaunchedEffect(charge) {
        if (charge && !initialise) {
            vm.semaine(lundi)?.mots?.forEachIndexed { i, m ->
                if (i < 5) {
                    francais[i] = m.francais
                    italien[i] = m.italien
                }
            }
            initialise = true
        }
    }

    val complets = (0 until 5).count { francais[it].isNotBlank() && italien[it].isNotBlank() }
    val incomplets = (0 until 5).count { francais[it].isNotBlank() != italien[it].isNotBlank() }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Mots d'italien", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Semaine du ${Jours.enClair(lundi)} : un mot par soir, du lundi au vendredi, pour les trois enfants. " +
                    "Écris l'italien avec l'article et les accents (« il gatto », « la città »).",
                style = MaterialTheme.typography.bodyLarge,
                color = EncreDouce,
            )
            for (i in 0 until 5) {
                Carte(Modifier.fillMaxWidth()) {
                    Text(JOURS[i], style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = francais[i],
                        onValueChange = { francais[i] = it.take(40) },
                        label = { Text("En français") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = italien[i],
                        onValueChange = { italien[i] = it.take(40) },
                        label = { Text("En italien") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = if (i < 4) ImeAction.Next else ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (incomplets > 0) {
                Text("Il manque la traduction (ou le mot français) sur une ligne.", color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
            }
            BoutonPrincipal(
                texte = "Enregistrer les $complets mot${if (complets > 1) "s" else ""}",
                onClick = { vm.enregistrerMotsItalien(lundi, (0 until 5).map { MotItalien(francais[it], italien[it]) }) },
                actif = complets > 0 && incomplets == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Le parent crée une carte postale : la ville (dans la liste), une image du téléphone et un petit texte. */
@Composable
fun EcranNouvelleCarte(vm: AppViewModel) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var recherche by remember { mutableStateOf("") }
    var ville by remember { mutableStateOf<Ville?>(null) }
    var texte by remember { mutableStateOf("") }
    var motFr by remember { mutableStateOf("") }
    var motIt by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<String?>(null) }
    var apercu by remember { mutableStateOf<ImageBitmap?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var probleme by remember { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }

    fun charger(uri: Uri) {
        enCours = true
        probleme = null
        portee.launch {
            val compressee = Images.compresser(contexte, uri)
            if (compressee == null) {
                probleme = "Impossible de lire cette image."
            } else {
                image = compressee
                apercu = Images.decoder(compressee)?.asImageBitmap()
            }
            enCours = false
        }
    }

    val galerie = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.finActiviteExterne()
        if (uri != null) charger(uri)
    }
    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { reussi ->
        vm.finActiviteExterne()
        val u = photo
        if (reussi && u != null) charger(Uri.parse(u))
    }

    val trouvees = if (recherche.isBlank()) emptyList() else VILLES.filter {
        it.nom.contains(recherche.trim(), ignoreCase = true) || it.region.contains(recherche.trim(), ignoreCase = true)
    }.take(8)
    val motsValides = motFr.isBlank() == motIt.isBlank()

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Nouvelle carte postale", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Carte(Modifier.fillMaxWidth()) {
                Text("La ville", style = MaterialTheme.typography.titleMedium)
                val v = ville
                if (v != null) {
                    Text("${v.nom} (${v.region})", style = MaterialTheme.typography.titleLarge, color = Encre)
                    BoutonSecondaire(texte = "Changer de ville", onClick = { ville = null })
                } else {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = { Text("Chercher une ville ou une région") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    for (t in trouvees) {
                        Pastille(texte = "${t.nom} · ${t.region}", choisie = false, onClick = { ville = t }, modifier = Modifier.padding(vertical = 3.dp))
                    }
                    if (recherche.isNotBlank() && trouvees.isEmpty()) {
                        Text("Cette ville n'est pas dans la liste : choisis une ville proche.", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                    }
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("L'image (recto)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val a = apercu
                if (a != null) {
                    Image(
                        a,
                        contentDescription = "Image de la carte",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(900f / 586f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoutonSecondaire(
                        texte = "Galerie",
                        onClick = {
                            vm.debutActiviteExterne()
                            galerie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    BoutonSecondaire(
                        texte = "Photo",
                        onClick = {
                            val u = LecteurDeListe.nouvellePhoto(contexte)
                            photo = u.toString()
                            vm.debutActiviteExterne()
                            try {
                                appareil.launch(u)
                            } catch (e: ActivityNotFoundException) {
                                vm.finActiviteExterne()
                                probleme = "Aucun appareil photo trouvé sur ce téléphone."
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (enCours) Text("Préparation de l'image…", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Le petit mot du pigeon (verso)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it.take(400) },
                    label = { Text("Ce que le pigeon raconte sur la ville") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Un mot à apprendre (facultatif)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(motFr, { motFr = it.take(30) }, label = { Text("Français") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(motIt, { motIt = it.take(30) }, label = { Text("Italien") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            val p = probleme
            if (p != null) Text(p, color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
            val choisie = ville
            BoutonPrincipal(
                texte = "Ajouter la carte",
                onClick = {
                    val img = image
                    if (choisie != null && img != null) vm.creerCarte(choisie, texte, motFr, motIt, img)
                },
                actif = choisie != null && image != null && texte.isNotBlank() && motsValides && !enCours,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "La carte rejoint les cartes à gagner des trois enfants.",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
