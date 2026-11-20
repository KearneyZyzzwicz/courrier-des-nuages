package fr.courrierdesnuages.app.france

/** Les treize régions de France métropolitaine (la couleur de leurs départements sur la carte). */
enum class Region(val nom: String, val dans: String) {
    AUVERGNE("Auvergne-Rhône-Alpes", "en Auvergne-Rhône-Alpes"),
    BOURGOGNE("Bourgogne-Franche-Comté", "en Bourgogne-Franche-Comté"),
    BRETAGNE("Bretagne", "en Bretagne"),
    CENTRE("Centre-Val de Loire", "dans le Centre-Val de Loire"),
    CORSE("Corse", "en Corse"),
    GRAND_EST("Grand Est", "dans le Grand Est"),
    HAUTS_DE_FRANCE("Hauts-de-France", "dans les Hauts-de-France"),
    ILE_DE_FRANCE("Île-de-France", "en Île-de-France"),
    NORMANDIE("Normandie", "en Normandie"),
    NOUVELLE_AQUITAINE("Nouvelle-Aquitaine", "en Nouvelle-Aquitaine"),
    OCCITANIE("Occitanie", "en Occitanie"),
    PAYS_DE_LA_LOIRE("Pays de la Loire", "dans les Pays de la Loire"),
    PROVENCE("Provence-Alpes-Côte d'Azur", "en Provence-Alpes-Côte d'Azur"),
}

/**
 * Un département de France métropolitaine et sa carte postale : une ville, son emplacement, et le
 * petit mot du Petit Avion. Le recto est dessiné par l'appli (la forme du département et un motif).
 */
data class Departement(
    val code: String,
    val nom: String,
    /** Le nom avec son article : « le Finistère », « l'Ain », « les Landes », « Paris ». */
    val leNom: String,
    val region: Region,
    val ville: String,
    val latitude: Double,
    val longitude: Double,
    val paragraphes: List<String>,
    val legende: String,
    /** Un emoji pour illustrer la carte. */
    val motif: String,
    /** Le nom des habitants de la ville, quand il vaut d'être appris. */
    val habitants: String? = null,
)

private typealias R = Region

/** Les 96 départements de métropole, Corse comprise, dans l'ordre de leur numéro. */
val DEPARTEMENTS: List<Departement> = listOf(
    Departement(
        "01", "Ain", "l'Ain", R.AUVERGNE, "Ars-sur-Formans", 45.9936, 4.8219,
        listOf(
            "Je me pose à Ars, un petit village de l'Ain.",
            "Il y a 200 ans, son curé, Jean-Marie Vianney, était si bon que des gens venaient de toute la France pour lui parler. Il les écoutait parfois plus de 15 heures par jour !",
            "On le prie aujourd'hui sous le nom de saint curé d'Ars : il est le patron de tous les curés du monde.",
        ),
        "ARS · LA BASILIQUE DU SAINT CURÉ", "⛪",
    ),
    Departement(
        "02", "Aisne", "l'Aisne", R.HAUTS_DE_FRANCE, "Laon", 49.5641, 3.6199,
        listOf(
            "Je me pose à Laon, une ville perchée sur une colline de l'Aisne.",
            "Lève les yeux vers les tours de la cathédrale : seize bœufs en pierre te regardent !",
            "Ils remercient les bœufs qui ont monté, il y a plus de 800 ans, les lourdes pierres du chantier jusqu'en haut de la colline.",
        ),
        "LAON · LES BŒUFS DE LA CATHÉDRALE", "🐂", "Laonnois",
    ),
    Departement(
        "03", "Allier", "l'Allier", R.AUVERGNE, "Vichy", 46.1277, 3.4259,
        listOf(
            "Je me pose à Vichy, au bord de la rivière Allier.",
            "Ici, l'eau sort de la terre toute chaude ! Depuis l'époque des Romains, on vient la boire et s'y baigner pour se soigner.",
            "Avec ses sels, on fabrique aussi des bonbons à la menthe à huit côtés : les pastilles de Vichy.",
        ),
        "VICHY · LA VILLE DES SOURCES", "💧", "Vichyssois",
    ),
    Departement(
        "04", "Alpes-de-Haute-Provence", "les Alpes-de-Haute-Provence", R.PROVENCE, "Moustiers-Sainte-Marie", 43.8466, 6.2214,
        listOf(
            "Je me pose à Moustiers-Sainte-Marie. Lève la tête : entre deux falaises, une étoile dorée est suspendue à une longue chaîne !",
            "La légende raconte qu'un chevalier, prisonnier pendant les croisades, avait promis à la Vierge Marie d'accrocher une étoile s'il rentrait chez lui.",
            "Il a tenu parole. Au-dessus du village, la chapelle Notre-Dame-de-Beauvoir veille sur les falaises.",
        ),
        "MOUSTIERS · L'ÉTOILE DES FALAISES", "⭐",
    ),
    Departement(
        "05", "Hautes-Alpes", "les Hautes-Alpes", R.PROVENCE, "Briançon", 44.8986, 6.6435,
        listOf(
            "Je me pose à Briançon, la plus haute ville de France, à plus de 1 300 mètres d'altitude !",
            "Pour protéger la frontière, Vauban, l'ingénieur du roi Louis XIV, l'a entourée de remparts et de forts.",
            "Ici, le soleil brille presque toute l'année, même quand il neige sur les montagnes.",
        ),
        "BRIANÇON · LES FORTS DE VAUBAN", "🏔️", "Briançonnais",
    ),
    Departement(
        "06", "Alpes-Maritimes", "les Alpes-Maritimes", R.PROVENCE, "Grasse", 43.6584, 6.9235,
        listOf(
            "Je me pose à Grasse, sur les collines au-dessus de la mer : c'est la capitale du parfum !",
            "Dans les champs, on cueille le jasmin et les roses à la main, très tôt le matin.",
            "Les créateurs de parfums sont appelés des « nez » : ils savent reconnaître des centaines d'odeurs.",
        ),
        "GRASSE · LES CHAMPS DE FLEURS", "🌸", "Grassois",
    ),
    Departement(
        "07", "Ardèche", "l'Ardèche", R.AUVERGNE, "Vallon-Pont-d'Arc", 44.4072, 4.3939,
        listOf(
            "Je me pose à Vallon-Pont-d'Arc. Ici, la rivière Ardèche passe sous une immense arche de pierre creusée par l'eau : le Pont d'Arc !",
            "Tout près, dans la grotte Chauvet, des hommes préhistoriques ont dessiné des lions, des chevaux et des mammouths, il y a plus de 30 000 ans.",
            "Pour protéger la vraie grotte, on visite une copie tout aussi belle.",
        ),
        "VALLON-PONT-D'ARC · L'ARCHE DE PIERRE", "🦁",
    ),
    Departement(
        "08", "Ardennes", "les Ardennes", R.GRAND_EST, "Charleville-Mézières", 49.7735, 4.7206,
        listOf(
            "Je me pose à Charleville-Mézières, la capitale des marionnettes ! Un grand festival y réunit des marionnettistes du monde entier.",
            "Sur une horloge géante, une marionnette raconte chaque heure un morceau de la légende des quatre fils Aymon.",
            "Les quatre frères s'enfuient sur Bayard, un cheval magique capable de les porter tous à la fois !",
        ),
        "CHARLEVILLE · LE GRAND MARIONNETTISTE", "🎭", "Carolomacériens",
    ),
    Departement(
        "09", "Ariège", "l'Ariège", R.OCCITANIE, "Foix", 42.9649, 1.6054,
        listOf(
            "Je me pose à Foix, au pied des Pyrénées.",
            "Sur un gros rocher, un château fort aux trois tours veille sur la ville depuis plus de mille ans.",
            "Ses murs étaient si solides qu'on le disait imprenable : aucune armée n'a réussi à le prendre de force.",
        ),
        "FOIX · LE CHÂTEAU DES COMTES", "🏰", "Fuxéens",
    ),
    Departement(
        "10", "Aube", "l'Aube", R.GRAND_EST, "Troyes", 48.2973, 4.0744,
        listOf(
            "Je me pose à Troyes. Vue du ciel, la vieille ville a la forme d'un bouchon de champagne !",
            "Ses maisons à colombages penchent tellement que, dans la ruelle des Chats, leurs toits se touchent presque.",
            "Les chats peuvent sauter d'un côté à l'autre sans jamais descendre dans la rue !",
        ),
        "TROYES · LA RUELLE DES CHATS", "🐈", "Troyens",
    ),
    Departement(
        "11", "Aude", "l'Aude", R.OCCITANIE, "Carcassonne", 43.2063, 2.3644,
        listOf(
            "Je me pose à Carcassonne. Sa Cité est une ville fortifiée avec deux rangées de remparts et plus de 50 tours !",
            "La légende raconte qu'un jour, l'armée de Charlemagne l'assiégeait depuis des années. Dame Carcas fit jeter un cochon bien nourri par-dessus les murs.",
            "Croyant qu'il restait beaucoup de nourriture dans la ville, l'armée s'en alla !",
        ),
        "CARCASSONNE · LA CITÉ ET SES REMPARTS", "🐖", "Carcassonnais",
    ),
    Departement(
        "12", "Aveyron", "l'Aveyron", R.OCCITANIE, "Millau", 44.0986, 3.0784,
        listOf(
            "Je me pose à Millau, dans l'Aveyron. Regarde ce pont géant au-dessus de la vallée du Tarn : c'est le viaduc de Millau !",
            "C'est l'un des ponts les plus hauts du monde : son plus grand pilier, avec son mât, dépasse la tour Eiffel.",
            "Les jours de brouillard, les voitures roulent au-dessus des nuages… comme moi !",
        ),
        "MILLAU · LE VIADUC", "🌉", "Millavois",
    ),
    Departement(
        "13", "Bouches-du-Rhône", "les Bouches-du-Rhône", R.PROVENCE, "Marseille", 43.2965, 5.3698,
        listOf(
            "Je me pose à Marseille, la plus vieille ville de France : des marins grecs l'ont fondée il y a plus de 2 600 ans !",
            "Tout en haut de la colline, la basilique Notre-Dame-de-la-Garde veille sur les marins et sur toute la ville.",
            "Les Marseillais l'appellent affectueusement « la Bonne Mère ».",
        ),
        "MARSEILLE · NOTRE-DAME-DE-LA-GARDE", "⛵", "Marseillais",
    ),
    Departement(
        "14", "Calvados", "le Calvados", R.NORMANDIE, "Bayeux", 49.2764, -0.7024,
        listOf(
            "Je me pose à Bayeux, dans le Calvados. Ici, on garde une broderie géante, longue de 70 mètres : la tapisserie de Bayeux !",
            "Comme une bande dessinée, elle raconte comment Guillaume le Conquérant a traversé la mer pour devenir roi d'Angleterre, il y a presque 1 000 ans.",
            "Cherche bien : on y voit des bateaux, des chevaux et même une comète !",
        ),
        "BAYEUX · LA TAPISSERIE", "☄️", "Bajocasses",
    ),
    Departement(
        "15", "Cantal", "le Cantal", R.AUVERGNE, "Aurillac", 44.9264, 2.4397,
        listOf(
            "Je me pose à Aurillac, au pied des volcans du Cantal. C'est la capitale du parapluie : on y en fabrique depuis très longtemps !",
            "Il y a plus de 1 000 ans, un jeune moine de l'abbaye d'Aurillac, Gerbert, était si savant qu'il est devenu pape.",
            "Sous le nom de Sylvestre II, ce fut le premier pape français.",
        ),
        "AURILLAC · LA VILLE DU PARAPLUIE", "☂️", "Aurillacois",
    ),
    Departement(
        "16", "Charente", "la Charente", R.NOUVELLE_AQUITAINE, "Angoulême", 45.6484, 0.1562,
        listOf(
            "Je me pose à Angoulême, la capitale de la bande dessinée !",
            "Chaque hiver, les dessinateurs du monde entier s'y retrouvent pour un grand festival.",
            "En te promenant, regarde bien les murs de la ville : de grands dessins de héros de BD y sont peints.",
        ),
        "ANGOULÊME · LES MURS PEINTS", "💬", "Angoumoisins",
    ),
    Departement(
        "17", "Charente-Maritime", "la Charente-Maritime", R.NOUVELLE_AQUITAINE, "La Rochelle", 46.1591, -1.1520,
        listOf(
            "Je me pose à La Rochelle, au bord de l'océan Atlantique.",
            "À l'entrée du vieux port, deux grosses tours se font face. Autrefois, chaque soir, on tendait entre elles une énorme chaîne pour fermer le port aux bateaux ennemis !",
            "L'une d'elles s'appelle d'ailleurs la tour de la Chaîne.",
        ),
        "LA ROCHELLE · LES TOURS DU VIEUX PORT", "⚓", "Rochelais",
    ),
    Departement(
        "18", "Cher", "le Cher", R.CENTRE, "Bourges", 47.0810, 2.3988,
        listOf(
            "Je me pose à Bourges, presque au centre de la France.",
            "Sa cathédrale Saint-Étienne est immense, avec des vitraux de toutes les couleurs qui racontent les histoires de la Bible.",
            "Ici vivait Jacques Cœur, un riche marchand qui envoyait ses bateaux jusqu'en Orient. Sa devise : « À vaillant cœur, rien d'impossible ! »",
        ),
        "BOURGES · LA CATHÉDRALE SAINT-ÉTIENNE", "❤️", "Berruyers",
    ),
    Departement(
        "19", "Corrèze", "la Corrèze", R.NOUVELLE_AQUITAINE, "Collonges-la-Rouge", 45.0597, 1.6553,
        listOf(
            "Je me pose à Collonges-la-Rouge, en Corrèze.",
            "Toutes les maisons de ce village sont construites en pierre rouge ! Au soleil couchant, elles brillent comme du feu.",
            "C'est ici qu'est née l'idée de choisir « les plus beaux villages de France ».",
        ),
        "COLLONGES-LA-ROUGE · LE VILLAGE ROUGE", "🏘️",
    ),
    Departement(
        "21", "Côte-d'Or", "la Côte-d'Or", R.BOURGOGNE, "Dijon", 47.3220, 5.0415,
        listOf(
            "Je me pose à Dijon, la ville de la moutarde et du pain d'épices !",
            "Sur un mur de l'église Notre-Dame, une petite chouette est sculptée dans la pierre. La tradition dit qu'il faut la caresser de la main gauche en faisant un vœu.",
            "À force d'être caressée, elle est devenue toute lisse.",
        ),
        "DIJON · LA CHOUETTE DE NOTRE-DAME", "🦉", "Dijonnais",
    ),
    Departement(
        "22", "Côtes-d'Armor", "les Côtes-d'Armor", R.BRETAGNE, "Perros-Guirec", 48.8148, -3.4392,
        listOf(
            "Je me pose à Perros-Guirec, sur la côte de Granit rose. Ici, les rochers sont roses !",
            "Le vent et la mer leur ont donné des formes étonnantes : on y voit une tortue, un champignon ou le chapeau de Napoléon.",
            "Amuse-toi à leur trouver un nom !",
        ),
        "PERROS-GUIREC · LA CÔTE DE GRANIT ROSE", "🐢", "Perrosiens",
    ),
    Departement(
        "23", "Creuse", "la Creuse", R.NOUVELLE_AQUITAINE, "Aubusson", 45.9559, 2.1676,
        listOf(
            "Je me pose à Aubusson, dans la Creuse. Depuis plus de 500 ans, on y tisse des tapisseries : de grands tableaux faits de fils de laine colorés.",
            "Les lissiers travaillent sur un métier à tisser, fil après fil.",
            "Une seule tapisserie peut demander des mois de travail !",
        ),
        "AUBUSSON · LES TAPISSERIES", "🐑", "Aubussonnais",
    ),
    Departement(
        "24", "Dordogne", "la Dordogne", R.NOUVELLE_AQUITAINE, "Montignac", 45.0644, 1.1619,
        listOf(
            "Je me pose à Montignac, en Dordogne.",
            "En 1940, quatre jeunes et leur chien, Robot, ont découvert un trou dans la colline. Il menait à une grotte couverte de peintures de chevaux, de cerfs et de taureaux : la grotte de Lascaux !",
            "Elles ont été peintes il y a plus de 17 000 ans. Aujourd'hui, on visite une copie, pour protéger les vraies.",
        ),
        "MONTIGNAC · LA GROTTE DE LASCAUX", "🐎",
    ),
    Departement(
        "25", "Doubs", "le Doubs", R.BOURGOGNE, "Besançon", 47.2378, 6.0241,
        listOf(
            "Je me pose à Besançon, nichée dans une boucle de la rivière Doubs.",
            "L'écrivain Victor Hugo et les frères Lumière, les inventeurs du cinéma, y sont nés !",
            "Dans la cathédrale, une horloge astronomique de 30 000 pièces donne l'heure, les marées et même les éclipses.",
        ),
        "BESANÇON · L'HORLOGE ASTRONOMIQUE", "🕰️", "Bisontins",
    ),
    Departement(
        "26", "Drôme", "la Drôme", R.AUVERGNE, "Hauterives", 45.2556, 5.0281,
        listOf(
            "Je me pose à Hauterives, dans la Drôme, chez un collègue : le facteur Cheval !",
            "Il y a plus de 100 ans, pendant ses tournées, il ramassait des cailloux aux formes bizarres.",
            "Pendant 33 ans, il les a assemblés tout seul pour construire un palais de rêve : le Palais idéal.",
        ),
        "HAUTERIVES · LE PALAIS IDÉAL DU FACTEUR CHEVAL", "✉️",
    ),
    Departement(
        "27", "Eure", "l'Eure", R.NORMANDIE, "Giverny", 49.0757, 1.5338,
        listOf(
            "Je me pose à Giverny, dans le jardin du peintre Claude Monet.",
            "Il y a creusé un étang, planté des nénuphars et construit un petit pont japonais tout vert.",
            "Pendant des années, il a peint ses nénuphars à toutes les heures du jour. Ses tableaux sont célèbres dans le monde entier.",
        ),
        "GIVERNY · LE JARDIN DE MONET", "🎨",
    ),
    Departement(
        "28", "Eure-et-Loir", "l'Eure-et-Loir", R.CENTRE, "Chartres", 48.4472, 1.4877,
        listOf(
            "Je me pose à Chartres. On voit sa cathédrale de très loin, au-dessus des champs de blé de la Beauce.",
            "Ses vitraux sont célèbres pour leur bleu magnifique : le « bleu de Chartres ».",
            "Sur le sol, un grand labyrinthe est dessiné. Les pèlerins le suivent en priant, jusqu'au centre.",
        ),
        "CHARTRES · LA CATHÉDRALE NOTRE-DAME", "⛪", "Chartrains",
    ),
    Departement(
        "29", "Finistère", "le Finistère", R.BRETAGNE, "Quimper", 47.9960, -4.1024,
        listOf(
            "Je me pose à Quimper, dans le Finistère, un nom qui veut dire « le bout de la terre ».",
            "Le premier évêque de la ville, saint Corentin, avait un poisson magique : chaque jour, il en mangeait la moitié, et le lendemain matin, le poisson était de nouveau entier !",
            "Sa grande cathédrale porte son nom.",
        ),
        "QUIMPER · LA CATHÉDRALE SAINT-CORENTIN", "🐟", "Quimpérois",
    ),
    Departement(
        "2A", "Corse-du-Sud", "la Corse-du-Sud", R.CORSE, "Bonifacio", 41.3874, 9.1594,
        listOf(
            "Je me pose à Bonifacio, tout au sud de la Corse. Ses maisons sont perchées au bord de hautes falaises blanches, au-dessus de la mer !",
            "Un escalier de 187 marches est taillé dans la falaise. La légende dit que des soldats du roi d'Aragon l'ont creusé en une seule nuit.",
            "Par beau temps, on aperçoit la Sardaigne : c'est déjà l'Italie !",
        ),
        "BONIFACIO · LES FALAISES", "🌊", "Bonifaciens",
    ),
    Departement(
        "2B", "Haute-Corse", "la Haute-Corse", R.CORSE, "Corte", 42.3063, 9.1495,
        listOf(
            "Je me pose à Corte, au cœur des montagnes de la Corse. Sa citadelle est perchée sur un rocher !",
            "Il y a plus de 250 ans, Pascal Paoli en avait fait la capitale de la Corse et y avait ouvert une université.",
            "Dans les montagnes tout autour vivent des mouflons, des moutons sauvages aux grandes cornes enroulées.",
        ),
        "CORTE · LA CITADELLE", "🐏", "Cortenais",
    ),
    Departement(
        "30", "Gard", "le Gard", R.OCCITANIE, "Nîmes", 43.8367, 4.3601,
        listOf(
            "Je me pose à Nîmes, une ville bâtie par les Romains. Ses arènes, vieilles de presque 2 000 ans, accueillent encore des spectacles !",
            "Le blason de la ville montre un crocodile attaché à un palmier, en souvenir de soldats romains revenus d'Égypte.",
            "Et le tissu de ton jean vient d'ici : le « denim », c'est la toile « de Nîmes » !",
        ),
        "NÎMES · LES ARÈNES", "🐊", "Nîmois",
    ),
    Departement(
        "31", "Haute-Garonne", "la Haute-Garonne", R.OCCITANIE, "Toulouse", 43.6047, 1.4442,
        listOf(
            "Je me pose à Toulouse, la ville rose, à cause de la couleur de ses briques !",
            "C'est aussi la ville des avions : on y assemble les grands Airbus. J'y ai plein de cousins !",
            "Sa basilique Saint-Sernin est l'une des plus grandes églises romanes d'Europe.",
        ),
        "TOULOUSE · LA VILLE ROSE", "✈️", "Toulousains",
    ),
    Departement(
        "32", "Gers", "le Gers", R.OCCITANIE, "Auch", 43.6465, 0.5855,
        listOf(
            "Je me pose à Auch, dans le Gers.",
            "Au milieu d'un grand escalier se dresse la statue de d'Artagnan, le plus célèbre des mousquetaires du roi. Il est né tout près d'ici, il y a environ 400 ans.",
            "Sa devise, avec ses amis : « Un pour tous, tous pour un ! »",
        ),
        "AUCH · LA STATUE DE D'ARTAGNAN", "🤺", "Auscitains",
    ),
    Departement(
        "33", "Gironde", "la Gironde", R.NOUVELLE_AQUITAINE, "Bordeaux", 44.8378, -0.5792,
        listOf(
            "Je me pose à Bordeaux, au bord de la Garonne.",
            "Devant la place de la Bourse, il y a le plus grand miroir d'eau du monde : une fine couche d'eau où se reflètent les vieux bâtiments. Les enfants adorent y courir pieds nus !",
            "Tout autour de la ville, les vignes s'étendent à perte de vue.",
        ),
        "BORDEAUX · LE MIROIR D'EAU", "🍇", "Bordelais",
    ),
    Departement(
        "34", "Hérault", "l'Hérault", R.OCCITANIE, "Saint-Guilhem-le-Désert", 43.7336, 3.5497,
        listOf(
            "Je me pose à Saint-Guilhem-le-Désert, un village caché au fond des gorges de l'Hérault.",
            "Il y a plus de 1 200 ans, Guillaume, un cousin de Charlemagne, y a fondé une abbaye pour devenir moine.",
            "Charlemagne lui avait offert un morceau de la Croix de Jésus, qui y est toujours gardé. Les pèlerins de Saint-Jacques s'y arrêtent encore.",
        ),
        "SAINT-GUILHEM · L'ABBAYE DE GELLONE", "🔔",
    ),
    Departement(
        "35", "Ille-et-Vilaine", "l'Ille-et-Vilaine", R.BRETAGNE, "Saint-Malo", 48.6493, -2.0257,
        listOf(
            "Je me pose à Saint-Malo, la cité des corsaires ! Ces marins avaient la permission du roi d'attaquer les bateaux ennemis.",
            "C'est d'ici que Jacques Cartier est parti découvrir le Canada.",
            "À marée basse, la mer se retire si loin qu'on peut aller à pied jusqu'à de petites îles.",
        ),
        "SAINT-MALO · LA CITÉ CORSAIRE", "🌊", "Malouins",
    ),
    Departement(
        "36", "Indre", "l'Indre", R.CENTRE, "Valençay", 47.1600, 1.5658,
        listOf(
            "Je me pose à Valençay, dans l'Indre. Son château a appartenu à Talleyrand, un ministre de Napoléon.",
            "On y fabrique un fromage de chèvre en forme de pyramide… sans pointe !",
            "La légende raconte que Napoléon, revenu fâché d'Égypte, lui aurait coupé la tête d'un coup d'épée.",
        ),
        "VALENÇAY · LE CHÂTEAU", "🧀", "Valencéens",
    ),
    Departement(
        "37", "Indre-et-Loire", "l'Indre-et-Loire", R.CENTRE, "Tours", 47.3941, 0.6848,
        listOf(
            "Je me pose à Tours, au bord de la Loire.",
            "Il y a très longtemps, un soldat romain, Martin, croisa un pauvre qui grelottait de froid. Avec son épée, il coupa son manteau en deux pour lui en donner la moitié !",
            "Devenu évêque de Tours, saint Martin est l'un des saints les plus aimés de France : des centaines de villages portent son nom.",
        ),
        "TOURS · LA BASILIQUE SAINT-MARTIN", "🤝", "Tourangeaux",
    ),
    Departement(
        "38", "Isère", "l'Isère", R.AUVERGNE, "La Salette", 44.8560, 5.9790,
        listOf(
            "Je me pose à La Salette, dans les montagnes de l'Isère.",
            "En 1846, deux enfants qui gardaient des vaches, Mélanie et Maximin, ont vu une belle dame en larmes : la Vierge Marie.",
            "Aujourd'hui, une grande basilique se dresse là-haut, à 1 800 mètres d'altitude, et les pèlerins y montent chaque été.",
        ),
        "LA SALETTE · LE SANCTUAIRE", "⛰️",
    ),
    Departement(
        "39", "Jura", "le Jura", R.BOURGOGNE, "Lons-le-Saunier", 46.6744, 5.5547,
        listOf(
            "Je me pose à Lons-le-Saunier, dans le Jura. C'est ici qu'est né Rouget de Lisle, qui a écrit La Marseillaise, l'hymne de la France !",
            "Toutes les heures, le carillon de la ville en joue les premières notes.",
            "Ici est aussi fabriqué un fromage qui fait sourire : La Vache qui rit.",
        ),
        "LONS-LE-SAUNIER · LA VILLE DE LA MARSEILLAISE", "🐄", "Lédoniens",
    ),
    Departement(
        "40", "Landes", "les Landes", R.NOUVELLE_AQUITAINE, "Mont-de-Marsan", 43.8902, -0.4997,
        listOf(
            "Je me pose à Mont-de-Marsan, au cœur de la forêt des Landes, la plus grande forêt plantée par les hommes en Europe.",
            "Autrefois, la région était un grand marécage.",
            "Les bergers gardaient leurs moutons perchés sur des échasses, pour voir loin et ne pas se mouiller les pieds !",
        ),
        "MONT-DE-MARSAN · LA FORÊT DES LANDES", "🌲", "Montois",
    ),
    Departement(
        "41", "Loir-et-Cher", "le Loir-et-Cher", R.CENTRE, "Chambord", 47.6161, 1.5170,
        listOf(
            "Je me pose à Chambord, le plus grand des châteaux de la Loire, construit pour le roi François Ier. Il a plus de 400 pièces !",
            "Au centre, il y a un escalier magique : deux personnes peuvent le monter en même temps sans jamais se croiser.",
            "On dit que Léonard de Vinci en a eu l'idée.",
        ),
        "CHAMBORD · L'ESCALIER À DOUBLE VIS", "🏰",
    ),
    Departement(
        "42", "Loire", "la Loire", R.AUVERGNE, "Saint-Étienne", 45.4397, 4.3872,
        listOf(
            "Je me pose à Saint-Étienne, dans la Loire. Autrefois, on y creusait des mines de charbon.",
            "C'est d'ici qu'est parti le premier train de France, il y a 200 ans : des chevaux tiraient des wagons de charbon sur des rails !",
            "Aujourd'hui, son équipe de football joue en vert : on l'appelle « les Verts ».",
        ),
        "SAINT-ÉTIENNE · LE PREMIER TRAIN DE FRANCE", "🚂", "Stéphanois",
    ),
    Departement(
        "43", "Haute-Loire", "la Haute-Loire", R.AUVERGNE, "Le Puy-en-Velay", 45.0434, 3.8858,
        listOf(
            "Je me pose au Puy-en-Velay, une ville pleine de pitons de volcans ! Sur l'un d'eux, une grande statue rouge de la Vierge Marie a été faite avec le métal de 213 canons.",
            "Sur un autre, la petite chapelle Saint-Michel est perchée tout en haut d'un rocher pointu.",
            "C'est d'ici que partent à pied de nombreux pèlerins vers Saint-Jacques-de-Compostelle, en Espagne.",
        ),
        "LE PUY-EN-VELAY · NOTRE-DAME DE FRANCE", "🌋", "Ponots",
    ),
    Departement(
        "44", "Loire-Atlantique", "la Loire-Atlantique", R.PAYS_DE_LA_LOIRE, "Nantes", 47.2184, -1.5536,
        listOf(
            "Je me pose à Nantes. Ici est né Jules Verne, qui écrivait des histoires d'aventures et de machines extraordinaires.",
            "Aujourd'hui, un éléphant géant en bois et en métal, haut de 12 mètres, se promène dans la ville avec des passagers sur son dos !",
            "Quand il en a envie, il arrose les passants avec sa trompe.",
        ),
        "NANTES · LE GRAND ÉLÉPHANT", "🐘", "Nantais",
    ),
    Departement(
        "45", "Loiret", "le Loiret", R.CENTRE, "Orléans", 47.9030, 1.9093,
        listOf(
            "Je me pose à Orléans, au bord de la Loire. En 1429, la ville était assiégée par les Anglais.",
            "Une jeune fille de 17 ans, Jeanne d'Arc, guidée par sa foi, est arrivée avec l'armée du roi et a libéré la ville !",
            "Chaque année, au mois de mai, Orléans fait la fête en son honneur. Sainte Jeanne d'Arc est une des patronnes de la France.",
        ),
        "ORLÉANS · LA VILLE DE JEANNE D'ARC", "⚜️", "Orléanais",
    ),
    Departement(
        "46", "Lot", "le Lot", R.OCCITANIE, "Rocamadour", 44.7994, 1.6178,
        listOf(
            "Je me pose à Rocamadour, un village du Lot accroché à une falaise !",
            "Les pèlerins montent un grand escalier de 216 marches pour aller prier la Vierge noire dans sa chapelle. Autrefois, beaucoup le gravissaient à genoux.",
            "On y fabrique aussi un petit fromage de chèvre tout rond : le rocamadour.",
        ),
        "ROCAMADOUR · LA CITÉ ACCROCHÉE À LA FALAISE", "⛪",
    ),
    Departement(
        "47", "Lot-et-Garonne", "le Lot-et-Garonne", R.NOUVELLE_AQUITAINE, "Agen", 44.2033, 0.6163,
        listOf(
            "Je me pose à Agen, la capitale du pruneau ! Les prunes d'ici sont séchées au four pour devenir de délicieux pruneaux.",
            "Il y a aussi un pont très étonnant : un pont-canal !",
            "Les bateaux y naviguent sur un canal… qui passe par-dessus la Garonne.",
        ),
        "AGEN · LE PONT-CANAL", "🛥️", "Agenais",
    ),
    Departement(
        "48", "Lozère", "la Lozère", R.OCCITANIE, "Florac", 44.3240, 3.5935,
        listOf(
            "Je me pose à Florac, en Lozère, dans les Cévennes.",
            "Il y a presque 150 ans, l'écrivain Robert Louis Stevenson a traversé ces montagnes à pied avec une ânesse, Modestine, qui portait ses bagages.",
            "Elle était si têtue qu'il avançait à pas de tortue ! Aujourd'hui, des randonneurs refont son chemin avec des ânes.",
        ),
        "FLORAC · LE CHEMIN DE STEVENSON", "🐴",
    ),
    Departement(
        "49", "Maine-et-Loire", "le Maine-et-Loire", R.PAYS_DE_LA_LOIRE, "Saumur", 47.2600, -0.0769,
        listOf(
            "Je me pose à Saumur, au bord de la Loire.",
            "Ici vivent les cavaliers du Cadre noir, tout habillés de noir. Ils apprennent à leurs chevaux à faire des sauts extraordinaires !",
            "Dans les caves creusées dans la pierre blanche, on fait pousser des champignons.",
        ),
        "SAUMUR · LE CADRE NOIR", "🏇", "Saumurois",
    ),
    Departement(
        "50", "Manche", "la Manche", R.NORMANDIE, "Le Mont-Saint-Michel", 48.6361, -1.5115,
        listOf(
            "Je me pose au Mont-Saint-Michel, une abbaye perchée sur un rocher au milieu de la mer !",
            "La légende raconte que l'archange saint Michel est apparu à l'évêque Aubert pour lui demander d'y construire une église.",
            "Ici, la marée monte très vite : on dit « à la vitesse d'un cheval au galop » !",
        ),
        "LE MONT-SAINT-MICHEL · L'ABBAYE", "👼",
    ),
    Departement(
        "51", "Marne", "la Marne", R.GRAND_EST, "Reims", 49.2583, 4.0317,
        listOf(
            "Je me pose à Reims. Il y a plus de 1 500 ans, l'évêque saint Remi y a baptisé Clovis, le roi des Francs.",
            "Ensuite, presque tous les rois de France sont venus se faire couronner dans sa cathédrale.",
            "Cherche sur la façade une statue célèbre : l'Ange au sourire !",
        ),
        "REIMS · LA CATHÉDRALE DES SACRES", "👑", "Rémois",
    ),
    Departement(
        "52", "Haute-Marne", "la Haute-Marne", R.GRAND_EST, "Colombey-les-Deux-Églises", 48.2225, 4.8850,
        listOf(
            "Je me pose à Colombey-les-Deux-Églises, un petit village de la Haute-Marne.",
            "Le général de Gaulle, qui a dirigé la France libre pendant la Seconde Guerre mondiale, y habitait.",
            "Sur la colline, une immense croix de Lorraine, haute de plus de 40 mètres, rappelle son souvenir.",
        ),
        "COLOMBEY · LA CROIX DE LORRAINE", "🇫🇷",
    ),
    Departement(
        "53", "Mayenne", "la Mayenne", R.PAYS_DE_LA_LOIRE, "Pontmain", 48.4406, -1.0603,
        listOf(
            "Je me pose à Pontmain, un petit village de la Mayenne.",
            "Un soir d'hiver de 1871, pendant la guerre, deux frères, Eugène et Joseph, ont vu dans le ciel étoilé une belle dame : la Vierge Marie.",
            "Elle souriait, et un message s'est écrit sous ses pieds : « Mais priez mes enfants ». Peu après, la guerre s'est arrêtée.",
        ),
        "PONTMAIN · LA BASILIQUE", "🌟",
    ),
    Departement(
        "54", "Meurthe-et-Moselle", "la Meurthe-et-Moselle", R.GRAND_EST, "Nancy", 48.6921, 6.1844,
        listOf(
            "Je me pose à Nancy. Sa place Stanislas est entourée de grilles toutes dorées : on dirait un palais !",
            "Ici, on croque des bergamotes, de petits bonbons dorés au parfum d'agrume.",
            "Il y a plus de 200 ans, deux religieuses chassées de leur couvent ont gagné leur vie en faisant des macarons : on les appelle encore « les Sœurs macarons » !",
        ),
        "NANCY · LA PLACE STANISLAS", "🍬", "Nancéiens",
    ),
    Departement(
        "55", "Meuse", "la Meuse", R.GRAND_EST, "Bar-le-Duc", 48.7728, 5.1606,
        listOf(
            "Je me pose à Bar-le-Duc, dans la Meuse. On y fabrique une confiture de groseilles très rare.",
            "Avant de cuire les fruits, des « épépineuses » retirent les pépins un par un… avec une plume d'oie !",
            "Pas loin, à Commercy, on cuit les madeleines, ces petits gâteaux en forme de coquillage.",
        ),
        "BAR-LE-DUC · LA CONFITURE À LA PLUME D'OIE", "🍒", "Barisiens",
    ),
    Departement(
        "56", "Morbihan", "le Morbihan", R.BRETAGNE, "Carnac", 47.5840, -3.0783,
        listOf(
            "Je me pose à Carnac, dans le Morbihan. Ici, des milliers de grandes pierres dressées, les menhirs, sont alignées en longues rangées dans les champs.",
            "Elles ont été posées il y a plus de 6 000 ans !",
            "Une légende raconte que saint Cornély, poursuivi par des soldats romains, les aurait changés en pierres.",
        ),
        "CARNAC · LES ALIGNEMENTS DE MENHIRS", "🗿", "Carnacois",
    ),
    Departement(
        "57", "Moselle", "la Moselle", R.GRAND_EST, "Metz", 49.1193, 6.1757,
        listOf(
            "Je me pose à Metz. Sa cathédrale a tellement de vitraux qu'on l'appelle « la lanterne du Bon Dieu » !",
            "La légende raconte qu'autrefois, un dragon terrible, le Graoully, effrayait les habitants.",
            "Saint Clément, le premier évêque de la ville, l'a chassé en le tenant attaché avec son étole.",
        ),
        "METZ · LA LANTERNE DU BON DIEU", "🐉", "Messins",
    ),
    Departement(
        "58", "Nièvre", "la Nièvre", R.BOURGOGNE, "Nevers", 46.9908, 3.1590,
        listOf(
            "Je me pose à Nevers, au bord de la Loire.",
            "Après avoir vu la Vierge Marie à Lourdes, sainte Bernadette est venue vivre ici, chez les sœurs de la Charité. Elle y a passé le reste de sa vie à prier et à soigner les malades.",
            "Des pèlerins du monde entier viennent la prier dans la chapelle du couvent.",
        ),
        "NEVERS · LE COUVENT DE SAINTE BERNADETTE", "🕊️", "Neversois",
    ),
    Departement(
        "59", "Nord", "le Nord", R.HAUTS_DE_FRANCE, "Cambrai", 50.1760, 3.2350,
        listOf(
            "Je me pose à Cambrai, dans le Nord. Ici, on fabrique des bonbons à la menthe tout rayés : les bêtises de Cambrai.",
            "On raconte qu'un apprenti confiseur s'est trompé dans sa recette… et que sa « bêtise » était délicieuse !",
            "Sur l'hôtel de ville, deux géants, Martin et Martine, frappent les heures sur une cloche.",
        ),
        "CAMBRAI · LES BÊTISES", "🍭", "Cambrésiens",
    ),
    Departement(
        "60", "Oise", "l'Oise", R.HAUTS_DE_FRANCE, "Beauvais", 49.4295, 2.0807,
        listOf(
            "Je me pose à Beauvais, dans l'Oise. Sa cathédrale Saint-Pierre a le chœur gothique le plus haut du monde : 48 mètres, autant qu'un immeuble de 16 étages !",
            "Les bâtisseurs ont voulu monter si haut que les voûtes se sont écroulées une fois.",
            "Mais ils ont tout reconstruit !",
        ),
        "BEAUVAIS · LA CATHÉDRALE SAINT-PIERRE", "⛪", "Beauvaisiens",
    ),
    Departement(
        "61", "Orne", "l'Orne", R.NORMANDIE, "Alençon", 48.4329, 0.0913,
        listOf(
            "Je me pose à Alençon, dans l'Orne. C'est ici qu'est née la petite Thérèse, qui deviendra sainte Thérèse de Lisieux, patronne de la France avec Jeanne d'Arc.",
            "Ses parents, Louis et Zélie Martin, sont aussi des saints : ce sont les premiers parents canonisés ensemble !",
            "Zélie fabriquait la dentelle d'Alençon, si fine qu'on l'appelle « la reine des dentelles ».",
        ),
        "ALENÇON · LA MAISON DE SAINTE THÉRÈSE", "🌹", "Alençonnais",
    ),
    Departement(
        "62", "Pas-de-Calais", "le Pas-de-Calais", R.HAUTS_DE_FRANCE, "Calais", 50.9513, 1.8587,
        listOf(
            "Je me pose à Calais, tout près de l'Angleterre. Par beau temps, on voit les falaises blanches de Douvres, de l'autre côté de la mer !",
            "Sous la mer, un tunnel de 50 kilomètres permet aux trains de rejoindre l'Angleterre en une demi-heure.",
            "Sur la place de la mairie, une statue montre six bourgeois qui ont autrefois sauvé leur ville.",
        ),
        "CALAIS · LE TUNNEL SOUS LA MANCHE", "🚆", "Calaisiens",
    ),
    Departement(
        "63", "Puy-de-Dôme", "le Puy-de-Dôme", R.AUVERGNE, "Clermont-Ferrand", 45.7772, 3.0870,
        listOf(
            "Je me pose à Clermont-Ferrand, au pied du puy de Dôme, un ancien volcan.",
            "C'est la ville des pneus Michelin et de leur bonhomme blanc, Bibendum !",
            "Ici est né Blaise Pascal, un savant qui a inventé l'une des premières machines à calculer quand il avait 19 ans.",
        ),
        "CLERMONT-FERRAND · LE PUY DE DÔME", "🌋", "Clermontois",
    ),
    Departement(
        "64", "Pyrénées-Atlantiques", "les Pyrénées-Atlantiques", R.NOUVELLE_AQUITAINE, "Pau", 43.2951, -0.3708,
        listOf(
            "Je me pose à Pau, face aux montagnes des Pyrénées. Dans son château est né le bon roi Henri IV.",
            "À sa naissance, son grand-père lui a frotté les lèvres avec de l'ail et du vin, pour qu'il soit fort !",
            "Son berceau était une immense carapace de tortue, qu'on peut toujours voir au château.",
        ),
        "PAU · LE CHÂTEAU D'HENRI IV", "🐢", "Palois",
    ),
    Departement(
        "65", "Hautes-Pyrénées", "les Hautes-Pyrénées", R.OCCITANIE, "Lourdes", 43.0947, -0.0459,
        listOf(
            "Je me pose à Lourdes, au pied des Pyrénées. En 1858, une jeune fille pauvre, Bernadette, a vu la Vierge Marie dix-huit fois dans une grotte.",
            "En grattant la terre, elle a fait jaillir une source.",
            "Aujourd'hui, des millions de pèlerins viennent prier à la grotte. Le soir, ils marchent en chantant, une bougie à la main.",
        ),
        "LOURDES · LA GROTTE DE MASSABIELLE", "🕯️", "Lourdais",
    ),
    Departement(
        "66", "Pyrénées-Orientales", "les Pyrénées-Orientales", R.OCCITANIE, "Font-Romeu", 42.5050, 2.0400,
        listOf(
            "Je me pose à Font-Romeu, dans les Pyrénées. Ici, un immense miroir courbé, haut comme un immeuble, concentre les rayons du soleil.",
            "C'est le plus grand four solaire du monde ! Il peut chauffer à plus de 3 000 degrés.",
            "Tout près, en hiver, on fait du ski au soleil.",
        ),
        "FONT-ROMEU · LE FOUR SOLAIRE", "☀️",
    ),
    Departement(
        "67", "Bas-Rhin", "le Bas-Rhin", R.GRAND_EST, "Strasbourg", 48.5734, 7.7521,
        listOf(
            "Je me pose à Strasbourg, en Alsace. Pendant plus de 200 ans, sa cathédrale a été le plus haut bâtiment du monde !",
            "À l'intérieur, une horloge astronomique fait défiler les apôtres devant Jésus chaque jour à midi et demi.",
            "En décembre, son marché de Noël est l'un des plus anciens et des plus beaux d'Europe.",
        ),
        "STRASBOURG · LA CATHÉDRALE NOTRE-DAME", "🎄", "Strasbourgeois",
    ),
    Departement(
        "68", "Haut-Rhin", "le Haut-Rhin", R.GRAND_EST, "Colmar", 48.0794, 7.3585,
        listOf(
            "Je me pose à Colmar, en Alsace. Ses maisons colorées se reflètent dans les canaux de la « Petite Venise ».",
            "Ici est né Auguste Bartholdi, le sculpteur de la statue de la Liberté de New York !",
            "Au printemps, regarde bien sur les toits : des cigognes y font leur nid.",
        ),
        "COLMAR · LA PETITE VENISE", "🗽", "Colmariens",
    ),
    Departement(
        "69", "Rhône", "le Rhône", R.AUVERGNE, "Lyon", 45.7640, 4.8357,
        listOf(
            "Je me pose à Lyon. Chaque 8 décembre, les Lyonnais posent des bougies à leurs fenêtres pour remercier la Vierge Marie, dont la statue dorée veille sur la ville du haut de la colline de Fourvière.",
            "C'est la fête des Lumières !",
            "Lyon est aussi la ville de Guignol, la célèbre marionnette, et celle du cinéma, inventé ici par les frères Lumière.",
        ),
        "LYON · LA FÊTE DES LUMIÈRES", "🎬", "Lyonnais",
    ),
    Departement(
        "70", "Haute-Saône", "la Haute-Saône", R.BOURGOGNE, "Ronchamp", 47.7004, 6.6365,
        listOf(
            "Je me pose à Ronchamp, en Haute-Saône. Sur la colline se dresse une chapelle pas comme les autres : Notre-Dame-du-Haut.",
            "Son toit ressemble à un grand chapeau, ou à la coque d'un bateau ! C'est l'architecte Le Corbusier qui l'a dessinée.",
            "Par ses petites fenêtres de toutes les tailles, la lumière entre comme par magie.",
        ),
        "RONCHAMP · NOTRE-DAME-DU-HAUT", "⛪",
    ),
    Departement(
        "71", "Saône-et-Loire", "la Saône-et-Loire", R.BOURGOGNE, "Cluny", 46.4344, 4.6597,
        listOf(
            "Je me pose à Cluny. Il y a plus de 1 000 ans, des moines y ont construit une abbaye immense.",
            "Pendant des siècles, son église a été la plus grande du monde chrétien, jusqu'à la construction de Saint-Pierre de Rome !",
            "Les moines de Cluny ont fondé des monastères dans toute l'Europe.",
        ),
        "CLUNY · L'ABBAYE", "🔔", "Clunisois",
    ),
    Departement(
        "72", "Sarthe", "la Sarthe", R.PAYS_DE_LA_LOIRE, "Le Mans", 48.0061, 0.1996,
        listOf(
            "Je me pose au Mans, dans la Sarthe. Chaque année, des voitures de course y roulent pendant 24 heures sans s'arrêter, jour et nuit : ce sont les 24 Heures du Mans !",
            "Les pilotes se relaient au volant.",
            "Dans la vieille ville, les rues pavées et les remparts construits par les Romains te font remonter le temps.",
        ),
        "LE MANS · LES 24 HEURES", "🏎️", "Manceaux",
    ),
    Departement(
        "73", "Savoie", "la Savoie", R.AUVERGNE, "Chambéry", 45.5646, 5.9178,
        listOf(
            "Je me pose à Chambéry, au milieu des montagnes de Savoie. Au centre de la ville, une drôle de fontaine montre quatre éléphants… dont on ne voit que l'avant !",
            "Elle rend hommage au comte de Boigne, un enfant du pays qui avait fait fortune en Inde.",
            "Autrefois, le saint suaire, un linge très précieux pour les chrétiens, était gardé dans la chapelle du château.",
        ),
        "CHAMBÉRY · LA FONTAINE DES ÉLÉPHANTS", "⛲", "Chambériens",
    ),
    Departement(
        "74", "Haute-Savoie", "la Haute-Savoie", R.AUVERGNE, "Chamonix", 45.9237, 6.8694,
        listOf(
            "Je me pose à Chamonix, au pied du mont Blanc, la plus haute montagne d'Europe de l'Ouest : plus de 4 800 mètres !",
            "Deux hommes de la vallée, Jacques Balmat et Michel Paccard, en ont atteint le sommet pour la première fois en 1786.",
            "En 1924, Chamonix a accueilli les tout premiers Jeux olympiques d'hiver.",
        ),
        "CHAMONIX · LE MONT BLANC", "⛷️", "Chamoniards",
    ),
    Departement(
        "75", "Paris", "Paris", R.ILE_DE_FRANCE, "Paris", 48.8566, 2.3522,
        listOf(
            "Je me pose à Paris, la capitale de la France. La tour Eiffel a été construite pour une grande exposition en 1889. Tous les sept ans, on la repeint entièrement, à la main !",
            "Sur l'île de la Cité, la cathédrale Notre-Dame a brûlé en 2019.",
            "Des centaines d'artisans l'ont reconstruite en cinq ans, et elle a rouvert ses portes en décembre 2024.",
        ),
        "PARIS · LA TOUR EIFFEL ET NOTRE-DAME", "🗼", "Parisiens",
    ),
    Departement(
        "76", "Seine-Maritime", "la Seine-Maritime", R.NORMANDIE, "Étretat", 49.7070, 0.2057,
        listOf(
            "Je me pose à Étretat, au bord de la Manche. Ici, la mer a creusé de grandes arches dans les hautes falaises blanches, comme des trompes d'éléphant !",
            "Devant elles se dresse un rocher pointu : l'Aiguille.",
            "Dans les romans, le gentleman cambrioleur Arsène Lupin y cachait ses trésors.",
        ),
        "ÉTRETAT · LES FALAISES ET L'AIGUILLE", "🎩", "Étretatais",
    ),
    Departement(
        "77", "Seine-et-Marne", "la Seine-et-Marne", R.ILE_DE_FRANCE, "Fontainebleau", 48.4047, 2.7016,
        listOf(
            "Je me pose à Fontainebleau. Pendant 800 ans, les rois de France ont habité son château, au milieu d'une immense forêt.",
            "C'est dans sa cour que Napoléon a fait ses adieux à ses soldats, avant de partir en exil.",
            "Dans la forêt, petits et grands s'amusent à escalader de gros rochers.",
        ),
        "FONTAINEBLEAU · LE CHÂTEAU DES ROIS", "🦌", "Bellifontains",
    ),
    Departement(
        "78", "Yvelines", "les Yvelines", R.ILE_DE_FRANCE, "Versailles", 48.8049, 2.1204,
        listOf(
            "Je me pose à Versailles. Le roi Louis XIV, qu'on appelait le Roi-Soleil, y a fait construire un château magnifique.",
            "Dans la galerie des Glaces, 357 miroirs reflètent la lumière des fenêtres.",
            "Dans les jardins, les fontaines dansent en musique !",
        ),
        "VERSAILLES · LA GALERIE DES GLACES", "🌞", "Versaillais",
    ),
    Departement(
        "79", "Deux-Sèvres", "les Deux-Sèvres", R.NOUVELLE_AQUITAINE, "Coulon", 46.3225, -0.5856,
        listOf(
            "Je me pose à Coulon, dans le Marais poitevin, qu'on appelle « la Venise verte ». Ici, on se promène en barque sur des canaux couverts de petites lentilles d'eau.",
            "Le batelier pousse la barque avec une longue perche.",
            "S'il remue la vase du fond et approche une flamme, des bulles de gaz s'enflamment : l'eau prend feu !",
        ),
        "COULON · LA VENISE VERTE", "🛶", "Coulonnais",
    ),
    Departement(
        "80", "Somme", "la Somme", R.HAUTS_DE_FRANCE, "Amiens", 49.8941, 2.2958,
        listOf(
            "Je me pose à Amiens. Sa cathédrale Notre-Dame est la plus grande de France : on pourrait y faire entrer deux fois Notre-Dame de Paris !",
            "C'est aux portes de cette ville que le soldat Martin a partagé son manteau avec un pauvre.",
            "Autour de la ville, des jardiniers cultivent leurs légumes sur de petites îles, les hortillonnages, et s'y rendent en barque.",
        ),
        "AMIENS · LA CATHÉDRALE NOTRE-DAME", "⛪", "Amiénois",
    ),
    Departement(
        "81", "Tarn", "le Tarn", R.OCCITANIE, "Albi", 43.9289, 2.1464,
        listOf(
            "Je me pose à Albi, une ville toute en briques rouges.",
            "Sa cathédrale Sainte-Cécile ressemble à un château fort : c'est la plus grande cathédrale en briques du monde !",
            "À l'intérieur, les voûtes sont peintes en bleu et semées d'étoiles d'or.",
        ),
        "ALBI · LA CATHÉDRALE SAINTE-CÉCILE", "⛪", "Albigeois",
    ),
    Departement(
        "82", "Tarn-et-Garonne", "le Tarn-et-Garonne", R.OCCITANIE, "Moissac", 44.1050, 1.0850,
        listOf(
            "Je me pose à Moissac, sur le chemin de Saint-Jacques-de-Compostelle.",
            "Son abbaye abrite un cloître de plus de 900 ans, avec 76 arcades. Sur leurs chapiteaux, des sculptures racontent des histoires de la Bible.",
            "On cultive ici un raisin doré et sucré : le chasselas.",
        ),
        "MOISSAC · LE CLOÎTRE", "🍇", "Moissagais",
    ),
    Departement(
        "83", "Var", "le Var", R.PROVENCE, "Saint-Maximin-la-Sainte-Baume", 43.4528, 5.8619,
        listOf(
            "Je me pose à Saint-Maximin, en Provence.",
            "Une tradition raconte que sainte Marie-Madeleine, l'amie de Jésus, est arrivée en Provence par la mer. Elle aurait prié pendant 30 ans dans une grotte de la montagne voisine, la Sainte-Baume.",
            "Ses reliques sont gardées dans la grande basilique de la ville.",
        ),
        "SAINT-MAXIMIN · LA BASILIQUE", "🙏", "Maximinois",
    ),
    Departement(
        "84", "Vaucluse", "le Vaucluse", R.PROVENCE, "Avignon", 43.9493, 4.8055,
        listOf(
            "Je me pose à Avignon. Tu connais la chanson « Sur le pont d'Avignon » ? Le pont existe vraiment, mais il s'arrête au milieu du Rhône !",
            "La légende raconte qu'un jeune berger, Bénézet, a reçu du Ciel l'ordre de le construire.",
            "Il y a 700 ans, les papes habitaient ici, dans un immense palais.",
        ),
        "AVIGNON · LE PONT ET LE PALAIS DES PAPES", "🎶", "Avignonnais",
    ),
    Departement(
        "85", "Vendée", "la Vendée", R.PAYS_DE_LA_LOIRE, "Les Sables-d'Olonne", 46.4967, -1.7831,
        listOf(
            "Je me pose aux Sables-d'Olonne, en Vendée, au bord de l'océan.",
            "Tous les quatre ans, des marins en partent pour le Vendée Globe : le tour du monde à la voile, tout seul et sans escale !",
            "Il leur faut deux ou trois mois pour revenir, accueillis par une foule immense.",
        ),
        "LES SABLES-D'OLONNE · LE VENDÉE GLOBE", "⛵", "Sablais",
    ),
    Departement(
        "86", "Vienne", "la Vienne", R.NOUVELLE_AQUITAINE, "Poitiers", 46.5802, 0.3404,
        listOf(
            "Je me pose à Poitiers. Les soirs d'été, la façade de l'église Notre-Dame-la-Grande s'illumine de toutes les couleurs, comme au Moyen Âge, quand ses sculptures étaient peintes !",
            "Tout près, à Ligugé, saint Martin a fondé il y a plus de 1 600 ans le plus ancien monastère d'Occident.",
            "Les moines y prient toujours.",
        ),
        "POITIERS · NOTRE-DAME-LA-GRANDE", "🌈", "Poitevins",
    ),
    Departement(
        "87", "Haute-Vienne", "la Haute-Vienne", R.NOUVELLE_AQUITAINE, "Limoges", 45.8336, 1.2611,
        listOf(
            "Je me pose à Limoges, la capitale de la porcelaine. Il y a plus de 250 ans, on a découvert tout près une terre blanche très rare, le kaolin.",
            "Avec elle, on fabrique des assiettes et des tasses si fines que la lumière passe à travers !",
            "Tous les sept ans, les habitants portent en procession dans les rues les reliques de leurs saints : ce sont les ostensions.",
        ),
        "LIMOGES · LA PORCELAINE", "☕", "Limougeauds",
    ),
    Departement(
        "88", "Vosges", "les Vosges", R.GRAND_EST, "Épinal", 48.1724, 6.4496,
        listOf(
            "Je me pose à Épinal, dans les Vosges.",
            "Depuis plus de 200 ans, on y imprime des images aux couleurs vives : les images d'Épinal !",
            "Autrefois, des marchands les vendaient de village en village. Elles racontaient des contes, des vies de saints ou des histoires de soldats.",
        ),
        "ÉPINAL · LES IMAGES D'ÉPINAL", "🖼️", "Spinaliens",
    ),
    Departement(
        "89", "Yonne", "l'Yonne", R.BOURGOGNE, "Treigny", 47.5839, 3.1564,
        listOf(
            "Je me pose à Treigny, dans l'Yonne, près du chantier de Guédelon. Ici, des ouvriers construisent un vrai château fort… aujourd'hui !",
            "Ils travaillent comme au Moyen Âge : sans machines, avec des pierres, du bois, et des chevaux pour tirer les charrettes.",
            "Le chantier a commencé en 1997, et il continue encore.",
        ),
        "GUÉDELON · LE CHÂTEAU EN CONSTRUCTION", "🏗️",
    ),
    Departement(
        "90", "Territoire de Belfort", "le Territoire de Belfort", R.BOURGOGNE, "Belfort", 47.6380, 6.8628,
        listOf(
            "Je me pose à Belfort. Au pied de la citadelle, un lion géant est sculpté dans la falaise de grès rouge : il mesure 22 mètres de long !",
            "Il rappelle le courage des habitants, qui ont résisté à un long siège en 1870.",
            "Son sculpteur, Bartholdi, a aussi créé la statue de la Liberté.",
        ),
        "BELFORT · LE LION", "🦁", "Belfortains",
    ),
    Departement(
        "91", "Essonne", "l'Essonne", R.ILE_DE_FRANCE, "Évry", 48.6245, 2.4300,
        listOf(
            "Je me pose à Évry, dans l'Essonne.",
            "Sa cathédrale de la Résurrection est la plus jeune de France : elle a été construite il y a 30 ans seulement !",
            "Elle est toute ronde, en briques, et regarde bien son toit : des arbres y poussent !",
        ),
        "ÉVRY · LA CATHÉDRALE AUX ARBRES", "🌳", "Évryens",
    ),
    Departement(
        "92", "Hauts-de-Seine", "les Hauts-de-Seine", R.ILE_DE_FRANCE, "Sèvres", 48.8239, 2.2117,
        listOf(
            "Je me pose à Sèvres, tout près de Paris. Ici, on garde le mètre étalon et le kilogramme étalon.",
            "Pendant longtemps, toutes les règles et toutes les balances du monde ont été vérifiées grâce à eux !",
            "On y fabrique aussi une porcelaine très fine, offerte par la France aux grands visiteurs.",
        ),
        "SÈVRES · LE MÈTRE ÉTALON", "📏", "Sévriens",
    ),
    Departement(
        "93", "Seine-Saint-Denis", "la Seine-Saint-Denis", R.ILE_DE_FRANCE, "Saint-Denis", 48.9362, 2.3574,
        listOf(
            "Je me pose à Saint-Denis. Dans sa basilique reposent presque tous les rois et les reines de France.",
            "Elle porte le nom de saint Denis, le premier évêque de Paris.",
            "Tout près, le Stade de France a accueilli les Jeux olympiques de 2024.",
        ),
        "SAINT-DENIS · LA BASILIQUE DES ROIS", "👑", "Dionysiens",
    ),
    Departement(
        "94", "Val-de-Marne", "le Val-de-Marne", R.ILE_DE_FRANCE, "Vincennes", 48.8474, 2.4390,
        listOf(
            "Je me pose à Vincennes. Son château a l'un des plus hauts donjons d'Europe : 52 mètres !",
            "Il y a près de 800 ans, le roi Saint Louis aimait s'asseoir sous un grand chêne, dans le bois voisin.",
            "Là, il écoutait les gens venus lui demander de régler leurs disputes, et il rendait la justice.",
        ),
        "VINCENNES · LE DONJON ET LE CHÊNE DE SAINT LOUIS", "⚖️", "Vincennois",
    ),
    Departement(
        "95", "Val-d'Oise", "le Val-d'Oise", R.ILE_DE_FRANCE, "Auvers-sur-Oise", 49.0717, 2.1700,
        listOf(
            "Je me pose à Auvers-sur-Oise. Le peintre Vincent van Gogh y a passé les deux derniers mois de sa vie.",
            "Il peignait sans arrêt : plus de 70 tableaux en 70 jours !",
            "Tu peux voir l'église du village, les champs de blé et l'auberge qu'il a peints.",
        ),
        "AUVERS-SUR-OISE · LE VILLAGE DE VAN GOGH", "🌻", "Auversois",
    ),
)

private val PAR_CODE: Map<String, Departement> by lazy { DEPARTEMENTS.associateBy { it.code } }

fun departement(code: String): Departement? = PAR_CODE[code]
