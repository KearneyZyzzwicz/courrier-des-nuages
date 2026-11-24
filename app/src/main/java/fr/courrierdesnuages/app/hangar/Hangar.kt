package fr.courrierdesnuages.app.hangar

import fr.courrierdesnuages.app.data.Matiere
import fr.courrierdesnuages.app.data.Profil

/**
 * Un engin volant du hangar. Le pilote (la tête de l'avatar) se pose sur le siège : son visage est
 * centré en ([siegeX], [siegeY]) sur un dessin de 320 × 320, à l'échelle [siegeEchelle].
 */
data class Engin(
    val cle: String,
    val nom: String,
    /** « en tapis volant » : pour « Le hérisson en tapis volant t'attend pour la tournée. » */
    val en: String,
    /** Les cinq pièces à gagner, dans l'ordre. */
    val pieces: List<String>,
    val siegeX: Float,
    val siegeY: Float,
    val siegeEchelle: Float,
)

/** Les 15 engins, dans l'ordre où ils se construisent. */
val ENGINS: List<Engin> = listOf(
    Engin("tapis", "Le tapis volant à colis", "en tapis volant",
        listOf("le tapis", "les franges", "les motifs dorés", "les colis", "la formule magique"), 132f, 178f, 1.0f),
    Engin("baignoire", "La baignoire à hélice", "en baignoire à hélice",
        listOf("la baignoire", "les pieds de lion", "le mât", "l'hélice", "les bulles de savon"), 160f, 176f, 1.0f),
    Engin("montgolfiere", "La montgolfière à rayures", "en montgolfière à rayures",
        listOf("la nacelle", "les cordes", "le brûleur", "le ballon", "les rayures"), 160f, 226f, 0.82f),
    Engin("velo", "Le vélo volant", "en vélo volant",
        listOf("les roues", "le guidon", "le panier", "les ailes", "la sonnette"), 242f, 140f, 0.72f),
    Engin("zeppelin", "Le zeppelin postal", "en zeppelin postal",
        listOf("le ballon", "les ailerons", "la nacelle", "l'hélice", "l'enveloppe"), 168f, 194f, 0.72f),
    Engin("theiere", "La théière volante", "en théière volante",
        listOf("la théière", "le bec verseur", "le couvercle", "l'hélice", "les pois blancs"), 160f, 110f, 0.84f),
    Engin("hydravion", "L'hydravion", "en hydravion",
        listOf("les flotteurs", "la carlingue", "les ailes", "le pare-brise", "l'hélice"), 150f, 150f, 0.8f),
    Engin("cerf_volant", "Le cerf-volant géant", "en cerf-volant géant",
        listOf("la toile", "les baguettes", "la queue à nœuds", "les ficelles", "la nacelle"), 230f, 190f, 0.74f),
    Engin("nuage", "Le nuage à hélice", "en nuage à hélice",
        listOf("le nuage", "le siège moelleux", "le volant", "l'hélice", "les gouttes de pluie"), 166f, 182f, 0.95f),
    Engin("parapluie", "Le parapluie volant", "en parapluie volant",
        listOf("la toile", "le manche", "les rubans", "les cordes", "le panier"), 182f, 210f, 0.78f),
    Engin("autogire", "L'autogire", "en autogire",
        listOf("la carlingue", "les roues", "le rotor", "l'hélice", "le gouvernail"), 150f, 156f, 0.95f),
    Engin("traineau", "Le traîneau volant", "en traîneau volant",
        listOf("les patins", "le traîneau", "les cadeaux", "les grelots", "la poussière d'étoiles"), 176f, 162f, 0.86f),
    Engin("boite_aux_lettres", "La boîte aux lettres volante", "en boîte aux lettres volante",
        listOf("la boîte", "le hublot", "les ailes", "l'hélice", "les lettres"), 160f, 172f, 0.86f),
    Engin("bateau", "Le bateau volant", "en bateau volant",
        listOf("la coque", "le mât", "la voile", "les ailes", "le drapeau"), 122f, 172f, 0.86f),
    Engin("soucoupe", "La soucoupe volante", "en soucoupe volante",
        listOf("la soucoupe", "la coupole", "les lumières", "les antennes", "le rayon magique"), 160f, 168f, 0.95f),
)

/** La pièce que vient de rapporter une séance de maths, et l'engin s'il est maintenant terminé. */
data class NouvellePiece(val engin: Engin, val piece: String, val numero: Int, val termine: Boolean)

/**
 * Le hangar des engins : chaque médaille d'or en maths (toutes les années) est une pièce, comme
 * chaque pièce offerte par le parent, et cinq pièces font un nouvel engin. L'enfant choisit l'engin
 * de son facteur parmi ceux qu'il a construits.
 */
object Hangar {
    const val PIECES_PAR_ENGIN = 5

    /** Les engins construits avec [pieces] pièces. */
    fun construits(pieces: Long): List<Engin> = ENGINS.take((pieces.coerceAtLeast(0) / PIECES_PAR_ENGIN).toInt())

    /** L'engin en construction et ses pièces déjà là ; null quand tout le hangar est construit. */
    fun enConstruction(pieces: Long): Pair<Engin, Int>? {
        val p = pieces.coerceAtLeast(0)
        val engin = ENGINS.getOrNull((p / PIECES_PAR_ENGIN).toInt()) ?: return null
        return engin to (p % PIECES_PAR_ENGIN).toInt()
    }

    /** La pièce gagnée en passant à [pieces] pièces (null au-delà du dernier engin). */
    fun nouvellePiece(pieces: Long): NouvellePiece? {
        if (pieces <= 0) return null
        val i = ((pieces - 1) / PIECES_PAR_ENGIN).toInt()
        val engin = ENGINS.getOrNull(i) ?: return null
        val k = ((pieces - 1) % PIECES_PAR_ENGIN).toInt()
        return NouvellePiece(engin, engin.pieces[k], k + 1, termine = k == PIECES_PAR_ENGIN - 1)
    }

    /** L'engin choisi, s'il est bien construit (une remise à zéro peut reprendre une pièce). */
    fun enginChoisi(cle: String?, pieces: Long): Engin? = cle?.let { c -> construits(pieces).firstOrNull { it.cle == c } }
}

/** Les pièces du hangar d'un enfant : ses médailles d'or en maths, et les pièces offertes par le parent. */
val Profil.piecesDuHangar: Long get() = medaillesDe(Matiere.MATHS).or + piecesOffertes.coerceAtLeast(0)

/** Des pièces offertes par le parent attendent l'enfant sur son accueil. */
val Profil.pieceOfferteEnAttente: Boolean get() = piecesOffertes > piecesOffertesVues

/** L'engin du facteur de l'enfant, ou null pour son engin de départ (celui de l'avatar). */
val Profil.enginDuFacteur: Engin? get() = Hangar.enginChoisi(engin, piecesDuHangar)
