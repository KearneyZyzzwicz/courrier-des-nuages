package fr.courrierdesnuages.app.son

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.courrierdesnuages.app.R
import java.util.Locale

/** Les bruitages : le moteur de l'avion et le coup de tampon. Un bouton permet de couper le son. */
class Sons(contexte: Context) {
    private val reglages = contexte.getSharedPreferences("reglages", Context.MODE_PRIVATE)

    var coupe by mutableStateOf(reglages.getBoolean("sonCoupe", false))
        private set

    private val charges = HashSet<Int>()
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .apply { setOnLoadCompleteListener { _, id, statut -> if (statut == 0) charges += id } }

    private val moteur = pool.load(contexte, R.raw.moteur, 1)
    private val tampon = pool.load(contexte, R.raw.tampon, 1)
    private var fluxMoteur = 0

    fun basculer() {
        coupe = !coupe
        reglages.edit().putBoolean("sonCoupe", coupe).apply()
        if (coupe) arreterMoteur()
    }

    fun demarrerMoteur() {
        arreterMoteur()
        if (coupe || moteur !in charges) return
        fluxMoteur = pool.play(moteur, 0.35f, 0.35f, 1, -1, 1f)
    }

    fun arreterMoteur() {
        if (fluxMoteur != 0) pool.stop(fluxMoteur)
        fluxMoteur = 0
    }

    fun coupDeTampon() {
        if (coupe || tampon !in charges) return
        pool.play(tampon, 0.9f, 0.9f, 2, 0, 1f)
    }

    fun liberer() {
        pool.release()
    }
}

/** La voix d'Android, en français, un peu plus lente que d'habitude. */
class Voix(contexte: Context) : TextToSpeech.OnInitListener {

    var prete by mutableStateOf(false)
        private set
    var probleme by mutableStateOf<String?>(null)
        private set

    private val tts = TextToSpeech(contexte.applicationContext, this)
    private var enAttente: String? = null

    override fun onInit(statut: Int) {
        if (statut != TextToSpeech.SUCCESS) {
            probleme = "La voix d'Android ne démarre pas sur ce téléphone."
            return
        }
        val langue = tts.setLanguage(Locale.FRANCE)
        if (langue == TextToSpeech.LANG_MISSING_DATA || langue == TextToSpeech.LANG_NOT_SUPPORTED) {
            probleme = "La voix française n'est pas installée sur ce téléphone."
            return
        }
        tts.setSpeechRate(0.8f)
        prete = true
        enAttente?.let { dire(it) }
        enAttente = null
    }

    fun dire(texte: String) {
        if (!prete) {
            enAttente = texte
            return
        }
        tts.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "mot")
    }

    fun taire() {
        enAttente = null
        if (prete) tts.stop()
    }

    fun liberer() {
        tts.shutdown()
    }
}
