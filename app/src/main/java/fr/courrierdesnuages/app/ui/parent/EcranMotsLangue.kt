package fr.courrierdesnuages.app.ui.parent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.langues.Langue
import fr.courrierdesnuages.app.langues.MOTS_PAR_JOUR
import fr.courrierdesnuages.app.langues.ModeReponse
import fr.courrierdesnuages.app.langues.MotLangue
import fr.courrierdesnuages.app.langues.SemaineLangue
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import java.time.LocalDate

private val NOMS_DES_JOURS = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
private val JOURS_COURTS = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

/** Une ligne en cours de saisie. */
private data class Ligne(val numero: Long, val jour: Int, val francais: String, val traduction: String, val mode: ModeReponse) {
    val complete: Boolean get() = francais.isNotBlank() && traduction.isNotBlank()
    val vide: Boolean get() = francais.isBlank() && traduction.isBlank()
}

/**
 * Le parent prépare les mots d'une langue jour par jour (de 1 à 10 par jour, les mêmes pour les
 * trois enfants), et choisit pour chaque mot comment l'enfant répond.
 */
@Composable
fun EcranMotsLangue(vm: AppViewModel, langue: Langue) {
    val aujourdhui = remember { LocalDate.now() }
    val courante = Semaines.lundi(aujourdhui).toString()
    val suivante = Semaines.lundi(aujourdhui).plusWeeks(1).toString()
    var lundi by remember { mutableStateOf(courante) }
    var jour by remember { mutableStateOf(Semaines.numeroDuJour(aujourdhui)) }
    // Les lignes de chaque semaine affichée, gardées tant que l'écran est ouvert.
    val lignes = remember { mutableStateMapOf<String, List<Ligne>>() }
    val modifiees = remember { mutableStateMapOf<String, Boolean>() }
    var compteur by remember { mutableStateOf(0L) }
    val charge = vm.semaineChargee(langue, lundi)

    LaunchedEffect(lundi, charge) {
        if (charge && lundi !in lignes) {
            val semaine = vm.semaine(langue, lundi)
            lignes[lundi] = (1..7).flatMap { j ->
                semaine?.jours?.get(j).orEmpty().map { m -> Ligne(compteur++, j, m.francais, m.traduction, m.mode) }
            }
        }
    }

    val toutes = lignes[lundi].orEmpty()
    val duJour = toutes.filter { it.jour == jour }

    fun changer(nouvelles: List<Ligne>) {
        lignes[lundi] = nouvelles
        modifiees[lundi] = true
    }

    fun remplacer(l: Ligne) = changer(toutes.map { if (it.numero == l.numero) l else it })

    val incompletes = lignes.values.flatten().count { !it.complete && !it.vide }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Mots ${langue.deLaLangue}", onRetour = { vm.retour() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pastille(texte = "Cette semaine", choisie = lundi == courante, onClick = {
                    lundi = courante
                    jour = Semaines.numeroDuJour(aujourdhui)
                })
                Pastille(texte = "Semaine prochaine", choisie = lundi == suivante, onClick = {
                    lundi = suivante
                    jour = 1
                })
            }
            Text(
                "Semaine du ${Jours.enClair(lundi)}. De 1 à $MOTS_PAR_JOUR mots par jour, les mêmes pour les trois enfants. " +
                    "Un jour sans séance n'est pas une faute : ses mots attendent la séance suivante.",
                style = MaterialTheme.typography.bodyLarge,
                color = EncreDouce,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (j in 1..7) {
                    val nombre = toutes.count { it.jour == j && it.complete }
                    val date = LocalDate.parse(lundi).plusDays((j - 1).toLong()).dayOfMonth
                    Pastille(
                        texte = "${JOURS_COURTS[j - 1]} $date" + if (nombre > 0) " · $nombre" else "",
                        choisie = j == jour,
                        onClick = { jour = j },
                    )
                }
            }
            if (!charge) {
                Text("Chargement…", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            } else {
                Text(NOMS_DES_JOURS[jour - 1], style = MaterialTheme.typography.titleLarge)
                if (duJour.isEmpty()) {
                    Text("Pas encore de mot ce jour-là.", style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
                duJour.forEachIndexed { i, ligne ->
                    LigneDeMot(
                        numero = i + 1,
                        ligne = ligne,
                        langue = langue,
                        onChange = { remplacer(it) },
                        onSupprimer = { changer(toutes.filter { it.numero != ligne.numero }) },
                    )
                }
                if (duJour.size < MOTS_PAR_JOUR) {
                    BoutonSecondaire(
                        texte = "Ajouter un mot",
                        onClick = { changer(toutes + Ligne(compteur++, jour, "", "", ModeReponse.VIGNETTES)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    "Vignettes : l'enfant choisit parmi 4 mots de la semaine (un seul essai). " +
                        "Écrire : il tape le mot (deux essais, puis les lettres mélangées). " +
                        "Lettres : il remet les lettres du mot dans l'ordre (deux essais).",
                    style = MaterialTheme.typography.bodySmall,
                    color = EncreDouce,
                )
            }
            if (incompletes > 0) {
                Text(
                    "Il manque la traduction (ou le mot français) sur une ligne.",
                    color = RougeFonce,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BoutonPrincipal(
                texte = "Enregistrer",
                onClick = {
                    val semaines = modifiees.keys.map { l ->
                        SemaineLangue(
                            lundi = l,
                            jours = lignes[l].orEmpty().filter { it.complete }.groupBy { it.jour }.mapValues { (_, ls) ->
                                ls.take(MOTS_PAR_JOUR).map { MotLangue(it.francais, it.traduction, it.mode) }
                            },
                        )
                    }
                    vm.enregistrerMotsLangue(langue, semaines)
                },
                actif = modifiees.isNotEmpty() && incompletes == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LigneDeMot(numero: Int, ligne: Ligne, langue: Langue, onChange: (Ligne) -> Unit, onSupprimer: () -> Unit) {
    Carte(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mot $numero", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onSupprimer, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Retirer ce mot", tint = EncreDouce)
            }
        }
        OutlinedTextField(
            value = ligne.francais,
            onValueChange = { onChange(ligne.copy(francais = it.take(40))) },
            label = { Text("En français") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = ligne.traduction,
            onValueChange = { onChange(ligne.copy(traduction = it.take(40))) },
            label = { Text("En ${langue.adjectif}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in ModeReponse.entries) {
                Pastille(texte = m.nom, choisie = ligne.mode == m, onClick = { onChange(ligne.copy(mode = m)) })
            }
        }
    }
}
