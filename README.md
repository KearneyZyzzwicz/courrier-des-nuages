# Le Courrier des Nuages

Application Android d'aide aux devoirs pour une famille : dictée de mots, tables d'addition et de
multiplication, mots d'italien. Chaque enfant a son profil, son avatar et son code secret.

- Le code de l'application est public ; les données de la famille sont dans une base privée,
  protégée par un compte familial. Aucune donnée personnelle n'est enregistrée dans ce dépôt.
- Chaque modification envoyée sur `main` fabrique automatiquement une nouvelle version
  (GitHub Actions) publiée dans les **Releases**. L'application se met à jour toute seule.

## Installer sur un téléphone

1. Sur le téléphone, ouvrir la dernière version dans **Releases** et télécharger
   `courrier-des-nuages.apk`.
2. Ouvrir le fichier et autoriser l'installation d'applications depuis le navigateur.
3. Au premier lancement, relier le téléphone au compte de la famille.

## Crédits

- Polices [Fredoka](https://fonts.google.com/specimen/Fredoka) et [Andika](https://software.sil.org/andika/)
  (SIL Open Font License, voir `app/src/main/assets/licences`).
- Illustrations (avatars, pigeon, avion, cartes postales) créées pour le projet.
- Carte de l'Italie : limites des régions de l'[ISTAT](https://www.istat.it/) (licence CC BY),
  via [openpolis/geojson-italy](https://github.com/openpolis/geojson-italy), simplifiées pour l'appli.
