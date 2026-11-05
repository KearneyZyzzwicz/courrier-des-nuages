package fr.courrierdesnuages.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.courrierdesnuages.app.calcul.EcranTriDesColis
import fr.courrierdesnuages.app.dictee.EcranDictee
import fr.courrierdesnuages.app.italien.EcranAlbum
import fr.courrierdesnuages.app.italien.EcranCartePostale
import fr.courrierdesnuages.app.italien.EcranEpingler
import fr.courrierdesnuages.app.italien.EcranPigeon
import fr.courrierdesnuages.app.ui.composants.FondCiel
import fr.courrierdesnuages.app.ui.ecrans.EcranAccueilEnfant
import fr.courrierdesnuages.app.ui.ecrans.EcranChoixProfil
import fr.courrierdesnuages.app.ui.ecrans.EcranCodeEnfant
import fr.courrierdesnuages.app.ui.ecrans.EcranCodeParent
import fr.courrierdesnuages.app.ui.ecrans.EcranEspaceParent
import fr.courrierdesnuages.app.ui.ecrans.EcranFicheEnfant
import fr.courrierdesnuages.app.ui.ecrans.EcranNouveauCodeParent
import fr.courrierdesnuages.app.ui.ecrans.EcranRelier
import fr.courrierdesnuages.app.ui.parent.EcranCeSoir
import fr.courrierdesnuages.app.ui.parent.EcranMotsItalien
import fr.courrierdesnuages.app.ui.parent.EcranNouvelleCarte
import fr.courrierdesnuages.app.ui.parent.EcranLectureListe

@Composable
fun Racine(vm: AppViewModel = viewModel()) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.passeEnArrierePlan() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.revientAuPremierPlan() }

    val ecran = vm.ecran
    BackHandler(enabled = ecran != Ecran.Relier && ecran != Ecran.ChoixProfil) { vm.retour() }

    FondCiel {
        AnimatedContent(
            targetState = ecran,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "ecrans",
        ) { e ->
            AfficherEcran(vm, e)
        }
    }
}

@Composable
private fun AfficherEcran(vm: AppViewModel, e: Ecran) {
    when (e) {
        Ecran.Relier -> EcranRelier(vm)
        Ecran.ChoixProfil -> EcranChoixProfil(vm)
        is Ecran.CodeEnfant -> {
            val p = vm.profil(e.idProfil)
            if (p != null) EcranCodeEnfant(vm, p)
        }
        Ecran.CodeDuParent -> EcranCodeParent(vm)
        is Ecran.NouveauCodeParent -> EcranNouveauCodeParent(vm, e.premiereFois)
        Ecran.EspaceParent -> EcranEspaceParent(vm)
        is Ecran.FicheEnfant -> {
            val p = vm.profil(e.idProfil)
            if (e.idProfil == null || p != null) EcranFicheEnfant(vm, p)
        }
        is Ecran.AccueilEnfant -> {
            val p = vm.profil(e.idProfil)
            if (p != null) EcranAccueilEnfant(vm, p)
        }
        is Ecran.CeSoir -> {
            val p = vm.profil(e.idProfil)
            if (p != null) EcranCeSoir(vm, p)
        }
        is Ecran.LectureListe -> {
            val p = vm.profil(e.idProfil)
            if (p != null) EcranLectureListe(vm, p, e.image)
        }
        is Ecran.Dictee -> {
            val s = vm.seanceDictee
            if (s != null) EcranDictee(seance = s, sons = vm.sons, voix = vm.voix, onQuitter = { vm.quitterDictee() })
        }
        is Ecran.TriDesColis -> {
            val s = vm.seanceCalcul
            if (s != null) EcranTriDesColis(seance = s, sons = vm.sons, onQuitter = { vm.quitterCalcul() })
        }
        is Ecran.Pigeon -> {
            val s = vm.seanceItalien
            if (s != null) {
                EcranPigeon(
                    seance = s,
                    sons = vm.sons,
                    onQuitter = { vm.quitterItalien() },
                    onOuvrirCarte = { vm.ouvrirCarteGagnee() },
                )
            }
        }
        is Ecran.CartePostaleVue -> {
            val p = vm.profil(e.idProfil)
            val carte = vm.carte(e.idCarte)
            if (p != null && carte != null) EcranCartePostale(vm, p, carte, e.nouvelle)
        }
        is Ecran.Epingler -> {
            val p = vm.profil(e.idProfil)
            val carte = vm.carte(e.idCarte)
            if (p != null && carte != null) EcranEpingler(vm, p, carte)
        }
        is Ecran.Album -> {
            val p = vm.profil(e.idProfil)
            if (p != null) EcranAlbum(vm, p)
        }
        Ecran.MotsItalien -> EcranMotsItalien(vm)
        Ecran.NouvelleCarte -> EcranNouvelleCarte(vm)
    }
}
