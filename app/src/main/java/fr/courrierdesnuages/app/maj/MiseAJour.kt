package fr.courrierdesnuages.app.maj

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.courrierdesnuages.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class NouvelleVersion(val numero: Int, val nom: String, val adresse: String, val taille: Long)

/**
 * Les nouvelles versions sont publiées sur GitHub. L'appli les cherche, puis ouvre leur
 * téléchargement dans le navigateur : c'est Android qui installe le fichier téléchargé.
 * L'appli n'installe rien elle-même (depuis la 1.39) : Google Play Protect se méfie des applis
 * qui ont l'autorisation d'installer d'autres applis.
 */
object MiseAJour {
    private const val ADRESSE_API =
        "https://api.github.com/repos/KearneyZyzzwicz/courrier-des-nuages/releases/latest"
    private const val NOM_FICHIER = "courrier-des-nuages.apk"

    /** La dernière version publiée, toujours à cette adresse. */
    private const val ADRESSE_DERNIERE =
        "https://github.com/KearneyZyzzwicz/courrier-des-nuages/releases/latest/download/$NOM_FICHIER"

    /** Renvoie la nouvelle version si elle est plus récente que celle installée, sinon null. */
    suspend fun chercher(): NouvelleVersion? = withContext(Dispatchers.IO) {
        val connexion = URL(ADRESSE_API).openConnection() as HttpURLConnection
        try {
            connexion.connectTimeout = 10_000
            connexion.readTimeout = 10_000
            connexion.setRequestProperty("Accept", "application/vnd.github+json")
            connexion.setRequestProperty("User-Agent", "CourrierDesNuages")
            val code = connexion.responseCode
            if (code == 404) return@withContext null // aucune version publiée pour l'instant
            if (code != 200) throw IOException("Réponse $code")
            val texte = connexion.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(texte)
            val etiquette = json.optString("tag_name") // par exemple « v1.12 »
            val numero = etiquette.substringAfterLast('.').toIntOrNull() ?: return@withContext null
            if (numero <= BuildConfig.VERSION_CODE) return@withContext null
            val fichiers = json.optJSONArray("assets") ?: return@withContext null
            var trouvee: NouvelleVersion? = null
            for (i in 0 until fichiers.length()) {
                val f = fichiers.getJSONObject(i)
                if (f.optString("name") == NOM_FICHIER) {
                    trouvee = NouvelleVersion(
                        numero = numero,
                        nom = etiquette.removePrefix("v"),
                        adresse = f.optString("browser_download_url"),
                        taille = f.optLong("size"),
                    )
                }
            }
            trouvee
        } finally {
            connexion.disconnect()
        }
    }

    /** Ouvre le téléchargement dans le navigateur ; faux s'il n'y a aucun navigateur. */
    fun ouvrirTelechargement(contexte: Context, version: NouvelleVersion?): Boolean {
        val adresse = version?.adresse?.takeIf { it.startsWith("https://github.com/") } ?: ADRESSE_DERNIERE
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adresse)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            contexte.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Efface l'ancien fichier téléchargé par les versions d'avant la 1.39 (environ 35 Mo). */
    fun nettoyer(contexte: Context) {
        runCatching { File(contexte.cacheDir, "maj").deleteRecursively() }
    }
}
