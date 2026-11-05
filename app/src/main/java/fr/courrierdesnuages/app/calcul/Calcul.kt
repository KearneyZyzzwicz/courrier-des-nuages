package fr.courrierdesnuages.app.calcul

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val FOIS = '×'
const val PLUS = '+'

/** Un calcul des tables : 4 × 7, 7 × 4, 6 + 3… */
data class Calcul(val a: Int, val b: Int, val operation: Char) {

    val resultat: Int get() = if (operation == FOIS) a * b else a + b

    /** Tel qu'il est affiché sur l'étiquette du colis. */
    val texte: String get() = "$a $operation $b"

    /** Le même calcul quel que soit le sens (4 × 7 et 7 × 4) : sert pour les calculs à revoir. */
    val canonique: Calcul get() = Calcul(min(a, b), max(a, b), operation)

    fun retourne(): Calcul = Calcul(b, a, operation)

    companion object {
        private val forme = Regex("^\\s*(\\d{1,2})\\s*([×x*+])\\s*(\\d{1,2})\\s*$")

        /** Relit un calcul enregistré (« 4 × 7 »). */
        fun lire(texte: String): Calcul? {
            val m = forme.find(texte) ?: return null
            val op = if (m.groupValues[2] == "+") PLUS else FOIS
            return Calcul(m.groupValues[1].toInt(), m.groupValues[3].toInt(), op)
        }

        /** Code d'une table : « x4 » pour la table de multiplication de 4, « +6 » pour l'addition. */
        fun codeTable(operation: Char, n: Int): String = if (operation == FOIS) "x$n" else "+$n"

        /** Les 10 calculs d'une table, de 4 × 1 à 4 × 10. */
        fun table(code: String): List<Calcul> {
            val operation = if (code.startsWith("+")) PLUS else FOIS
            val n = code.drop(1).toIntOrNull()?.takeIf { it in 1..10 } ?: return emptyList()
            return (1..10).map { Calcul(n, it, operation) }
        }

        /**
         * Tous les calculs des tables choisies, chacun posé au hasard dans un sens ou dans l'autre,
         * dans un ordre aléatoire. Un calcul commun à deux tables (4 × 5 et 5 × 4) n'est posé qu'une fois.
         */
        fun pourLesTables(tables: List<String>, hasard: Random = Random.Default): List<Calcul> {
            val vus = HashSet<Calcul>()
            val calculs = ArrayList<Calcul>()
            for (code in tables) {
                for (c in table(code)) {
                    if (vus.add(c.canonique)) calculs += if (hasard.nextBoolean()) c else c.retourne()
                }
            }
            return calculs.shuffled(hasard)
        }

        /** 4 réponses pour la seconde chance : la bonne et 3 erreurs vraisemblables, mélangées. */
        fun propositions(c: Calcul, hasard: Random = Random.Default): List<Int> {
            val juste = c.resultat
            val proches = if (c.operation == FOIS) {
                listOf(
                    (c.a + 1) * c.b, (c.a - 1) * c.b, c.a * (c.b + 1), c.a * (c.b - 1),
                    juste + 1, juste - 1, juste + 10, juste - 10,
                )
            } else {
                listOf(juste + 1, juste - 1, juste + 2, juste - 2, juste + 10, juste - 10)
            }
            val candidats = proches.filter { it > 0 && it != juste }.distinct().shuffled(hasard)
                .sortedBy { abs(it - juste) > 10 } // les erreurs proches d'abord
            val fausses = LinkedHashSet<Int>()
            for (n in candidats) {
                if (fausses.size == 3) break
                fausses += n
            }
            var secours = juste + 3
            while (fausses.size < 3) {
                if (secours != juste && secours > 0) fausses += secours
                secours++
            }
            return (fausses.toList() + juste).shuffled(hasard)
        }
    }
}
