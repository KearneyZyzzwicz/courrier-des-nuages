package fr.courrierdesnuages.app.data

import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

object Mots {
    private val espaces = Regex("\\s+")
    private val puceAuDebut = Regex("^[-*>•·–—0-9.)\\s]+")

    /**
     * Met un mot sous une forme comparable : espaces en trop retirés, apostrophes et tirets
     * typographiques remplacés par ceux du clavier. Les accents et les majuscules restent.
     */
    fun normaliser(texte: String): String = texte
        .replace('’', '\'')
        .replace('‘', '\'')
        .replace('ʼ', '\'')
        .replace('`', '\'')
        .replace('‐', '-')
        .replace('‑', '-')
        .replace(' ', ' ')
        .replace(' ', ' ')
        .trim()
        .replace(espaces, " ")

    /** La règle de la dictée : identique au mot enregistré, accents, majuscules et déterminant compris. */
    fun identiques(saisie: String, attendu: String): Boolean = normaliser(saisie) == normaliser(attendu)

    /** Identifiant de document pour un mot (la base n'accepte pas tous les caractères). */
    fun cle(mot: String): String {
        val octets = MessageDigest.getInstance("SHA-1").digest(normaliser(mot).toByteArray(Charsets.UTF_8))
        return octets.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /** Les lettres du mot (sans les espaces), mélangées, et dans un autre ordre que le mot quand c'est possible. */
    fun melanger(mot: String, hasard: Random = Random.Default): List<Char> {
        val lettres = normaliser(mot).filter { it != ' ' }.toList()
        if (lettres.toSet().size < 2) return lettres
        var essai = lettres.shuffled(hasard)
        var tours = 0
        while (essai == lettres && tours < 20) {
            essai = lettres.shuffled(hasard)
            tours++
        }
        return essai
    }

    /** Sans doublons (en comparant les formes normalisées), dans l'ordre d'arrivée. */
    fun sansDoublons(mots: List<String>): List<String> {
        val vus = HashSet<String>()
        return mots.map { normaliser(it) }.filter { it.isNotEmpty() && vus.add(it) }
    }

    /**
     * Transforme le texte lu sur la photo en mots : une ligne par mot, ou des mots séparés par des
     * virgules. Les puces et les numéros sont retirés ; le parent corrige le reste.
     */
    fun extraire(lignes: List<String>): List<String> {
        val resultat = ArrayList<String>()
        for (ligne in lignes) {
            for (morceau in ligne.split(',', ';', '•', '·', '/', '|')) {
                val mot = normaliser(morceau)
                    .replace(puceAuDebut, "")
                    .trim(' ', '.', ',', ';', ':', '!', '?', '"', '«', '»', '(', ')', '[', ']')
                if (mot.length < 2 || mot.none { it.isLetter() }) continue
                if (mot.count { it == ' ' } > 3) continue // une phrase ou un titre
                resultat += mot
            }
        }
        return sansDoublons(resultat)
    }
}

/** Les jours sont enregistrés au format 2026-11-05. */
object Jours {
    private val joli = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    fun aujourdhui(): String = LocalDate.now().toString()

    fun enClair(jour: String): String = runCatching { LocalDate.parse(jour).format(joli) }.getOrDefault(jour)
}
