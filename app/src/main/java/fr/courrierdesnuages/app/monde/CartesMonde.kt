package fr.courrierdesnuages.app.monde

/** Les grandes régions du monde anglophone (la couleur de leurs pays sur la carte). */
enum class Continent { EUROPE, AMERIQUE, CARAIBES, AFRIQUE, ASIE, OCEANIE }

/**
 * Une carte postale du tour du monde des pays anglophones, rapportée par Mister Owl (le hibou facteur
 * de nuit, dans son bus) après une semaine d'anglais sans faute. [pays] est le code du pays sur la carte du monde (Natural Earth).
 */
data class CarteMonde(
    val id: String,
    val pays: String,
    val nomPays: String,
    /** Le pays avec son article : « le Royaume-Uni », « l'Inde », « les Bahamas », « Malte ». */
    val lePays: String,
    val continent: Continent,
    val drapeau: String,
    val ville: String,
    /** Le nom anglais de la ville, pour le « Greetings from… » du recto. */
    val villeAnglaise: String,
    val latitude: Double,
    val longitude: Double,
    val paragraphes: List<String>,
    val motFrancais: String,
    val motAnglais: String,
    val legende: String,
    val motif: String,
)

private typealias C = Continent

/** Les 22 cartes du tour du monde anglophone. */
val TOUR_DU_MONDE: List<CarteMonde> = listOf(
    CarteMonde(
        "londres", "GBR", "Royaume-Uni", "le Royaume-Uni", C.EUROPE, "🇬🇧", "Londres", "London", 51.5007, -0.1246,
        listOf(
            "Je me pose à Londres, la capitale du Royaume-Uni, au bord de la Tamise.",
            "Écoute : « Big Ben » n'est pas le nom de la tour, mais celui de sa cloche géante, qui sonne les heures depuis plus de 150 ans !",
            "À la tour de Londres, on garde toujours des corbeaux : une légende dit que, s'ils partaient, le royaume tomberait.",
        ),
        "horloge", "clock", "LONDON · BIG BEN", "💂",
    ),
    CarteMonde(
        "inverness", "GBR", "Royaume-Uni", "le Royaume-Uni", C.EUROPE, "🇬🇧", "Inverness", "Inverness", 57.4778, -4.2247,
        listOf(
            "Je me pose à Inverness, en Écosse, tout près d'un lac très long et très profond : le loch Ness.",
            "Une légende raconte qu'un monstre, Nessie, vit au fond de ses eaux sombres. Beaucoup de gens l'ont cherché… personne ne l'a jamais trouvé !",
            "En Écosse, on joue de la cornemuse, et certains hommes portent le kilt, une jupe à carreaux.",
        ),
        "monstre", "monster", "INVERNESS · LE LOCH NESS", "🐉",
    ),
    CarteMonde(
        "dublin", "IRL", "Irlande", "l'Irlande", C.EUROPE, "🇮🇪", "Dublin", "Dublin", 53.3498, -6.2603,
        listOf(
            "Je me pose à Dublin, la capitale de l'Irlande, l'île verte.",
            "Saint Patrick, qui a apporté la foi chrétienne aux Irlandais, expliquait la Sainte Trinité avec une feuille de trèfle : trois feuilles, une seule plante !",
            "Chaque 17 mars, pour sa fête, tout le monde s'habille en vert.",
        ),
        "vert", "green", "DUBLIN · LA SAINT-PATRICK", "☘️",
    ),
    CarteMonde(
        "new-york", "USA", "États-Unis", "les États-Unis", C.AMERIQUE, "🇺🇸", "New York", "New York", 40.7128, -74.0060,
        listOf(
            "Je me pose à New York, la ville des gratte-ciel, aux États-Unis.",
            "À l'entrée du port se dresse la statue de la Liberté. C'est un cadeau de la France ! Elle a été sculptée par Auguste Bartholdi, et sa torche brille au-dessus de la mer.",
            "Ici, les taxis sont jaunes, et la ville ne dort jamais.",
        ),
        "liberté", "liberty", "NEW YORK · LA STATUE DE LA LIBERTÉ", "🗽",
    ),
    CarteMonde(
        "washington", "USA", "États-Unis", "les États-Unis", C.AMERIQUE, "🇺🇸", "Washington", "Washington", 38.8977, -77.0365,
        listOf(
            "Je me pose à Washington, la capitale des États-Unis.",
            "Le président du pays habite et travaille dans une grande maison toute blanche : la Maison-Blanche.",
            "La ville porte le nom de George Washington, le premier président des États-Unis.",
        ),
        "maison", "house", "WASHINGTON · LA MAISON-BLANCHE", "🏛️",
    ),
    CarteMonde(
        "san-francisco", "USA", "États-Unis", "les États-Unis", C.AMERIQUE, "🇺🇸", "San Francisco", "San Francisco", 37.7749, -122.4194,
        listOf(
            "Je me pose à San Francisco, sur la côte ouest des États-Unis, au bord de l'océan Pacifique.",
            "Son grand pont rouge, le Golden Gate, disparaît souvent dans le brouillard du matin.",
            "Dans ses rues qui montent et qui descendent, de vieux tramways sont tirés par des câbles cachés sous la chaussée !",
        ),
        "pont", "bridge", "SAN FRANCISCO · LE GOLDEN GATE", "🌉",
    ),
    CarteMonde(
        "cap-canaveral", "USA", "États-Unis", "les États-Unis", C.AMERIQUE, "🇺🇸", "Cap Canaveral", "Cape Canaveral", 28.3922, -80.6077,
        listOf(
            "Je me pose au cap Canaveral, en Floride, au bord de l'océan Atlantique.",
            "C'est d'ici que sont parties les fusées vers la Lune. En 1969, les astronautes Neil Armstrong et Buzz Aldrin ont été les premiers à y marcher !",
            "Aujourd'hui encore, des fusées décollent d'ici vers l'espace.",
        ),
        "lune", "moon", "CAPE CANAVERAL · LES FUSÉES", "🚀",
    ),
    CarteMonde(
        "honolulu", "USA", "États-Unis", "les États-Unis", C.OCEANIE, "🇺🇸", "Honolulu", "Honolulu", 21.3069, -157.8583,
        listOf(
            "Je me pose à Honolulu, sur les îles d'Hawaï, au milieu de l'océan Pacifique. Elles font partie des États-Unis.",
            "Les Hawaïens ont inventé le surf il y a très longtemps : ils glissaient sur les vagues, debout sur de longues planches de bois.",
            "Sur ces îles, des volcans crachent encore de la lave, qui fabrique de nouvelles terres.",
        ),
        "vague", "wave", "HONOLULU · LES VAGUES D'HAWAÏ", "🏄",
    ),
    CarteMonde(
        "toronto", "CAN", "Canada", "le Canada", C.AMERIQUE, "🇨🇦", "Toronto", "Toronto", 43.6532, -79.3832,
        listOf(
            "Je me pose à Toronto, la plus grande ville du Canada. Ici, on parle anglais, mais beaucoup de Canadiens parlent aussi français !",
            "Pas loin d'ici, les chutes du Niagara font un bruit de tonnerre, à la frontière avec les États-Unis.",
            "Le drapeau du Canada porte une feuille d'érable : avec la sève de cet arbre, on fait un délicieux sirop.",
        ),
        "érable", "maple", "TORONTO · LES CHUTES DU NIAGARA", "🍁",
    ),
    CarteMonde(
        "sydney", "AUS", "Australie", "l'Australie", C.OCEANIE, "🇦🇺", "Sydney", "Sydney", -33.8568, 151.2153,
        listOf(
            "Je me pose à Sydney, en Australie, de l'autre côté de la Terre !",
            "Son opéra a des toits blancs en forme de voiles de bateau. Et ici, Noël tombe en plein été : on le fête souvent à la plage !",
            "Dans les parcs, tu peux croiser des kangourous et des koalas, qui dorment presque toute la journée.",
        ),
        "kangourou", "kangaroo", "SYDNEY · L'OPÉRA", "🐨",
    ),
    CarteMonde(
        "cairns", "AUS", "Australie", "l'Australie", C.OCEANIE, "🇦🇺", "Cairns", "Cairns", -16.9186, 145.7781,
        listOf(
            "Je me pose à Cairns, dans le nord de l'Australie.",
            "Au large, dans la mer chaude, s'étend la Grande Barrière de corail : le plus grand récif de corail du monde ! On peut même la voir depuis l'espace.",
            "Des milliers de poissons de toutes les couleurs et des tortues de mer y vivent.",
        ),
        "poisson", "fish", "CAIRNS · LA GRANDE BARRIÈRE DE CORAIL", "🐠",
    ),
    CarteMonde(
        "wellington", "NZL", "Nouvelle-Zélande", "la Nouvelle-Zélande", C.OCEANIE, "🇳🇿", "Wellington", "Wellington", -41.2865, 174.7762,
        listOf(
            "Je me pose à Wellington, la capitale de la Nouvelle-Zélande.",
            "Ici vit le kiwi, un drôle d'oiseau qui ne sait pas voler ! Il est si aimé que les Néo-Zélandais se surnomment eux-mêmes « les Kiwis ».",
            "Avant chaque match, les joueurs de rugby dansent le haka, une danse des Maoris, le premier peuple de ces îles.",
        ),
        "oiseau", "bird", "WELLINGTON · LE PAYS DU KIWI", "🥝",
    ),
    CarteMonde(
        "agra", "IND", "Inde", "l'Inde", C.ASIE, "🇮🇳", "Agra", "Agra", 27.1751, 78.0421,
        listOf(
            "Je me pose à Agra, en Inde, un pays où l'anglais est l'une des langues officielles.",
            "Voici le Taj Mahal, un immense monument de marbre blanc. Il y a près de 400 ans, un empereur l'a fait construire en souvenir de sa femme, qu'il aimait tant.",
            "Le marbre paraît rose au lever du soleil, blanc à midi et doré le soir !",
        ),
        "amour", "love", "AGRA · LE TAJ MAHAL", "🕌",
    ),
    CarteMonde(
        "le-cap", "ZAF", "Afrique du Sud", "l'Afrique du Sud", C.AFRIQUE, "🇿🇦", "Le Cap", "Cape Town", -33.9249, 18.4241,
        listOf(
            "Je me pose au Cap, tout au sud de l'Afrique, en Afrique du Sud.",
            "Au-dessus de la ville se dresse la montagne de la Table, plate comme une table. Quand un nuage blanc la recouvre, on dit qu'on a mis la nappe !",
            "Sur une plage voisine vivent des milliers de manchots, qui se promènent entre les baigneurs.",
        ),
        "manchot", "penguin", "CAPE TOWN · LA MONTAGNE DE LA TABLE", "🐧",
    ),
    CarteMonde(
        "nairobi", "KEN", "Kenya", "le Kenya", C.AFRIQUE, "🇰🇪", "Nairobi", "Nairobi", -1.2921, 36.8219,
        listOf(
            "Je me pose à Nairobi, la capitale du Kenya.",
            "Juste à côté des immeubles de la ville, un grand parc national abrite des lions, des zèbres, des girafes et des rhinocéros !",
            "Dans un refuge, les enfants peuvent donner à manger aux girafes, qui tendent leur longue langue bleue.",
        ),
        "girafe", "giraffe", "NAIROBI · LE PARC NATIONAL", "🦁",
    ),
    CarteMonde(
        "moshi", "TZA", "Tanzanie", "la Tanzanie", C.AFRIQUE, "🇹🇿", "Moshi", "Moshi", -3.3349, 37.3404,
        listOf(
            "Je me pose à Moshi, en Tanzanie, au pied du Kilimandjaro, la plus haute montagne d'Afrique : près de 6 000 mètres !",
            "Il fait très chaud ici, et pourtant son sommet est couvert de neige et de glace.",
            "En Tanzanie, on parle anglais et swahili. En swahili, « bonjour » se dit « jambo » !",
        ),
        "neige", "snow", "MOSHI · LE KILIMANDJARO", "🏔️",
    ),
    CarteMonde(
        "livingstone", "ZMB", "Zambie", "la Zambie", C.AFRIQUE, "🇿🇲", "Livingstone", "Livingstone", -17.8419, 25.8543,
        listOf(
            "Je me pose à Livingstone, en Zambie, près des chutes Victoria.",
            "Ici, le grand fleuve Zambèze tombe d'un coup dans une faille profonde. On appelle ces chutes « la fumée qui gronde », à cause du nuage d'eau qui monte vers le ciel !",
            "La ville porte le nom de David Livingstone, un explorateur écossais.",
        ),
        "chute d'eau", "waterfall", "LIVINGSTONE · LES CHUTES VICTORIA", "🌈",
    ),
    CarteMonde(
        "kingston", "JAM", "Jamaïque", "la Jamaïque", C.CARAIBES, "🇯🇲", "Kingston", "Kingston", 17.9712, -76.7936,
        listOf(
            "Je me pose à Kingston, la capitale de la Jamaïque, une île de la mer des Caraïbes.",
            "C'est le pays d'Usain Bolt, l'homme le plus rapide du monde : il a couru le 100 mètres en moins de 10 secondes !",
            "C'est aussi ici qu'est née la musique reggae, celle de Bob Marley.",
        ),
        "rapide", "fast", "KINGSTON · LE PAYS D'USAIN BOLT", "🏃",
    ),
    CarteMonde(
        "nassau", "BHS", "Bahamas", "les Bahamas", C.CARAIBES, "🇧🇸", "Nassau", "Nassau", 25.0443, -77.3504,
        listOf(
            "Je me pose à Nassau, aux Bahamas, un pays fait de centaines d'îles dans une mer turquoise.",
            "Sur une petite île, des cochons vivent sur la plage et nagent dans la mer pour venir saluer les visiteurs !",
            "Sur certaines plages, le sable est même rose.",
        ),
        "cochon", "pig", "NASSAU · LES COCHONS NAGEURS", "🐷",
    ),
    CarteMonde(
        "singapour", "SGP", "Singapour", "Singapour", C.ASIE, "🇸🇬", "Singapour", "Singapore", 1.2868, 103.8545,
        listOf(
            "Je me pose à Singapour, une ville qui est aussi un pays, tout au bout de l'Asie.",
            "Son symbole est le Merlion, une statue moitié lion, moitié poisson, qui crache de l'eau dans la baie.",
            "Dans ses jardins poussent des arbres géants en métal, couverts de plantes, qui s'illuminent la nuit !",
        ),
        "jardin", "garden", "SINGAPORE · LE MERLION", "🌴",
    ),
    CarteMonde(
        "la-valette", "MLT", "Malte", "Malte", C.EUROPE, "🇲🇹", "La Valette", "Valletta", 35.8989, 14.5146,
        listOf(
            "Je me pose à La Valette, la capitale de Malte, une petite île au milieu de la Méditerranée. Ici, on parle maltais et anglais.",
            "La Bible raconte que saint Paul a fait naufrage sur cette île, et qu'il y a annoncé l'Évangile.",
            "Plus tard, les chevaliers de Malte, des moines-soldats, y ont construit cette ville fortifiée.",
        ),
        "chevalier", "knight", "VALLETTA · LA VILLE DES CHEVALIERS", "🛡️",
    ),
    CarteMonde(
        "cebu", "PHL", "Philippines", "les Philippines", C.ASIE, "🇵🇭", "Cebu", "Cebu", 10.3157, 123.8854,
        listOf(
            "Je me pose à Cebu, aux Philippines, un pays de plus de 7 000 îles ! Il y a 500 ans, l'explorateur Magellan y a planté une grande croix, que l'on voit toujours.",
            "Aujourd'hui, les Philippines sont le plus grand pays catholique d'Asie.",
            "Sur une île voisine, des centaines de collines rondes deviennent brunes à la saison sèche : on les appelle les collines de chocolat !",
        ),
        "île", "island", "CEBU · LA CROIX DE MAGELLAN", "🍫",
    ),
)

private val PAR_ID: Map<String, CarteMonde> by lazy { TOUR_DU_MONDE.associateBy { it.id } }

fun carteMonde(id: String): CarteMonde? = PAR_ID[id]

/** Les pays du tour du monde, une seule fois chacun. */
val PAYS_DU_TOUR: Set<String> by lazy { TOUR_DU_MONDE.map { it.pays }.toSet() }
