package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Medaille
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.pointsEnTexte
import fr.courrierdesnuages.app.dictee.DessinMedaille
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.composants.AvatarAnime
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import java.time.LocalDate
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.Jaune
import fr.courrierdesnuages.app.ui.theme.Rouge
import fr.courrierdesnuages.app.ui.theme.Vert

/** L'accueil de l'enfant une fois son code tapé. */
@Composable
fun EcranAccueilEnfant(vm: AppViewModel, profil: Profil) {
    val donnees = vm.donnees(profil.id)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            BoutonSecondaire(
                texte = "Quitter",
                onClick = { vm.quitter() },
                icone = Icons.AutoMirrored.Filled.ExitToApp,
            )
        }
        AvatarAnime(profil.avatar, taille = 150.dp)
        Text(
            "Bonjour ${profil.prenom} !",
            style = MaterialTheme.typography.displaySmall,
            color = Encre,
            textAlign = TextAlign.Center,
        )
        Text(
            "${profil.avatar.nom} ${profil.avatar.engin} t'attend pour la tournée.",
            style = MaterialTheme.typography.bodyLarge,
            color = EncreDouce,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        // Après une remise à zéro faite par le parent.
        val messageEnfant = vm.messageEnfant
        if (messageEnfant != null && messageEnfant.first == profil.id) {
            Carte(Modifier.fillMaxWidth()) {
                Text(messageEnfant.second, style = MaterialTheme.typography.titleMedium, color = Encre)
                Spacer(Modifier.height(8.dp))
                BoutonSecondaire(texte = "D'accord", onClick = { vm.oublierMessageEnfant() })
            }
            Spacer(Modifier.height(16.dp))
        }

        // Français : la dictée du dernier soir envoyé.
        val soir = donnees.dernierSoir
        val seance = soir?.let { donnees.seance(it.date, Matiere.FRANCAIS) }
        val aRevoir = if (soir == null) 0 else donnees.revisions.count { r -> soir.mots.none { Mots.identiques(it, r.mot) } }
        when {
            !donnees.chargees -> TuileMatiere("Français", "La dictée de l'avion", Rouge, "Aa", etat = "Ouverture…")
            soir == null || soir.mots.isEmpty() ->
                TuileMatiere("Français", "La dictée de l'avion", Rouge, "Aa", etat = "Pas encore de mots")
            seance == null -> TuileMatiere(
                matiere = "Français",
                titre = "${soir.mots.size} mots" + if (aRevoir > 0) " + $aRevoir à revoir" else "",
                couleur = Rouge,
                symbole = "Aa",
                etat = "C'est parti !",
                onClick = { vm.commencerDictee(profil) },
            )
            !seance.terminee -> TuileMatiere(
                matiere = "Français",
                titre = "Mot ${seance.resultats.size + 1} sur ${seance.ordre.size}",
                couleur = Rouge,
                symbole = "Aa",
                etat = "Reprendre",
                onClick = { vm.commencerDictee(profil) },
            )
            else -> TuileMatiere(
                matiere = "Français",
                titre = "Fait : ${pointsEnTexte(seance.points)} / ${seance.ordre.size}",
                couleur = Rouge,
                symbole = "Aa",
                etat = "S'entraîner",
                medaille = seance.medaille,
                onClick = { vm.commencerDictee(profil) },
            )
        }
        // Maths : le tri des colis, seulement les soirs où des tables ont été choisies.
        val seanceMaths = soir?.let { donnees.seance(it.date, Matiere.MATHS) }
        val nombreDeTables = soir?.tables?.size ?: 0
        when {
            !donnees.chargees -> TuileMatiere("Maths", "Le tri des colis", Vert, "+×", etat = "Ouverture…")
            nombreDeTables == 0 -> TuileMatiere("Maths", "Le tri des colis", Vert, "+×", etat = "Pas de tables ce soir")
            seanceMaths == null -> TuileMatiere(
                matiere = "Maths",
                titre = "$nombreDeTables table${if (nombreDeTables > 1) "s" else ""}" +
                    if (donnees.revisionsMaths.isNotEmpty()) " + ${donnees.revisionsMaths.size} à revoir" else "",
                couleur = Vert,
                symbole = "+×",
                etat = "C'est parti !",
                onClick = { vm.commencerCalcul(profil) },
            )
            !seanceMaths.terminee -> TuileMatiere(
                matiere = "Maths",
                titre = "Colis ${seanceMaths.resultats.size + 1} sur ${seanceMaths.ordre.size}",
                couleur = Vert,
                symbole = "+×",
                etat = "Reprendre",
                onClick = { vm.commencerCalcul(profil) },
            )
            else -> TuileMatiere(
                matiere = "Maths",
                titre = "Fait : ${pointsEnTexte(seanceMaths.points)} / ${seanceMaths.ordre.size}",
                couleur = Vert,
                symbole = "+×",
                etat = "S'entraîner",
                medaille = seanceMaths.medaille,
                onClick = { vm.commencerCalcul(profil) },
            )
        }
        // Italien : le pigeon voyageur apporte le mot du jour (du lundi au vendredi) ; le week-end,
        // on rattrape les mots de la semaine pas encore faits et on revoit ceux qui n'étaient pas sus.
        val jour = Jours.aujourdhui()
        val seanceItalien = donnees.seance(jour, Matiere.ITALIEN)
        val lundi = Semaines.lundi(LocalDate.now()).toString()
        val motsItalien = if (donnees.chargees) vm.motsDuSoirItalien(profil) else emptyList()
        val motDuJour = vm.motDuJourItalien()
        when {
            !donnees.chargees || !vm.semaineChargee(lundi) ->
                TuileMatiere("Italien", "Le pigeon voyageur", Jaune, "Ciao", etat = "Ouverture…")
            seanceItalien == null && motsItalien.isEmpty() ->
                TuileMatiere("Italien", "Le pigeon voyageur", Jaune, "Ciao", etat = "Pas de mot aujourd'hui")
            seanceItalien == null -> TuileMatiere(
                matiere = "Italien",
                titre = when {
                    motDuJour != null && motsItalien.size > 1 ->
                        "Le mot du jour + ${motsItalien.size - 1} autre${if (motsItalien.size > 2) "s" else ""}"
                    motDuJour != null -> "Le mot du jour"
                    else -> "${motsItalien.size} mot${if (motsItalien.size > 1) "s" else ""} à rattraper"
                },
                couleur = Jaune,
                symbole = "Ciao",
                etat = "C'est parti !",
                onClick = { vm.commencerItalien(profil) },
            )
            !seanceItalien.terminee -> TuileMatiere(
                matiere = "Italien",
                titre = "Message ${seanceItalien.resultats.size + 1} sur ${seanceItalien.ordre.size}",
                couleur = Jaune,
                symbole = "Ciao",
                etat = "Reprendre",
                onClick = { vm.commencerItalien(profil) },
            )
            else -> TuileMatiere(
                matiere = "Italien",
                titre = "Fait : ${pointsEnTexte(seanceItalien.points)} / ${seanceItalien.ordre.size}",
                couleur = Jaune,
                symbole = "Ciao",
                etat = "S'entraîner",
                onClick = { vm.commencerItalien(profil) },
            )
        }

        // L'album des cartes postales, et la carte qui attend d'être épinglée.
        val aEpingler = donnees.cartesGagnees.firstOrNull { !it.epinglee }
        if (aEpingler != null) {
            Spacer(Modifier.height(8.dp))
            BoutonPrincipal(
                texte = "Une carte postale t'attend !",
                onClick = { vm.ouvrirCarte(profil, aEpingler.idCarte, nouvelle = true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(8.dp))
        BoutonSecondaire(
            texte = "Mon album d'Italie (${donnees.cartesGagnees.size} carte${if (donnees.cartesGagnees.size > 1) "s" else ""})",
            onClick = { vm.ouvrirAlbum(profil) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        BoutonSecondaire(
            texte = "Ma tournée des îles",
            onClick = { vm.ouvrirTournee(profil) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        BoutonSecondaire(
            texte = "Classement et médailles",
            onClick = { vm.ouvrirClassement(profil) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TuileMatiere(
    matiere: String,
    titre: String,
    couleur: Color,
    symbole: String,
    etat: String,
    medaille: Medaille = Medaille.AUCUNE,
    onClick: (() -> Unit)? = null,
) {
    val contenu = @Composable {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(couleur),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbole, style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(matiere, style = MaterialTheme.typography.titleLarge)
                Text(titre, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            if (medaille != Medaille.AUCUNE) {
                DessinMedaille(medaille, Modifier.size(40.dp))
                Spacer(Modifier.width(6.dp))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (onClick != null) couleur else CielBas,
            ) {
                Text(
                    etat,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (onClick != null) Color.White else EncreDouce,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            contentColor = Encre,
            shadowElevation = 3.dp,
            modifier = modifier,
        ) { contenu() }
    } else {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            contentColor = Encre,
            shadowElevation = 1.dp,
            modifier = modifier.alpha(0.85f),
        ) { contenu() }
    }
}
