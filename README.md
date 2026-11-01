# AtlantykaChatBubbles

Plugin indépendant de bulles de discussion pour **Paper 1.21.8 / Java 21**.

## Fonctionnalités
- Affiche les messages au-dessus de la tête du joueur.
- Les bulles suivent le joueur.
- Empile jusqu'à trois messages par joueur.
- Supprime les bulles après la durée configurée.
- Commande `/holochat reload`.

## Compilation
Le dépôt utilise GitHub Actions. À chaque push sur `main`, GitHub compile le plugin avec Java 21 et publie le JAR comme artefact du workflow.

Compilation locale : `mvn clean package`.

## Installation
Arrêter Paper, placer le JAR dans `plugins/`, puis redémarrer le serveur.
