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
