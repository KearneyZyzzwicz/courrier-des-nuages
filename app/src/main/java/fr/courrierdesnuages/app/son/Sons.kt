package fr.courrierdesnuages.app.son

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

/**
 * La voix d'Android, en français, un peu plus lente que d'habitude.
 *
 * Le moteur de synthèse charge sa voix au premier mot qu'on lui demande, et ce premier mot se perd
 * parfois. La voix est donc réveillée dès sa création par un mot dit en silence, et chaque mot est
 * redemandé une fois si la voix ne l'a pas commencé au bout de 2 secondes.
 */
class Voix(contexte: Context) : TextToSpeech.OnInitListener {

    var prete by mutableStateOf(false)
        private set
    var probleme by mutableStateOf<String?>(null)
        private set

    private val principal = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(contexte.applicationContext, this)

    /** Le mot à dire, gardé tant que la voix ne l'a pas commencé. */
    private var enAttente: String? = null
    private var idEnCours: String? = null
    private var numero = 0
    private var relances = 0

    private val verifier = Runnable {
        if (enAttente != null && !tts.isSpeaking && relances < 1) {
            relances += 1
            parler()
        }
    }

    // Appelé par Android, parfois avant la fin de la création de l'objet : on attend le fil principal.
    override fun onInit(statut: Int) {
        principal.post { demarrer(statut) }
    }

    private fun demarrer(statut: Int) {
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
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = commence(id)

            override fun onDone(id: String?) = commence(id)

            @Deprecated("Remplacée par onError(String, Int), qui l'appelle.")
            override fun onError(id: String?) {
                principal.post { if (id != null && id == idEnCours) verifier.run() }
            }
        })
        // Réveil du moteur : un mot dit en silence lui fait charger sa voix avant le premier vrai mot.
        val silence = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0f) }
        tts.speak("Bonjour", TextToSpeech.QUEUE_ADD, silence, "reveil")
        prete = true
        if (enAttente != null) parler(TextToSpeech.QUEUE_ADD)
    }

    /** La voix a commencé (ou fini) ce mot : plus besoin de le redemander. */
    private fun commence(id: String?) {
        principal.post {
            if (id != null && id == idEnCours) {
                enAttente = null
                principal.removeCallbacks(verifier)
            }
        }
    }

    private fun parler(mode: Int = TextToSpeech.QUEUE_FLUSH) {
        val texte = enAttente ?: return
        numero += 1
        val id = "mot$numero"
        idEnCours = id
        tts.speak(texte, mode, null, id)
        principal.removeCallbacks(verifier)
        principal.postDelayed(verifier, 2000L)
    }

    fun dire(texte: String) {
        enAttente = texte
        relances = 0
        if (prete) parler()
    }

    fun taire() {
        enAttente = null
        idEnCours = null
        principal.removeCallbacks(verifier)
        if (prete) tts.stop()
    }

    fun liberer() {
        principal.removeCallbacks(verifier)
        tts.shutdown()
    }
}
