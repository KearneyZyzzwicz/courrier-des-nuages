package fr.courrierdesnuages.app.suivi

/**
 * La tournée des îles : chaque mot écrit juste est une lettre livrée, chaque calcul juste un colis
 * trié. Les îles s'éloignent de plus en plus les unes des autres, pour que les petits comme les
 * grands en atteignent une nouvelle régulièrement tout au long de l'année.
 */
object Tournee {

    private val NOMS = listOf(
        "Le Bureau des Nuages",
        "L'île des Petits Facteurs",
        "L'île aux Timbres",
        "L'île des Enveloppes",
        "L'île du Vent Doux",
        "L'île aux Cerfs-Volants",
        "L'île des Arcs-en-Ciel",
        "L'île aux Plumes",
        "L'île de la Boîte aux Lettres",
        "L'île des Montgolfières",
        "L'île du Tampon d'Or",
        "L'île aux Nuages Roses",
        "L'île des Colis Perdus",
        "L'île du Phare des Brumes",
        "L'île aux Étoiles Filantes",
        "L'île des Moulins à Vent",
        "L'île de la Grande Sacoche",
        "L'île aux Pigeons",
        "L'île des Cartes Postales",
        "L'île du Soleil Levant",
        "L'île aux Flocons",
        "L'île des Hirondelles",
        "L'île de la Lune Ronde",
        "L'île aux Bulles",
        "L'île du Grand Tri",
        "L'île aux Comètes",
        "L'île des Lanternes",
        "L'île du Mont Cumulus",
        "L'île aux Aurores",
        "L'île des Mille Lettres",
        "L'île du Ciel Bleu",
        "L'île aux Constellations",
        "L'île de l'Arc d'Or",
        "L'île des Grands Vents",
        "L'île aux Mille Ailes",
        "L'île du Bout du Ciel",
    )

    /** Le nom de l'île [k] (0 : le départ). Au-delà du Bout du Ciel, les îles sont numérotées. */
    fun nom(k: Int): String = NOMS.getOrNull(k) ?: "L'île n° $k"

    /** Les livraisons qu'il faut pour atteindre l'île [k] : 40, 90, 150, 220… */
    fun seuil(k: Int): Long = 5L * k * k + 35L * k

    /** La dernière île atteinte avec [livraisons]. */
    fun ileAtteinte(livraisons: Long): Int {
        var k = 0
        while (seuil(k + 1) <= livraisons) k++
        return k
    }

    /** Le chemin parcouru entre la dernière île atteinte et la suivante, de 0 à 1. */
    fun avancee(livraisons: Long): Float {
        val k = ileAtteinte(livraisons)
        val depuis = livraisons - seuil(k)
        return (depuis.toFloat() / (seuil(k + 1) - seuil(k)).toFloat()).coerceIn(0f, 1f)
    }

    /** Ce qu'il reste à livrer pour atteindre la prochaine île. */
    fun resteAvantLaSuivante(livraisons: Long): Long = seuil(ileAtteinte(livraisons) + 1) - livraisons.coerceAtLeast(0)
}

/** 1234 → « 1 234 » (espace insécable entre les milliers). */
internal fun nombreEnTexte(n: Long): String = n.toString().reversed().chunked(3).joinToString("\u00A0").reversed()
