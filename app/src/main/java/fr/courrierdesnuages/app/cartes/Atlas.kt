package fr.courrierdesnuages.app.cartes

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Une forme de la carte (un département, un pays) : ses contours en unités de carte (la carte
 * fait 1000 unités de large), et le point où écrire son nom.
 */
class Forme(val id: String, val anneaux: List<FloatArray>, val etiquetteX: Float, val etiquetteY: Float) {
    val gauche: Float
    val haut: Float
    val droite: Float
    val bas: Float

    init {
        var g = Float.MAX_VALUE
        var h = Float.MAX_VALUE
        var d = -Float.MAX_VALUE
        var b = -Float.MAX_VALUE
        for (a in anneaux) {
            var i = 0
            while (i < a.size) {
                g = min(g, a[i])
                d = max(d, a[i])
                h = min(h, a[i + 1])
                b = max(b, a[i + 1])
                i += 2
            }
        }
        gauche = g
        haut = h
        droite = d
        bas = b
    }

    val largeur: Float get() = droite - gauche
    val hauteur: Float get() = bas - haut

    /** Vrai si le point est dans la forme (îles comprises, trous exclus). */
    fun contient(x: Float, y: Float): Boolean {
        if (x < gauche || x > droite || y < haut || y > bas) return false
        var dedans = false
        for (a in anneaux) {
            val n = a.size / 2
            var j = n - 1
            for (i in 0 until n) {
                val xi = a[2 * i]
                val yi = a[2 * i + 1]
                val xj = a[2 * j]
                val yj = a[2 * j + 1]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) dedans = !dedans
                j = i
            }
        }
        return dedans
    }

    /** La distance du point au bord de la forme, 0 s'il est dedans. */
    fun distance(x: Float, y: Float): Float {
        if (contient(x, y)) return 0f
        var m = Float.MAX_VALUE
        for (a in anneaux) {
            val n = a.size / 2
            for (i in 0 until n) {
                val j = (i + 1) % n
                m = min(m, distanceAuSegment(x, y, a[2 * i], a[2 * i + 1], a[2 * j], a[2 * j + 1]))
            }
        }
        return m
    }

    private fun distanceAuSegment(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val l = dx * dx + dy * dy
        val t = if (l == 0f) 0f else (((x - x1) * dx + (y - y1) * dy) / l).coerceIn(0f, 1f)
        return hypot(x - (x1 + t * dx), y - (y1 + t * dy))
    }
}

/**
 * Une carte faite de formes, lue dans un fichier texte (res/raw) fabriqué à partir de données
 * géographiques libres. La projection est la même que celle qui a servi à dessiner les formes, pour
 * y placer les villes.
 */
class Atlas(
    val largeur: Float,
    val hauteur: Float,
    private val longitudeGauche: Double,
    private val latitudeHaut: Double,
    private val cosinus: Double,
    private val echelle: Double,
    /** Projection de Miller (carte du monde) plutôt qu'équirectangulaire (carte d'un pays). */
    private val miller: Boolean,
    val formes: Map<String, Forme>,
) {
    /** La latitude « projetée », en degrés : un peu étirée vers les pôles avec Miller. */
    private fun projetee(latitude: Double): Double =
        if (miller) 1.25 * ln(tan(PI / 4 + 0.4 * Math.toRadians(latitude))) * 180 / PI else latitude

    /** La position d'un lieu sur la carte, en unités de carte. */
    fun position(latitude: Double, longitude: Double): Pair<Float, Float> = Pair(
        ((longitude - longitudeGauche) * cosinus * echelle).toFloat(),
        ((projetee(latitudeHaut) - projetee(latitude)) * echelle).toFloat(),
    )

    /** La forme sous le point, s'il y en a une. */
    fun formeEn(x: Float, y: Float): Forme? = formes.values.firstOrNull { it.contient(x, y) }

    /**
     * L'enfant touche (x, y) pour placer la forme [cible]. C'est juste s'il touche la bonne forme,
     * ou s'il touche tout près d'elle ([rayon], la marge du doigt) en dehors de toute forme. Une forme
     * plus petite que [petite] (trop petite pour le doigt, comme Paris) se touche aussi depuis ses
     * voisines, dans la même marge. Tout est en unités de carte.
     */
    fun bienPlace(cible: String, x: Float, y: Float, rayon: Float, petite: Float): Boolean {
        val f = formes[cible] ?: return false
        val touchee = formeEn(x, y)
        if (touchee?.id == cible) return true
        val proche = f.distance(x, y) <= rayon
        return if (touchee == null) proche else proche && max(f.largeur, f.hauteur) < petite
    }

    companion object {
        /** Lit un atlas au format de fabriquer_atlas.py (coordonnées au dixième, en écarts). */
        fun lire(texte: String): Atlas {
            var entete: List<String>? = null
            val formes = LinkedHashMap<String, Forme>()
            for (ligne in texte.lineSequence()) {
                if (ligne.isBlank() || ligne.startsWith("#")) continue
                if (ligne.startsWith("@")) {
                    entete = ligne.substring(1).trim().split(' ')
                    continue
                }
                val parties = ligne.split('|')
                if (parties.size < 3) continue
                val (lx, ly) = parties[1].split(',').map { it.toInt() / 10f }
                val anneaux = parties[2].split(';').map { anneau ->
                    val points = anneau.split(' ')
                    val a = FloatArray(points.size * 2)
                    var x = 0
                    var y = 0
                    points.forEachIndexed { i, p ->
                        val virgule = p.indexOf(',')
                        val px = p.substring(0, virgule).toInt()
                        val py = p.substring(virgule + 1).toInt()
                        if (i == 0) {
                            x = px
                            y = py
                        } else {
                            x += px
                            y += py
                        }
                        a[2 * i] = x / 10f
                        a[2 * i + 1] = y / 10f
                    }
                    a
                }
                formes[parties[0]] = Forme(parties[0], anneaux, lx, ly)
            }
            val e = requireNotNull(entete) { "Atlas sans en-tête" }
            return Atlas(
                largeur = e[0].toFloat(),
                hauteur = e[1].toFloat(),
                longitudeGauche = e[2].toDouble(),
                latitudeHaut = e[3].toDouble(),
                cosinus = e[4].toDouble(),
                echelle = e[5].toDouble(),
                miller = e.getOrNull(6) == "miller",
                formes = formes,
            )
        }
    }
}
