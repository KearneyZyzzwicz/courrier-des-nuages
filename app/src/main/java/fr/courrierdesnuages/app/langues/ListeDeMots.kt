package fr.courrierdesnuages.app.langues

import fr.courrierdesnuages.app.data.Mots
import java.util.Locale

/** Un mot lu dans une liste importée (photo, image ou texte), avant la vérification du parent. */
data class PaireLue(val francais: String, val traduction: String)

/** Un morceau de texte lu sur l'image, avec sa place en pixels. */
data class Morceau(val texte: String, val gauche: Int, val haut: Int, val droite: Int, val bas: Int) {
    val hauteur: Int get() = (bas - haut).coerceAtLeast(1)
}

/** Les mots répartis sur les jours de la semaine, et ceux qui n'y tiennent pas. */
data class Repartition(val jours: Map<Int, List<MotLangue>>, val restants: List<MotLangue>)

/**
 * Lecture d'une liste de mots de langue : une ligne par mot, le mot français et sa traduction
 * séparés par un tiret, un signe égal, deux-points, une flèche ou deux colonnes
 * (« 1. Hello — Bonjour / Salut », « le chat = il gatto »). L'ordre des deux côtés est deviné.
 */
object ListeDeMots {

    /** Sépare deux colonnes d'une même ligne de l'image. */
    const val COLONNE = "\t"

    /** Au plus 40 lettres par mot, comme à la main. */
    private const val LONGUEUR_MAX = 40

    /**
     * Remet les morceaux lus dans l'ordre des lignes de la feuille : les morceaux à la même hauteur
     * forment une ligne, de gauche à droite, séparés par une tabulation (deux colonnes).
     */
    fun rangees(morceaux: List<Morceau>): List<String> {
        val lignes = ArrayList<MutableList<Morceau>>()
        for (m in morceaux.filter { it.texte.isNotBlank() }.sortedBy { it.haut + it.bas }) {
            val ligne = lignes.lastOrNull()
            if (ligne != null && memeLigne(ligne, m)) ligne += m else lignes += mutableListOf(m)
        }
        return lignes.map { l -> l.sortedBy { it.gauche }.joinToString(COLONNE) { it.texte.trim() } }
    }

    /** Même ligne : les deux se recouvrent sur au moins la moitié de la hauteur du plus petit. */
    private fun memeLigne(ligne: List<Morceau>, m: Morceau): Boolean {
        val haut = ligne.sumOf { it.haut } / ligne.size
        val bas = ligne.sumOf { it.bas } / ligne.size
        val recouvrement = minOf(bas, m.bas) - maxOf(haut, m.haut)
        return recouvrement >= 0.5 * minOf(bas - haut, m.hauteur)
    }

    private val numero = Regex("^\\s*\\d{1,2}\\s*[.)°/-]\\s*")
    private val puce = Regex("^\\s*[-*•·▪►✓☐→>]+\\s*")
    private val date = Regex("\\b\\d{1,2}/\\d{1,2}(/\\d{2,4})?\\b")
    private val titre = Regex("^(semaine|week|th[eè]me|theme|vocabulaire|vocabulary)\\b", RegexOption.IGNORE_CASE)

    /** Les séparateurs possibles, du plus sûr au moins sûr. */
    private val separateurs = listOf(
        Regex("\\t+"),
        Regex("\\s*[—–]\\s*"),
        Regex("\\s*(=>|->|→|⇒|=)\\s*"),
        Regex("\\s+-\\s+"),
        Regex("\\s*:\\s+"),
        Regex("\\s*\\|\\s*"),
        Regex("\\s{3,}"),
    )

    /** Coupe une ligne en deux côtés, ou null s'il n'y a pas de séparateur. */
    fun separer(ligne: String): Pair<String, String>? {
        for (sep in separateurs) {
            for (m in sep.findAll(ligne)) {
                val a = ligne.substring(0, m.range.first)
                val b = ligne.substring(m.range.last + 1)
                if (a.any { it.isLetter() } && b.any { it.isLetter() }) return a to b
            }
        }
        return null
    }

    /** Un côté propre : sans puce ni numéro, sans guillemets ni virgule au bout ; « ... » devient « … ». */
    fun nettoyer(cote: String): String {
        var t = Mots.normaliser(cote.replace("...", "…").replace('\t', ' '))
        t = t.replace(numero, "").replace(puce, "")
        t = t.trim(' ', ',', ';', ':', '.', '"', '«', '»', '“', '”', '*', '_')
        // Un point final ne sert à rien, mais « … », « ? » et « ! » font partie de la phrase.
        return Mots.normaliser(t).take(LONGUEUR_MAX).trim()
    }

    /** Une ligne à ignorer : un titre, une date, une consigne. */
    private fun ignoree(ligne: String): Boolean {
        val t = ligne.replace(numero, "").replace(puce, "").trim()
        return t.isEmpty() || titre.containsMatchIn(t) || date.containsMatchIn(t)
    }

    /**
     * Les paires de la liste, dans l'ordre : français d'abord, traduction ensuite (l'ordre de la
     * feuille est deviné ; le parent peut toujours inverser). Les titres et les dates sont ignorés ;
     * si plusieurs lignes sont numérotées, seules celles-là comptent.
     */
    fun lire(lignes: List<String>, langue: Langue): List<PaireLue> {
        val utiles = lignes.map { it.trim() }.filter { it.isNotEmpty() }
        val numerotees = utiles.filter { numero.containsMatchIn(it) && separer(it) != null }
        val candidates = if (numerotees.size >= 2) numerotees else utiles
        val brutes = candidates.filterNot { ignoree(it) }.mapNotNull { ligne ->
            val (a, b) = separer(ligne) ?: return@mapNotNull null
            val pa = nettoyer(a)
            val pb = nettoyer(b)
            if (pa.isEmpty() || pb.isEmpty()) null else pa to pb
        }
        // L'ordre de la feuille, deviné sur toute la liste ; une ligne qui dit nettement le contraire garde le sien.
        val ecarts = brutes.map { (a, b) -> scoreFrancais(b, langue) - scoreFrancais(a, langue) }
        val inverserTout = ecarts.sum() > 0
        val vus = HashSet<String>()
        return brutes.zip(ecarts)
            .map { (paire, ecart) ->
                val (a, b) = paire
                val inverser = when {
                    ecart >= 3 -> true
                    ecart <= -3 -> false
                    else -> inverserTout
                }
                if (inverser) PaireLue(b, a) else PaireLue(a, b)
            }
            .filter { vus.add(MotLangue(it.francais, it.traduction).code.lowercase(Locale.FRENCH)) }
    }

    private val motsFrancais = listOf(
        "le", "les", "un", "une", "des", "du", "au", "aux", "je", "tu", "nous", "vous", "elle", "mon", "ma",
        "mes", "ton", "ta", "tes", "est", "et", "suis", "es", "bonjour", "bonsoir", "merci", "salut", "oui",
        "non", "pardon", "s'il", "plaît", "comment", "ça", "va", "bien", "c'est", "qu'est-ce", "j'ai", "m'appelle",
        "au revoir", "bonne", "nuit", "matin", "soir",
    )
    private val motsAnglais = listOf(
        "the", "a", "an", "my", "your", "is", "are", "am", "you", "i", "i'm", "it's", "what", "how", "good",
        "please", "thank", "thanks", "hello", "hi", "yes", "no", "and", "goodbye", "bye", "morning", "night",
        "name", "sorry", "welcome", "of", "to", "do", "does", "this", "that", "where", "who", "old",
    )
    private val motsItaliens = listOf(
        "il", "lo", "gli", "uno", "una", "sono", "è", "ciao", "grazie", "prego", "buongiorno", "buonasera",
        "per", "favore", "come", "stai", "bene", "mi", "chiamo", "sì", "di", "che", "della", "del", "io", "ho",
    )

    /** Plus le nombre est grand, plus [texte] a l'air français (et moins il a l'air de [langue]). */
    fun scoreFrancais(texte: String, langue: Langue): Int {
        val t = texte.lowercase(Locale.FRENCH).replace('’', '\'')
        val mots = t.split(Regex("[^\\p{L}']+")).filter { it.isNotEmpty() }
        var score = 0
        score += 2 * mots.count { it in motsFrancais }
        score += 2 * t.count { it in "êëâçîïôûœ" }
        if (Regex("\\b[jlmtsdnc]'").containsMatchIn(t)) score += 1
        when (langue) {
            Langue.ANGLAIS -> {
                score -= 2 * mots.count { it in motsAnglais }
                score += t.count { it in "éèàù" }
                if ("th" in t || "w" in t) score -= 1
            }
            Langue.ITALIEN -> {
                score -= 2 * mots.count { it in motsItaliens }
                if ("zione" in t || "gli" in t || "zz" in t || "cch" in t) score -= 1
                score -= t.count { it in "ìò" }
            }
        }
        return score
    }

    /**
     * Répartit [mots] sur la semaine à partir du jour [depuis] (1 = lundi), [parJour] par jour au
     * plus, sans dépasser 10 mots un même jour avec ceux [dejaPrevus]. Ce qui ne tient pas avant
     * dimanche reste dans [Repartition.restants].
     */
    fun repartir(mots: List<MotLangue>, depuis: Int, parJour: Int, dejaPrevus: Map<Int, Int> = emptyMap()): Repartition {
        val jours = LinkedHashMap<Int, MutableList<MotLangue>>()
        val file = ArrayDeque(mots)
        var jour = depuis.coerceIn(1, 7)
        while (file.isNotEmpty() && jour <= 7) {
            val place = minOf(parJour.coerceAtLeast(1), MOTS_PAR_JOUR - (dejaPrevus[jour] ?: 0))
            repeat(place.coerceAtLeast(0)) {
                if (file.isNotEmpty()) jours.getOrPut(jour) { mutableListOf() } += file.removeFirst()
            }
            jour++
        }
        return Repartition(jours, file.toList())
    }

    /** Le nombre de mots par jour proposé : un par jour s'il y a assez de jours, sinon de quoi tout caser. */
    fun parJourConseille(nombre: Int, depuis: Int): Int {
        val jours = 8 - depuis.coerceIn(1, 7)
        return ((nombre + jours - 1) / jours).coerceIn(1, MOTS_PAR_JOUR)
    }
}
