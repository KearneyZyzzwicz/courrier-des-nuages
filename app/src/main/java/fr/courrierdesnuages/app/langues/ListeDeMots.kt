package fr.courrierdesnuages.app.langues

import fr.courrierdesnuages.app.data.Mots
import java.util.Locale

/** Un mot lu dans une liste importée (photo, image ou texte), avant la vérification du parent. */
data class PaireLue(val francais: String, val traduction: String)

/** Un morceau de texte lu sur l'image, avec sa place en pixels. */
data class Morceau(val texte: String, val gauche: Int, val haut: Int, val droite: Int, val bas: Int) {
    val hauteur: Int get() = (bas - haut).coerceAtLeast(1)
}

/** Une ligne de la feuille, avec sa hauteur sur l'image (ou sa place dans le texte). */
data class Rangee(val texte: String, val haut: Int, val bas: Int)

/** Les mots répartis sur les jours de la semaine, et ceux qui n'y tiennent pas. */
data class Repartition(val jours: Map<Int, List<MotLangue>>, val restants: List<MotLangue>)

/**
 * Lecture d'une liste de mots de langue. Soit une ligne par mot, le mot français et sa traduction
 * séparés par un tiret, un signe égal, deux-points, une flèche ou deux colonnes
 * (« 1. Hello — Bonjour / Salut », « le chat = il gatto ») ; soit le mot sur une ligne et sa
 * traduction juste en dessous, comme sur l'image d'anglais du samedi (les mots sont alors repérés
 * aux blancs entre eux). L'ordre des deux côtés est deviné.
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
    fun rangees(morceaux: List<Morceau>): List<Rangee> {
        val lignes = ArrayList<MutableList<Morceau>>()
        // Un numéro seul (dans une pastille, en marge) ne fait pas partie du mot : il fausserait les lignes.
        val utiles = morceaux.filter { it.texte.isNotBlank() && !numeroSeul.matches(it.texte) }
        for (m in utiles.sortedBy { it.haut + it.bas }) {
            val ligne = lignes.lastOrNull()
            if (ligne != null && memeLigne(ligne, m)) ligne += m else lignes += mutableListOf(m)
        }
        return lignes.map { l ->
            Rangee(l.sortedBy { it.gauche }.joinToString(COLONNE) { it.texte.trim() }, l.minOf { it.haut }, l.maxOf { it.bas })
        }
    }

    /** Les lignes d'un texte (.txt ou collé) : une ligne vide écarte deux mots, comme un blanc sur une feuille. */
    fun rangeesDuTexte(texte: String): List<Rangee> {
        val sortie = ArrayList<Rangee>()
        var y = 0
        for (ligne in texte.lines()) {
            if (ligne.isNotBlank()) sortie += Rangee(ligne, y, y + 8)
            y += 10
        }
        return sortie
    }

    /** Même ligne : les deux se recouvrent sur au moins la moitié de la hauteur du plus petit. */
    private fun memeLigne(ligne: List<Morceau>, m: Morceau): Boolean {
        val haut = ligne.sumOf { it.haut } / ligne.size
        val bas = ligne.sumOf { it.bas } / ligne.size
        val recouvrement = minOf(bas, m.bas) - maxOf(haut, m.haut)
        return recouvrement >= 0.5 * minOf(bas - haut, m.hauteur)
    }

    private val numero = Regex("^\\s*\\d{1,2}\\s*[.)°/-]\\s*")
    private val numeroSeul = Regex("^\\s*\\d{1,2}\\s*[.)]?\\s*$")
    private val numeroEnColonne = Regex("^\\s*\\d{1,2}\\s*[.)]?\\t+")
    private val enTetes = setOf(
        "anglais", "english", "français", "francais", "french", "italien", "italian", "italiano",
        "traduction", "translation", "mot", "mots", "word", "words",
    )
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
        var t = Mots.normaliser(cote.replace(numeroEnColonne, "").replace("...", "…").replace('\t', ' '))
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

    /** Les paires d'une liste de lignes (un texte collé, un fichier .txt). */
    fun lireLignes(lignes: List<String>, langue: Langue): List<PaireLue> = lire(rangeesDuTexte(lignes.joinToString("\n")), langue)

    /** Les paires d'un texte (un fichier .txt, un texte collé). */
    fun lireTexte(texte: String, langue: Langue): List<PaireLue> = lire(rangeesDuTexte(texte), langue)

    /**
     * Les paires de la liste, dans l'ordre : français d'abord, traduction ensuite (l'ordre de la
     * feuille est deviné ; le parent peut toujours inverser). Les titres, les dates, les numéros seuls
     * et les en-têtes de colonnes (« Anglais | Français ») sont ignorés ; si plusieurs lignes à
     * séparateur sont numérotées, seules celles-là comptent.
     */
    fun lire(rangees: List<Rangee>, langue: Langue): List<PaireLue> {
        val gardees = rangees
            .map { it.copy(texte = it.texte.trim()) }
            .filter { it.texte.isNotEmpty() && !ignoree(it.texte) && !numeroSeul.matches(it.texte) }
        val avecSeparateur = gardees.filter { separer(it.texte) != null }
        val decoupees = if (avecSeparateur.isNotEmpty() && avecSeparateur.size * 2 >= gardees.size) {
            val numerotees = avecSeparateur.filter { numero.containsMatchIn(it.texte) }
            (if (numerotees.size >= 2) numerotees else avecSeparateur).mapNotNull { separer(it.texte) }
        } else {
            empilees(gardees)
        }
        val brutes = decoupees
            .map { (a, b) -> nettoyer(a) to nettoyer(b) }
            .filter { (a, b) -> a.isNotEmpty() && b.isNotEmpty() && !(a.lowercase(Locale.FRENCH) in enTetes && b.lowercase(Locale.FRENCH) in enTetes) }
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

    /**
     * Sans séparateur : le mot sur une ligne, sa traduction en dessous. Les mots sont repérés aux
     * blancs : un écart nettement plus grand que les autres sépare deux mots. Sans blancs nets, les
     * lignes vont deux par deux.
     */
    private fun empilees(lignes: List<Rangee>): List<Pair<String, String>> {
        if (lignes.size < 2) return emptyList()
        val ecarts = lignes.zipWithNext { a, b -> b.haut - a.bas }
        val limite = seuilDesBlancs(ecarts)
        val groupes = ArrayList<MutableList<Rangee>>()
        if (limite != null) {
            groupes += mutableListOf(lignes.first())
            for (i in 1 until lignes.size) {
                if (ecarts[i - 1] > limite) groupes += mutableListOf(lignes[i]) else groupes.last() += lignes[i]
            }
        } else {
            for (deux in lignes.chunked(2)) groupes += deux.toMutableList()
        }
        return groupes.filter { it.size >= 2 }.map { g -> g.first().texte to g.drop(1).joinToString(" ") { it.texte } }
    }

    /**
     * Partage les écarts entre les lignes en deux groupes, les petits et les grands, le plus nettement
     * possible (le moins de dispersion dans chaque groupe). Renvoie la limite si les grands écarts
     * sont au moins 1,8 fois plus grands que les petits, sinon null (pas de blancs nets).
     */
    fun seuilDesBlancs(ecarts: List<Int>): Int? {
        if (ecarts.size < 2) return null
        val tries = ecarts.map { it.toDouble() }.sorted()
        fun dispersion(l: List<Double>): Double {
            val m = l.average()
            return l.sumOf { (it - m) * (it - m) }
        }
        var meilleure: Int? = null
        var plusPetite = Double.MAX_VALUE
        for (k in 1 until tries.size) {
            val d = dispersion(tries.subList(0, k)) + dispersion(tries.subList(k, tries.size))
            if (d < plusPetite) {
                plusPetite = d
                meilleure = k
            }
        }
        val k = meilleure ?: return null
        val petits = tries.subList(0, k).average()
        val grands = tries.subList(k, tries.size).average()
        return if (grands >= 1.8 * maxOf(petits, 1.0)) ((tries[k - 1] + tries[k]) / 2).toInt() else null
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
