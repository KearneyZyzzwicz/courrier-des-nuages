package fr.courrierdesnuages.app.lecture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Les photos des cartes créées par le parent sont rangées dans la base sous forme de texte (JPEG en
 * base64), réduites pour rester légères : la formule gratuite de Firebase n'a pas de stockage de fichiers.
 */
object Images {

    suspend fun compresser(contexte: Context, image: Uri, largeurMax: Int = 900, qualite: Int = 75): String? =
        withContext(Dispatchers.IO) {
            val bitmap = LecteurDeListe.apercu(contexte, image, largeurMax) ?: return@withContext null
            val modifiable = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
            val sortie = ByteArrayOutputStream()
            modifiable.compress(Bitmap.CompressFormat.JPEG, qualite, sortie)
            Base64.encodeToString(sortie.toByteArray(), Base64.NO_WRAP)
        }

    fun decoder(base64: String): Bitmap? = runCatching {
        val octets = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(octets, 0, octets.size)
    }.getOrNull()
}
