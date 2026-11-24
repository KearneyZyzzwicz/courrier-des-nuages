package fr.courrierdesnuages.app.ui.ecrans

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.courrierdesnuages.app.data.Jours
import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Profil
import fr.courrierdesnuages.app.data.Seance
import fr.courrierdesnuages.app.hangar.Hangar
import fr.courrierdesnuages.app.hangar.enginDuFacteur
import fr.courrierdesnuages.app.hangar.piecesDuHangar
import fr.courrierdesnuages.app.italien.Semaines
import fr.courrierdesnuages.app.langues.Langue
import fr.courrierdesnuages.app.ui.AppViewModel
import fr.courrierdesnuages.app.ui.deLeNom
import fr.courrierdesnuages.app.ui.composants.BanniereMiseAJour
import fr.courrierdesnuages.app.ui.composants.BarreRetour
import fr.courrierdesnuages.app.ui.composants.BoutonPrincipal
import fr.courrierdesnuages.app.ui.composants.BoutonSecondaire
import fr.courrierdesnuages.app.ui.composants.Carte
import fr.courrierdesnuages.app.ui.composants.ImageAvatar
import fr.courrierdesnuages.app.ui.composants.Pastille
import fr.courrierdesnuages.app.ui.theme.CielBas
import fr.courrierdesnuages.app.ui.theme.Encre
import fr.courrierdesnuages.app.ui.theme.EncreDouce
import fr.courrierdesnuages.app.ui.theme.RougeFonce
import fr.courrierdesnuages.app.ui.theme.Vert
import java.time.LocalDate

@Composable
fun EcranEspaceParent(vm: AppViewModel) {
    var confirmerDelier by remember { mutableStateOf(false) }
    var offrirA by remember { mutableStateOf<Profil?>(null) }

    Column(Modifier.fillMaxSize()) {
        BarreRetour("Espace parent", onRetour = { vm.quitter() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BanniereMiseAJour(vm)

            val envoi = vm.messageParent
            if (envoi != null) {
                Surface(shape = RoundedCornerShape(18.dp), color = Vert, contentColor = Color.White) {
                    Text(
                        envoi,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Saisie du soir", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                if (vm.profils.isEmpty()) {
                    Text(
                        "Ajoute d'abord les enfants ci-dessous.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }
                val aujourdhui = Jours.aujourdhui()
                for (p in vm.profils) {
                    val envoye = vm.donnees(p.id).soir(aujourdhui) != null
                    LigneSoir(p, envoye) { vm.ouvrirCeSoir(p) }
                }
            }

            if (vm.profils.isNotEmpty()) {
                Carte(Modifier.fillMaxWidth()) {
                    Text("Suivi des enfants", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    for (p in vm.profils) {
                        LigneSuivi(p, resumeDuJour(vm, p)) { vm.ouvrirSuivi(p) }
                    }
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Langues", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Les mots de chaque jour, les mêmes pour les trois enfants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EncreDouce,
                )
                for (langue in Langue.entries) {
                    LigneLangue(vm, langue)
                }
            }

            if (vm.profils.isNotEmpty()) {
                Carte(Modifier.fillMaxWidth()) {
                    Text("Récompenses", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Offre une carte ou une pièce du hangar, dans la matière de ton choix : elle attendra l'enfant sur son accueil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                    for (p in vm.profils) {
                        val d = vm.donnees(p.id)
                        val engins = Hangar.construits(p.piecesDuHangar).size
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.prenom, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "France ${d.cartesFrance.size} · Italie ${d.cartesGagnees.size} · Monde ${d.cartesMonde.size} · " +
                                        "$engins engin${if (engins > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EncreDouce,
                                )
                            }
                            TextButton(onClick = { offrirA = p }) { Text("Offrir") }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${vm.toutesLesCartes.size} cartes d'Italie à gagner, dont ${vm.cartesDuParent.size} créée${if (vm.cartesDuParent.size > 1) "s" else ""} par toi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                    val bientot = vm.profils.filter { vm.donnees(it.id).chargees && vm.cartesRestantes(it.id) <= 3 }
                    if (bientot.isNotEmpty()) {
                        Text(
                            "Plus que quelques cartes d'Italie à gagner pour ${bientot.joinToString(" et ") { it.prenom }} : pense à en créer !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RougeFonce,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    BoutonSecondaire(texte = "Créer une carte d'Italie", onClick = { vm.ouvrirNouvelleCarte() }, modifier = Modifier.fillMaxWidth())
                }
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Les enfants", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (vm.profils.isEmpty()) {
                    Text(
                        "Ajoute le profil de chaque enfant : prénom, classe, avatar et code secret.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EncreDouce,
                    )
                }
                for (p in vm.profils) {
                    LigneEnfant(p) { vm.ouvrirFiche(p) }
                }
                Spacer(Modifier.height(10.dp))
                BoutonPrincipal(
                    texte = "Ajouter un enfant",
                    onClick = { vm.ouvrirFiche(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Carte(Modifier.fillMaxWidth()) {
                Text("Réglages", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Changer le code parent",
                    onClick = { vm.changerCodeParent() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Rechercher une mise à jour",
                    onClick = { vm.verifierMiseAJour(silencieux = false) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = vm.messageMaj
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
                }
                Spacer(Modifier.height(10.dp))
                BoutonSecondaire(
                    texte = "Délier ce téléphone",
                    onClick = { confirmerDelier = true },
                    couleur = RougeFonce,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val email = vm.emailCompte
            Text(
                if (email != null) "Version ${vm.nomVersion} · $email" else "Version ${vm.nomVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // Les sources des cartes, demandées par leurs licences : ici plutôt que sous les cartes des enfants.
            Text(
                "Cartes : départements IGN Admin Express (Licence ouverte Etalab), Italie ISTAT (CC BY), monde Natural Earth.",
                style = MaterialTheme.typography.bodySmall,
                color = EncreDouce,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    val enfant = offrirA
    if (enfant != null) {
        DialogueOffrir(vm, enfant, onFermer = { offrirA = null })
    }

    if (confirmerDelier) {
        AlertDialog(
            onDismissRequest = { confirmerDelier = false },
            title = { Text("Délier ce téléphone ?") },
            text = {
                Text(
                    "Les profils et les résultats restent en sécurité dans la base. " +
                        "Pour relier à nouveau ce téléphone, il faudra le mot de passe du compte de la famille.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerDelier = false
                    vm.delierTelephone()
                }) { Text("Délier", color = RougeFonce) }
            },
            dismissButton = {
                TextButton(onClick = { confirmerDelier = false }) { Text("Annuler") }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun LigneEnfant(profil: Profil, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 56.dp, engin = profil.enginDuFacteur)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profil.prenom, style = MaterialTheme.typography.titleMedium)
                Text(profil.classe, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = EncreDouce)
        }
    }
}

/** Où en sont les séances du jour : « Français ✓ · Maths à faire · Italien ✓ ». */
private fun resumeDuJour(vm: AppViewModel, p: Profil): String {
    val d = vm.donnees(p.id)
    if (!d.chargees) return "Chargement…"
    fun etat(s: Seance?): String = when {
        s == null -> "à faire"
        s.terminee -> "✓"
        else -> "commencé"
    }
    val soir = d.dernierSoir
    val parties = buildList {
        if (soir != null && soir.mots.isNotEmpty()) add("Français " + etat(d.seance(soir.date, Matiere.FRANCAIS)))
        if (soir != null && soir.tables.isNotEmpty()) add("Maths " + etat(d.seance(soir.date, Matiere.MATHS)))
        for (langue in Langue.entries) {
            val seance = d.seance(Jours.aujourdhui(), langue.matiere)
            if (seance != null || vm.motsDuSoir(langue, p).isNotEmpty()) add(langue.nom + " " + etat(seance))
        }
    }
    return if (parties.isEmpty()) "Rien à faire aujourd'hui" else parties.joinToString(" · ")
}

@Composable
private fun LigneSuivi(profil: Profil, resume: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 48.dp, engin = profil.enginDuFacteur)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profil.prenom, style = MaterialTheme.typography.titleMedium)
                Text(resume, style = MaterialTheme.typography.bodyMedium, color = EncreDouce)
            }
            Surface(shape = RoundedCornerShape(50), color = Encre, contentColor = Color.White) {
                Text(
                    "Voir",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LigneSoir(profil: Profil, envoye: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageAvatar(profil.avatar, taille = 48.dp, engin = profil.enginDuFacteur)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profil.prenom, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (envoye) "Envoyé ce soir ✓" else "À préparer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (envoye) Vert else EncreDouce,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = Encre, contentColor = Color.White) {
                Text(
                    "Ce soir",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Les récompenses que le parent peut offrir, une par matière. */
private enum class Cadeau(val titre: String) {
    FRANCE("Français : une carte de France"),
    HANGAR("Maths : une pièce pour le hangar"),
    ITALIE("Italien : une carte d'Italie"),
    MONDE("Anglais : une carte du tour du monde"),
}

/** Le parent offre une récompense : il choisit la matière, puis au hasard ou une carte précise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogueOffrir(vm: AppViewModel, profil: Profil, onFermer: () -> Unit) {
    var cadeau by remember { mutableStateOf<Cadeau?>(null) }
    val departements = vm.departementsAGagner(profil.id)
    val italie = vm.cartesAGagner(profil.id)
    val monde = vm.cartesMondeAGagner(profil.id)
    val piece = vm.prochainePiece(profil)
    val choix = cadeau
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(choix?.titre ?: "Offrir une récompense à ${profil.prenom}") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (choix) {
                    null -> {
                        Text("Elle attendra ${profil.prenom} sur son accueil.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        ChoixCadeau(Cadeau.FRANCE, "${departements.size} départements à gagner", departements.isNotEmpty()) { cadeau = it }
                        ChoixCadeau(Cadeau.HANGAR, piece?.let { "Prochaine pièce : ${it.piece}" } ?: "Le hangar est complet", piece != null) { cadeau = it }
                        ChoixCadeau(Cadeau.ITALIE, "${italie.size} carte${if (italie.size > 1) "s" else ""} à gagner", italie.isNotEmpty()) { cadeau = it }
                        ChoixCadeau(Cadeau.MONDE, "${monde.size} carte${if (monde.size > 1) "s" else ""} à gagner", monde.isNotEmpty()) { cadeau = it }
                    }
                    Cadeau.FRANCE -> {
                        Text("${profil.prenom} l'épinglera sur la carte de France.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        BoutonPrincipal(
                            texte = "Un département au hasard",
                            onClick = {
                                vm.offrirDepartement(profil, null)
                                onFermer()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Ou choisis le département :", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (d in departements) {
                                Pastille(texte = "${d.code} ${d.nom}", choisie = false, onClick = {
                                    vm.offrirDepartement(profil, d)
                                    onFermer()
                                })
                            }
                        }
                    }
                    Cadeau.HANGAR -> {
                        if (piece != null) {
                            Text(
                                "La pièce « ${piece.piece} » ${deLeNom(piece.engin.nom)} (${piece.numero} sur ${Hangar.PIECES_PAR_ENGIN})." +
                                    if (piece.termine) " C'est la dernière : l'engin sera prêt !" else "",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Elle compte comme une médaille d'or pour le hangar, pas pour le classement.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EncreDouce,
                            )
                            Spacer(Modifier.height(10.dp))
                            BoutonPrincipal(
                                texte = "Offrir cette pièce",
                                onClick = {
                                    vm.offrirPiece(profil)
                                    onFermer()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Cadeau.ITALIE -> {
                        Text("${profil.prenom} l'épinglera sur la carte d'Italie.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        BoutonPrincipal(
                            texte = "Une carte au hasard",
                            onClick = {
                                vm.offrirCarte(profil, null)
                                onFermer()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Ou choisis la ville :", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (carte in italie) {
                                Pastille(texte = carte.ville, choisie = false, onClick = {
                                    vm.offrirCarte(profil, carte)
                                    onFermer()
                                })
                            }
                        }
                    }
                    Cadeau.MONDE -> {
                        Text("${profil.prenom} l'épinglera sur la carte du monde.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        BoutonPrincipal(
                            texte = "Une carte au hasard",
                            onClick = {
                                vm.offrirCarteMonde(profil, null)
                                onFermer()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Ou choisis la ville :", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in monde) {
                                Pastille(texte = "${c.drapeau} ${c.ville}", choisie = false, onClick = {
                                    vm.offrirCarteMonde(profil, c)
                                    onFermer()
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
        dismissButton = if (choix != null) {
            { TextButton(onClick = { cadeau = null }) { Text("Retour") } }
        } else {
            null
        },
        containerColor = Color.White,
    )
}

@Composable
private fun ChoixCadeau(cadeau: Cadeau, detail: String, actif: Boolean, onChoisir: (Cadeau) -> Unit) {
    Surface(
        onClick = { onChoisir(cadeau) },
        enabled = actif,
        shape = RoundedCornerShape(16.dp),
        color = CielBas,
        contentColor = Encre,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (actif) 1f else 0.5f),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(cadeau.titre, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = EncreDouce)
        }
    }
}

/** Une langue dans l'espace parent : les mots d'aujourd'hui et de la semaine, et le bouton pour les préparer. */
@Composable
private fun LigneLangue(vm: AppViewModel, langue: Langue) {
    val aujourdhui = LocalDate.now()
    val lundi = Semaines.lundi(aujourdhui).toString()
    val semaine = vm.semaine(langue, lundi)
    val duJour = semaine?.motsDuJour(Semaines.numeroDuJour(aujourdhui)).orEmpty()
    val dansLaSemaine = semaine?.tous?.size ?: 0
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(langue.nom, style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    !vm.semaineChargee(langue, lundi) -> "Chargement…"
                    dansLaSemaine == 0 -> "Pas encore de mot cette semaine."
                    duJour.isEmpty() -> "Aucun mot aujourd'hui, $dansLaSemaine dans la semaine."
                    else -> "Aujourd'hui : " + duJour.joinToString(", ") { it.traduction } + "."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = EncreDouce,
            )
        }
        Spacer(Modifier.width(8.dp))
        BoutonSecondaire(texte = "Préparer", onClick = { vm.ouvrirMotsLangue(langue) })
    }
}
