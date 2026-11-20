package fr.courrierdesnuages.app.data

/** Les noms des collections de la base, partagés entre la base et la logique de l'appli. */
const val REVISIONS_FRANCAIS = "revisions"
const val REVISIONS_MATHS = "revisionsMaths"
const val REVISIONS_ITALIEN = "revisionsItalien"
const val REVISIONS_ANGLAIS = "revisionsAnglais"

/** Les cartes gagnées de chaque album : Italie, tour du monde anglophone, départements de France. */
const val CARTES_ITALIE = "cartes"
const val CARTES_MONDE = "cartesMonde"
const val CARTES_FRANCE = "cartesFrance"
val COLLECTIONS_DE_CARTES = listOf(CARTES_ITALIE, CARTES_MONDE, CARTES_FRANCE)
