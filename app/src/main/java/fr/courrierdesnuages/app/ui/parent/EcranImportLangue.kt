package fr.courrierdesnuages.app.ui.parent

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.langues.ListeDeMots
import fr.courrierdesnuages.app.langues.MOTS_PAR_JOUR
import fr.courrierdesnuages.app.langues.ModeReponse
import fr.courrierdesnuages.app.langues.MotLangue
import fr.courrierdesnuages.app.langues.PaireLue
import fr.courrierdesnuages.app.lecture.LecteurDeListe
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.Ecran
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.ecrans.Chargement
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException

private val JOURS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
private val JOURS_COURTS = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
private val PAR_JOUR = listOf(1, 2, 3, 5, MOTS_PAR_JOUR)
private val JOUR_ET_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

/** Un mot de la liste lue, en cours de vérification. */
private data class LigneLue(val numero: Long, val francais: String, val traduction: String) {
    val complete: Boolean get() = francais.isNotBlank() && traduction.isNotBlank()
    val vide: Boolean get() = francais.isBlank() && traduction.isBlank()
}

/**
 * Lecture d'une liste de mots de langue (photo, image enregistrée, fichier .txt ou texte collé) :
 * le parent vérifie les mots et leur traduction, choisit les jours et la façon de répondre, puis
 * les mots s'ajoutent à la semaine, après ceux déjà prévus.
 */
@Composable
fun EcranImportLangue(vm: AppViewModel, e: Ecran.ImportLangue) {
    val langue = e.langue
    val contexte = LocalContext.current
    val aujourdhui = remember { LocalDate.now() }
    val courante = Semaines.lundi(aujourdhui).toString()
    val suivante = Semaines.lundi(aujourdhui).plusWeeks(1).toString()
    val finDeSemaine = Semaines.numeroDuJour(aujourdhui) >= 6
    // Le samedi et le dimanche, la liste est le plus souvent pour la semaine qui vient.
    var lundi by remember { mutableStateOf(if (e.lundi == suivante || finDeSemaine) suivante else courante) }
    var depuis by remember { mutableStateOf(if (lundi == suivante) 1 else Semaines.numeroDuJour(aujourdhui)) }
    val lignes = remember { mutableStateListOf<LigneLue>() }
    var compteur by remember { mutableStateOf(0L) }
    val modeTexte = e.texte != null
    var enLecture by remember { mutableStateOf(e.image != null || e.fichierTexte != null) }
    var lue by remember { mutableStateOf(!modeTexte) }
    var texte by remember { mutableStateOf(e.texte.orEmpty()) }
    var probleme by remember { mutableStateOf<String?>(null) }
    var apercu by remember { mutableStateOf<ImageBitmap?>(null) }
    var parJourChoisi by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(ModeReponse.VIGNETTES) }

    fun remplir(paires: List<PaireLue>) {
        lignes.clear()
        for (p in paires) lignes += LigneLue(compteur++, p.francais, p.traduction)
        probleme = if (paires.isEmpty()) {
            "Aucun mot trouvé. Il faut une ligne par mot, avec le mot et sa traduction séparés par un tiret, " +
                "un signe = ou deux-points (« Hello — Bonjour »), ou la traduction juste sous le mot. Tu peux aussi les ajouter à la main."
        } else {
            null
        }
        lue = true
    }

    LaunchedEffect(e) {
        try {
            when {
                e.image != null -> {
                    val uri = Uri.parse(e.image)
                    apercu = LecteurDeListe.apercu(contexte, uri)?.asImageBitmap()
                    remplir(ListeDeMots.lire(LecteurDeListe.lireRangees(contexte, uri), langue))
                }
                e.fichierTexte != null -> {
                    val contenu = LecteurDeListe.lireTexte(contexte, Uri.parse(e.fichierTexte))
                    remplir(ListeDeMots.lireTexte(contenu, langue))
                }
            }
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            probleme = "La lecture a échoué : ${ex.message}"
            lue = true
        } finally {
            enLecture = false
        }
    }

    // Les mots déjà prévus cette semaine-là : pas de doublon, et au plus 10 mots par jour.
    val semaine = vm.semaine(langue, lundi)
    val chargee = vm.semaineChargee(langue, lundi)
    val deja = semaine?.tous.orEmpty().map { it.code.lowercase(Locale.FRENCH) }.toSet()
    val completes = lignes.filter { it.complete }.map { MotLangue(it.francais.trim(), it.traduction.trim(), mode) }
    val vus = HashSet<String>()
    val nouveaux = completes.filter { m -> m.code.lowercase(Locale.FRENCH).let { it !in deja && vus.add(it) } }
    val doublons = completes.filter { m -> m.code.lowercase(Locale.FRENCH) in deja }
    val parJour = parJourChoisi ?: ListeDeMots.parJourConseille(nouveaux.size, depuis)
    val dejaPrevus = (1..7).associateWith { j -> semaine?.jours?.get(j).orEmpty().size }
    val repartition = ListeDeMots.repartir(nouveaux, depuis, parJour, dejaPrevus)
    val incompletes = lignes.count { !it.complete && !it.vide }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Importer des mots ${langue.deLaLangue}", onRetour = { vm.retour() })
        if (enLecture) {
            Chargement("Lecture de la liste…")
        } else Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (modeTexte) {
                Carte(Modifier.fillMaxWidth()) {
                    Text("Le texte de la liste", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Appuie longuement dans la case, puis sur « Coller ». Une ligne par mot (« Hello — Bonjour », « le chat = il gatto »), ou le mot sur une ligne et sa traduction en dessous.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it.take(4000) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    BoutonPrincipal(
                        texte = if (lue) "Relire le texte" else "Lire le texte",
                        onClick = { remplir(ListeDeMots.lireTexte(texte, langue)) },
                        actif = texte.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val a = apercu
            if (a != null) {
                Image(
                    bitmap = a,
                    contentDescription = "La liste",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            val p = probleme
            if (p != null) Text(p, color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
            if (lue) {
                Carte(Modifier.fillMaxWidth()) {
                    Text("Vérifie chaque mot", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Accents et déterminants comptent, pas les majuscules ni la ponctuation (« My name is… » s'écrit « My name is »). " +
                            "Plusieurs réponses possibles : sépare-les par « / ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                    Spacer(Modifier.height(6.dp))
                    lignes.forEachIndexed { i, ligne ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mot ${i + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { lignes.removeAll { it.numero == ligne.numero } }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Retirer ce mot", tint = EncreDouce)
                            }
                        }
                        OutlinedTextField(
                            value = ligne.francais,
                            onValueChange = { v -> remplacer(lignes, ligne.copy(francais = v.take(40))) },
                            label = { Text("En français") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ligne.traduction,
                            onValueChange = { v -> remplacer(lignes, ligne.copy(traduction = v.take(40))) },
                            label = { Text("En ${langue.adjectif}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = {
                                val inverses = lignes.map { it.copy(francais = it.traduction, traduction = it.francais) }
                                lignes.clear()
                                lignes.addAll(inverses)
                            },
                            enabled = lignes.isNotEmpty(),
                        ) { Text("⇄ Inverser les côtés") }
                        TextButton(onClick = { lignes += LigneLue(compteur++, "", "") }) { Text("+ Ajouter un mot") }
                    }
                    if (incompletes > 0) {
                        Text("Il manque la traduction (ou le mot français) sur une ligne.", color = RougeFonce, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (doublons.isNotEmpty()) {
                        Text(
                            "Déjà prévu${if (doublons.size > 1) "s" else ""} cette semaine-là, pas ajouté${if (doublons.size > 1) "s" else ""} : " +
                                doublons.joinToString(", ") { it.traduction } + ".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EncreDouce,
                        )
                    }
                }

                Carte(Modifier.fillMaxWidth()) {
                    Text("Quels jours ?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pastille(texte = "Cette semaine", choisie = lundi == courante, onClick = {
                            lundi = courante
                            depuis = Semaines.numeroDuJour(aujourdhui)
                        })
                        Pastille(texte = "Semaine prochaine", choisie = lundi == suivante, onClick = {
                            lundi = suivante
                            depuis = 1
                        })
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("À partir du :", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (j in 1..7) {
                            val date = LocalDate.parse(lundi).plusDays((j - 1).toLong()).dayOfMonth
                            Pastille(texte = "${JOURS_COURTS[j - 1]} $date", choisie = j == depuis, onClick = { depuis = j })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Mots par jour :", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (n in PAR_JOUR) {
                            Pastille(
                                texte = if (n == MOTS_PAR_JOUR) "Tous le même jour" else "$n",
                                choisie = n == parJour,
                                onClick = { parJourChoisi = n },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (!chargee) {
                        Text("Chargement de la semaine…", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                    } else {
                        for ((j, mots) in repartition.jours) {
                            val jour = LocalDate.parse(lundi).plusDays((j - 1).toLong()).format(JOUR_ET_DATE)
                            Text(
                                "${jour.replaceFirstChar { it.uppercase() }} : " + mots.joinToString(", ") { it.traduction },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                        if (repartition.restants.isNotEmpty()) {
                            Text(
                                "${repartition.restants.size} mot${if (repartition.restants.size > 1) "s" else ""} ne tien${if (repartition.restants.size > 1) "nent" else "t"} pas avant dimanche : " +
                                    "choisis plus de mots par jour ou un jour plus tôt.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RougeFonce,
                            )
                        }
                    }
                }

                Carte(Modifier.fillMaxWidth()) {
                    Text("Comment l'enfant répond", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (m in ModeReponse.entries) {
                            Pastille(texte = m.nom, choisie = mode == m, onClick = { mode = m })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pour tous ces mots : ${mode.explication}. Tu pourras le changer mot par mot ensuite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }

                val nombre = nouveaux.size - repartition.restants.size
                BoutonPrincipal(
                    texte = if (nombre > 0) "Ajouter $nombre mot${if (nombre > 1) "s" else ""} à la semaine" else "Ajouter à la semaine",
                    onClick = {
                        val jours = repartition.jours.keys.sorted().map { JOURS[it - 1] }
                        val resume = "$nombre mot${if (nombre > 1) "s" else ""} ${langue.deLaLangue} ajouté${if (nombre > 1) "s" else ""} " +
                            "(semaine du ${Jours.enClair(lundi)}) : " + jours.joinToString(", ") + ". Vérifie-les ci-dessous."
                        vm.ajouterMotsImportes(langue, lundi, repartition.jours, resume)
                    },
                    actif = chargee && nombre > 0 && incompletes == 0 && repartition.restants.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun remplacer(lignes: MutableList<LigneLue>, nouvelle: LigneLue) {
    val i = lignes.indexOfFirst { it.numero == nouvelle.numero }
    if (i >= 0) lignes[i] = nouvelle
}
