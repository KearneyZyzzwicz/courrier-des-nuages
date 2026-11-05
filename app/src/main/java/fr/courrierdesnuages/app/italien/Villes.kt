package fr.courrierdesnuages.app.italien

/** Une ville d'Italie, avec sa région et son emplacement. */
data class Ville(val nom: String, val region: String, val latitude: Double, val longitude: Double)

/**
 * La carte de l'Italie de l'album (dessin res/drawable/carte_italie.xml, 1000 × 1299).
 * Projection équirectangulaire : mêmes réglages que ceux qui ont servi à dessiner la carte.
 */
object CarteItalie {
    const val LARGEUR = 1000f
    const val HAUTEUR = 1299f
    private const val ECHELLE = 107.3876493492991
    private const val COSINUS = 0.7513504932531299
    private const val LONGITUDE_GAUCHE = 6.376621418032804
    private const val LATITUDE_HAUT = 47.34155336139742

    /** Position sur la carte, de 0 à 1 dans chaque sens (0, 0 = coin en haut à gauche). */
    fun position(latitude: Double, longitude: Double): Pair<Float, Float> {
        val x = (longitude - LONGITUDE_GAUCHE) * COSINUS * ECHELLE
        val y = (LATITUDE_HAUT - latitude) * ECHELLE
        return Pair((x / LARGEUR).toFloat(), (y / HAUTEUR).toFloat())
    }

    fun position(ville: Ville): Pair<Float, Float> = position(ville.latitude, ville.longitude)
}

/** Les villes que le parent peut choisir pour une nouvelle carte postale. */
val VILLES: List<Ville> = listOf(
    Ville("Agrigente", "Sicile", 37.311, 13.576),
    Ville("Alberobello", "Pouilles", 40.784, 17.24),
    Ville("Alghero", "Sardaigne", 40.558, 8.319),
    Ville("Amalfi", "Campanie", 40.634, 14.603),
    Ville("Ancône", "Marches", 43.616, 13.519),
    Ville("Aoste", "Vallée d'Aoste", 45.737, 7.32),
    Ville("Assise", "Ombrie", 43.07, 12.617),
    Ville("Asti", "Piémont", 44.9, 8.207),
    Ville("Bari", "Pouilles", 41.117, 16.872),
    Ville("Bergame", "Lombardie", 45.698, 9.677),
    Ville("Bologne", "Émilie-Romagne", 44.494, 11.343),
    Ville("Bolzano", "Trentin-Haut-Adige", 46.498, 11.354),
    Ville("Brescia", "Lombardie", 45.541, 10.211),
    Ville("Cagliari", "Sardaigne", 39.223, 9.122),
    Ville("Campobasso", "Molise", 41.561, 14.668),
    Ville("Capri", "Campanie", 40.551, 14.243),
    Ville("Catane", "Sicile", 37.502, 15.087),
    Ville("Catanzaro", "Calabre", 38.91, 16.587),
    Ville("Chieti", "Abruzzes", 42.351, 14.168),
    Ville("Côme", "Lombardie", 45.808, 9.085),
    Ville("Cortina d'Ampezzo", "Vénétie", 46.54, 12.136),
    Ville("Cosenza", "Calabre", 39.299, 16.25),
    Ville("Courmayeur", "Vallée d'Aoste", 45.796, 6.972),
    Ville("Ferrare", "Émilie-Romagne", 44.836, 11.619),
    Ville("Florence", "Toscane", 43.77, 11.256),
    Ville("Foggia", "Pouilles", 41.462, 15.545),
    Ville("Gênes", "Ligurie", 44.405, 8.946),
    Ville("Isernia", "Molise", 41.596, 14.233),
    Ville("La Spezia", "Ligurie", 44.102, 9.824),
    Ville("L'Aquila", "Abruzzes", 42.35, 13.4),
    Ville("Lecce", "Pouilles", 40.353, 18.174),
    Ville("Livourne", "Toscane", 43.548, 10.311),
    Ville("Lucques", "Toscane", 43.843, 10.505),
    Ville("Mantoue", "Lombardie", 45.156, 10.791),
    Ville("Matera", "Basilicate", 40.666, 16.604),
    Ville("Messine", "Sicile", 38.194, 15.554),
    Ville("Milan", "Lombardie", 45.464, 9.19),
    Ville("Modène", "Émilie-Romagne", 44.647, 10.925),
    Ville("Naples", "Campanie", 40.852, 14.268),
    Ville("Novare", "Piémont", 45.446, 8.622),
    Ville("Nuoro", "Sardaigne", 40.321, 9.331),
    Ville("Olbia", "Sardaigne", 40.923, 9.498),
    Ville("Orvieto", "Ombrie", 42.718, 12.111),
    Ville("Padoue", "Vénétie", 45.406, 11.877),
    Ville("Palerme", "Sicile", 38.116, 13.361),
    Ville("Parme", "Émilie-Romagne", 44.801, 10.328),
    Ville("Pérouse", "Ombrie", 43.112, 12.389),
    Ville("Pescara", "Abruzzes", 42.464, 14.214),
    Ville("Pise", "Toscane", 43.716, 10.402),
    Ville("Pompéi", "Campanie", 40.749, 14.5),
    Ville("Portofino", "Ligurie", 44.303, 9.21),
    Ville("Potenza", "Basilicate", 40.64, 15.806),
    Ville("Ravenne", "Émilie-Romagne", 44.418, 12.204),
    Ville("Reggio de Calabre", "Calabre", 38.111, 15.647),
    Ville("Rimini", "Émilie-Romagne", 44.06, 12.566),
    Ville("Rome", "Latium", 41.903, 12.496),
    Ville("Salerne", "Campanie", 40.682, 14.769),
    Ville("San Gimignano", "Toscane", 43.468, 11.043),
    Ville("San Remo", "Ligurie", 43.816, 7.776),
    Ville("Sassari", "Sardaigne", 40.726, 8.556),
    Ville("Sienne", "Toscane", 43.318, 11.331),
    Ville("Syracuse", "Sicile", 37.075, 15.287),
    Ville("Taormina", "Sicile", 37.852, 15.288),
    Ville("Tarente", "Pouilles", 40.464, 17.247),
    Ville("Termoli", "Molise", 42.0, 14.995),
    Ville("Trapani", "Sicile", 38.018, 12.514),
    Ville("Trente", "Trentin-Haut-Adige", 46.07, 11.121),
    Ville("Trieste", "Frioul-Vénétie Julienne", 45.649, 13.777),
    Ville("Tropea", "Calabre", 38.677, 15.898),
    Ville("Turin", "Piémont", 45.07, 7.687),
    Ville("Udine", "Frioul-Vénétie Julienne", 46.063, 13.235),
    Ville("Urbino", "Marches", 43.726, 12.636),
    Ville("Venise", "Vénétie", 45.44, 12.316),
    Ville("Vérone", "Vénétie", 45.438, 10.992),
    Ville("Viterbe", "Latium", 42.418, 12.105),
)

fun villeNommee(nom: String): Ville? = VILLES.firstOrNull { it.nom == nom }
