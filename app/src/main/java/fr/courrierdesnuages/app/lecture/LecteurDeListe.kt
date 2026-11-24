package fr.courrierdesnuages.app.lecture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import fr.courrierdesnuages.app.data.Mots
import fr.courrierdesnuages.app.langues.ListeDeMots
import fr.courrierdesnuages.app.langues.Morceau
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** Lit les mots d'une photo de la liste, directement sur le téléphone (sans Internet). */
object LecteurDeListe {

    /** Fichier où l'appareil photo enregistre la photo de la liste. */
    fun nouvellePhoto(contexte: Context): Uri {
        val dossier = File(contexte.cacheDir, "photos").apply { mkdirs() }
        val fichier = File(dossier, "liste-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(contexte, "${contexte.packageName}.fichiers", fichier)
    }

    suspend fun lire(contexte: Context, image: Uri): List<String> {
        val entree = withContext(Dispatchers.IO) { InputImage.fromFilePath(contexte, image) }
        val lecteur = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val texte = lecteur.process(entree).await()
            val lignes = texte.textBlocks.flatMap { bloc -> bloc.lines.map { it.text } }
            return Mots.extraire(lignes)
        } finally {
            lecteur.close()
        }
    }

    /**
     * Les lignes d'une liste de langue, dans l'ordre de la feuille : les morceaux à la même hauteur
     * sont remis sur la même ligne (deux colonnes séparées par une tabulation).
     */
    suspend fun lireLignes(contexte: Context, image: Uri): List<String> {
        val entree = withContext(Dispatchers.IO) { InputImage.fromFilePath(contexte, image) }
        val lecteur = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val texte = lecteur.process(entree).await()
            val morceaux = texte.textBlocks.flatMap { bloc -> bloc.lines }.mapNotNull { ligne ->
                val boite = ligne.boundingBox ?: return@mapNotNull null
                Morceau(ligne.text, boite.left, boite.top, boite.right, boite.bottom)
            }
            return ListeDeMots.rangees(morceaux)
        } finally {
            lecteur.close()
        }
    }

    /** Le texte d'un fichier (.txt) : en UTF-8, ou en Windows-1252 s'il vient d'un vieux logiciel. */
    suspend fun lireTexte(contexte: Context, fichier: Uri): String = withContext(Dispatchers.IO) {
        val octets = contexte.contentResolver.openInputStream(fichier)?.use { it.readBytes() } ?: ByteArray(0)
        val utf8 = String(octets, Charsets.UTF_8)
        (if ('\uFFFD' in utf8) String(octets, charset("windows-1252")) else utf8).removePrefix("\uFEFF")
    }

    /** L'image réduite, dans le bon sens, pour l'afficher au-dessus des mots à corriger. */
    suspend fun apercu(contexte: Context, image: Uri, largeurMax: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(contexte.contentResolver, image)
                ImageDecoder.decodeBitmap(source) { decodeur, info, _ ->
                    val l = info.size.width
                    if (l > largeurMax) {
                        val h = info.size.height * largeurMax / l
                        decodeur.setTargetSize(largeurMax, h)
                    }
                }
            } else {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                contexte.contentResolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        }.getOrNull()
    }
}
