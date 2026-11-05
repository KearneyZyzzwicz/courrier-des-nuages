package fr.courrierdesnuages.app.ui.parent

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.lecture.LecteurDeListe
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.ecrans.Chargement
import fr.courrierdesnuages.app.ui.theme.Andika
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import kotlinx.coroutines.CancellationException

/**
 * Lecture de la photo de la liste : les mots trouvés s'affichent sous la photo,
 * le parent corrige, supprime ou ajoute, puis enregistre. Sans photo : correction de la liste actuelle.
 */
@Composable
fun EcranLectureListe(vm: AppViewModel, profil: Profil, image: String?) {
    val contexte = LocalContext.current
    val mots = remember { mutableStateListOf<String>() }
    var enLecture by remember { mutableStateOf(image != null) }
    var probleme by remember { mutableStateOf<String?>(null) }
    var apercu by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(image) {
        if (image == null) {
            mots.clear()
            mots.addAll(profil.liste)
            return@LaunchedEffect
        }
        val uri = Uri.parse(image)
        apercu = LecteurDeListe.apercu(contexte, uri)?.asImageBitmap()
        try {
            val lus = LecteurDeListe.lire(contexte, uri)
            mots.clear()
            mots.addAll(lus)
            if (lus.isEmpty()) probleme = "Aucun mot trouvé sur la photo. Essaie avec plus de lumière, ou ajoute les mots à la main."
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            probleme = "La lecture de la photo a échoué : ${e.message}"
        } finally {
            enLecture = false
        }
    }

    val propres = Mots.sansDoublons(mots)

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Liste de ${profil.prenom}", onRetour = { vm.retour() })
        if (enLecture) {
            Chargement("Lecture de la liste…")
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val a = apercu
                if (a != null) {
                    Image(
                        bitmap = a,
                        contentDescription = "Photo de la liste",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
                val p = probleme
                if (p != null) Text(p, color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
                Carte(Modifier.fillMaxWidth()) {
                    Text("Vérifie chaque mot", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Accents, majuscules et déterminant comptent : écris « une école » si la feuille dit « une école ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                    Spacer(Modifier.height(10.dp))
                    mots.forEachIndexed { i, mot ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mot,
                                onValueChange = { v -> if (i < mots.size) mots[i] = v.take(40) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 3.dp),
                            )
                            IconButton(onClick = { if (i < mots.size) mots.removeAt(i) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Supprimer ce mot", tint = EncreDouce)
                            }
                        }
                    }
                    TextButton(onClick = { mots.add("") }) { Text("+ Ajouter un mot") }
                }
                BoutonPrincipal(
                    texte = "Enregistrer la liste (${propres.size} mot${if (propres.size > 1) "s" else ""})",
                    onClick = { vm.enregistrerListe(profil, propres, retour = true) },
                    actif = propres.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
