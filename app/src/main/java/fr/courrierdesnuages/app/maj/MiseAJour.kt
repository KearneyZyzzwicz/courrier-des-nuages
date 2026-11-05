package fr.courrierdesnuages.app.maj

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import fr.courrierdesnuages.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class NouvelleVersion(val numero: Int, val nom: String, val adresse: String, val taille: Long)

/** Les nouvelles versions sont publiées sur GitHub ; l'appli les cherche et les installe. */
object MiseAJour {
    private const val ADRESSE_API =
        "https://api.github.com/repos/KearneyZyzzwicz/courrier-des-nuages/releases/latest"
    private const val NOM_FICHIER = "courrier-des-nuages.apk"

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

    suspend fun telecharger(
        contexte: Context,
        version: NouvelleVersion,
        progression: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dossier = File(contexte.cacheDir, "maj")
        dossier.mkdirs()
        dossier.listFiles()?.forEach { it.delete() }
        val fichier = File(dossier, NOM_FICHIER)
        // GitHub renvoie vers un autre serveur (toujours en https) : la redirection est suivie.
        val connexion = URL(version.adresse).openConnection() as HttpURLConnection
        try {
            connexion.instanceFollowRedirects = true
            connexion.connectTimeout = 15_000
            connexion.readTimeout = 30_000
            connexion.setRequestProperty("User-Agent", "CourrierDesNuages")
            val code = connexion.responseCode
            if (code != 200) throw IOException("Téléchargement impossible ($code)")
            val total = connexion.contentLengthLong.takeIf { it > 0 } ?: version.taille
            connexion.inputStream.use { entree ->
                fichier.outputStream().use { sortie ->
                    val tampon = ByteArray(64 * 1024)
                    var lu = 0L
                    while (true) {
                        val n = entree.read(tampon)
                        if (n < 0) break
                        sortie.write(tampon, 0, n)
                        lu += n
                        if (total > 0) progression((lu.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            connexion.disconnect()
        }
        fichier
    }

    fun peutInstaller(contexte: Context): Boolean =
        contexte.packageManager.canRequestPackageInstalls()

    /** Ouvre le réglage Android « Installer des applis inconnues » pour cette appli. */
    fun demanderAutorisation(contexte: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${contexte.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contexte.startActivity(intent)
    }

    fun installer(contexte: Context, fichier: File) {
        val uri = FileProvider.getUriForFile(contexte, "${contexte.packageName}.fichiers", fichier)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        contexte.startActivity(intent)
    }
}
