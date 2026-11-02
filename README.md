# Peon Simulator — Illuvatar

Simulateur de vie autonome au tour par tour sur une carte hexagonale. Des équipes de peons explorent leur environnement, survivent, apprennent de leurs actions, coopèrent ou s'affrontent et développent des stratégies à long terme.

Le projet repose sur une architecture hexagonale :

- Spring Boot orchestre les cas d'usage et expose l'API REST ;
- une interface web PixiJS permet de créer, observer et rejouer les mondes ;
- H2 ou PostgreSQL conserve les mondes, leurs snapshots et les événements ;
- un bus local ou Kafka transporte les demandes d'action et les événements du domaine.

La description détaillée du domaine et des choix techniques se trouve dans [ARCHITECTURE.md](ARCHITECTURE.md).

## Fonctionnalités

- génération reproductible de cartes hexagonales avec roches, arbres, nourriture, moutons et rivière optionnelle ;
- simulation automatique, pause, avance pas à pas et vitesse réglable ;
- perception locale et carte mentale propre à chaque peon ;
- besoins de survie, personnalités, relations, apprentissage et planification stratégique ;
- collecte, agriculture, échanges, combat et construction de maisons, champs, greniers et ponts ;
- journal d'événements, snapshots historiques et diffusion en temps réel par SSE ;
- diagnostics par peon, relecture d'une décision et analyse synthétique d'un monde.

## Prérequis

Pour le lancement complet :

- Docker avec Docker Compose.

Pour le lancement local :

- Java 21 ;
- Maven 3.9 ou une version compatible.

## Lancement complet avec Docker

```bash
docker compose up --build
```

Ouvrir ensuite <http://localhost:8080>.

Docker Compose démarre PostgreSQL 16, Kafka et l'application. Les données PostgreSQL sont conservées dans le volume `peon-postgres`.

Pour arrêter les services :

```bash
docker compose down
```

## Lancement local sans Kafka

```bash
mvn spring-boot:run
```

Par défaut, l'application utilise une base H2 persistante dans `./data` et un bus d'événements local. Aucun service externe n'est nécessaire.

Pour exécuter les tests :

```bash
mvn test
```

## Lancement local avec Kafka

Démarrer Kafka, puis définir les variables suivantes avant de lancer l'application.

Sous Bash :

```bash
KAFKA_ENABLED=true KAFKA_BOOTSTRAP_SERVERS=localhost:9092 mvn spring-boot:run
```

Sous PowerShell :

```powershell
$env:KAFKA_ENABLED = "true"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
mvn spring-boot:run
```

## Créer un monde par l'API

Seul le tableau `teams` est obligatoire. Les autres propriétés utilisent les valeurs par défaut de l'application lorsqu'elles sont omises.

```bash
curl -X POST http://localhost:8080/api/worlds \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Terre du Milieu",
    "width": 50,
    "height": 50,
    "seed": 42,
    "generateRiver": true,
    "teams": [
      { "name": "Humains", "peonCount": 10 },
      { "name": "Orcs", "peonCount": 10 }
    ]
  }'
```

Les principaux paramètres optionnels sont `rockPercentage`, `treePercentage`, `foodCellPercentage`, `minFoodPerCell`, `maxFoodPerCell`, `sheepCount`, `hungerHealthLossPerTurn` et `maxRounds`.

## API REST

### Mondes et simulation

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/worlds` | Crée un monde |
| `GET` | `/api/worlds` | Liste les mondes |
| `GET` | `/api/worlds/summary` | Liste les mondes sans leurs données détaillées |
| `DELETE` | `/api/worlds` | Supprime tous les mondes |
| `GET` | `/api/worlds/{worldId}` | Retourne l'état courant ou le snapshot indiqué par `?sequence={sequenceNumber}` |
| `POST` | `/api/worlds/{worldId}/start` | Démarre ou reprend la simulation |
| `POST` | `/api/worlds/{worldId}/pause` | Met la simulation en pause |
| `POST` | `/api/worlds/{worldId}/step` | Joue le prochain tour |
| `GET` | `/api/worlds/{worldId}/peons` | Liste les peons avec leur statut et leur séquence de décès éventuelle |
| `GET` | `/api/worlds/{worldId}/events` | Consulte les événements filtrables |
| `GET` | `/api/worlds/{worldId}/stream` | Reçoit les événements en Server-Sent Events |
| `GET` | `/api/simulation/speed` | Consulte la vitesse de simulation |
| `POST` | `/api/simulation/speed?multiplier={multiplier}` | Modifie la vitesse de simulation |
| `GET` | `/api/config` | Consulte les valeurs de jeu configurées |

L'endpoint des événements accepte les paramètres `after`, `limit`, `peonId`, `fromRound`, `toRound` et `types`.

### Obtenir le contexte compact d'un monde

`analysis-context` fournit les informations générales nécessaires à une analyse externe sans retourner les cellules, les cartes mentales ni l'état détaillé des peons :

```powershell
curl.exe "http://localhost:8080/api/worlds/{worldId}/analysis-context"
```

Exemple de réponse :

```json
{
  "id": "ef6f5bf8-5c3c-46b5-929c-29b7cc60c21f",
  "name": "Monde de contrôle",
  "status": "FINISHED",
  "dimensions": {
    "width": 20,
    "height": 20
  },
  "generation": {
    "seed": 42,
    "rockPercentage": 10.0,
    "treePercentage": 15.0,
    "foodCellPercentage": 15.0,
    "initialSheep": 10,
    "riverGenerationEnabled": true
  },
  "simulation": {
    "currentRound": 100,
    "maxRounds": 100,
    "sequenceNumber": 1131
  },
  "population": {
    "totalPeons": 16,
    "alivePeons": 2,
    "deadPeons": 14
  },
  "constructions": {
    "houses": 5,
    "granaries": 3,
    "fields": 6
  },
  "remainingSheep": 0
}
```

Cet endpoint complète les API existantes sans les remplacer. Il est adapté à la construction d'un prompt compact, contrairement à `GET /api/worlds/{worldId}` qui retourne l'intégralité du monde.

### Diagnostic et analyse

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/worlds/{worldId}/analysis` | Analyse synthétique du monde |
| `GET` | `/api/worlds/{worldId}/analysis-context` | Contexte compact du monde destiné à l'analyse externe |
| `GET` | `/api/worlds/{worldId}/peons/{peonId}/diagnostic` | Diagnostic détaillé d'un peon |
| `GET` | `/api/worlds/{worldId}/peons/{peonId}/death-context?sequenceLimit=20` | Contexte compact des dernières séquences liées au peon avant son décès |
| `GET` | `/api/worlds/{worldId}/peons/{peonId}/diagnostic/summary` | Synthèse du diagnostic |
| `GET` | `/api/worlds/{worldId}/peons/{peonId}/surroundings?sequence={sequenceNumber}&radius=6` | Environnement visible à une séquence donnée |
| `GET` | `/api/worlds/{worldId}/peons/{peonId}/mental-map` | Carte mentale courante ou historique |
| `POST` | `/api/worlds/{worldId}/peons/{peonId}/replay-decision?sequence={sequenceNumber}` | Rejoue une décision sans modifier le monde |
| `GET` | `/api/worlds/diagnostics/peon?world=...&peon=...` | Recherche un diagnostic par noms |

Les diagnostics acceptent également `fromRound` et `toRound` pour limiter la période étudiée.

### Lister les peons d'un monde

L'endpoint léger suivant retourne l'identifiant, le nom et le statut de chaque peon sans sérialiser son état complet :

```powershell
curl.exe "http://localhost:8080/api/worlds/{worldId}/peons"
```

Exemple de réponse :

```json
[
  {
    "id": "3fe12ca2-42c6-4d89-86e1-ea7c786268e5",
    "name": "Sylmar",
    "status": "MORT",
    "deathSequence": 658
  },
  {
    "id": "30dcaa0b-7260-46dd-81ec-a298f785dd98",
    "name": "Valandil",
    "status": "VIVANT",
    "deathSequence": null
  }
]
```

`deathSequence` vaut `null` pour un peon vivant.

### Analyser le contexte d'un décès

`death-context` produit un diagnostic compact destiné notamment à une analyse par une IA :

```powershell
curl.exe "http://localhost:8080/api/worlds/{worldId}/peons/{peonId}/death-context?sequenceLimit=20"
```

Le paramètre `sequenceLimit` définit le nombre maximal de séquences distinctes retournées. Sa valeur par défaut est `20` et les valeurs autorisées vont de `1` à `100`. Une séquence est retenue lorsque le peon agit, subit un effet ou est la cible d'une action. Il ne s'agit donc pas des dernières séquences globales du monde.

La section `window` permet de vérifier la période sélectionnée :

```json
{
  "fromSequence": 369,
  "toSequence": 658,
  "deathSequence": 658,
  "deathRound": 42,
  "sequenceCount": 20,
  "peonActionCount": 19,
  "sequences": [369, 385, 401, 417, 433, 449, 465, 481, 497, 513, 529, 545, 561, 576, 590, 604, 618, 632, 646, 658]
}
```

La réponse contient uniquement les données utiles à l'étude du décès :

- l'identité du peon ;
- la cause, la position et l'éventuel attaquant ;
- les variations de points de vie ;
- les décisions compactées avec l'état connu avant l'action et son résultat ;
- la meilleure alternative rejetée, sans la liste volumineuse de tous les candidats ;
- la stratégie à long terme active au moment de chaque décision ;
- un résumé agrégé de la vie du peon (`lifetimeSummary`) ;
- le contexte tactique de l'attaque fatale lorsqu'il existe (`fatalAttackContext`) ;
- les règles de dégâts pertinentes.

`lifetimeSummary` regroupe notamment les actions par type, les pertes de vie par cause, les échecs, les repas, les attaques et les communications. `fatalAttackContext` décrit les caractéristiques utiles de l'attaquant, les dégâts réels, les PV de la victime avant et après l'attaque et indique si l'attaquant figurait dans sa carte mentale. Ces blocs restent synthétiques afin de limiter le coût en tokens.

Pour une mort provoquée par un autre peon, l'attaque est incluse même si l'événement d'origine est associé à l'attaquant. Un peon vivant ne possède pas de contexte de décès et provoque une réponse HTTP `400`.

### Consulter un snapshot historique

Le paramètre `sequence` attend un numéro de séquence réel, par exemple :

```powershell
curl.exe "http://localhost:8080/api/worlds/{worldId}?sequence=500"
```

Sans ce paramètre, l'API retourne l'état courant. Une séquence représente une étape du journal d'événements et non un numéro de tour ; plusieurs séquences peuvent appartenir au même tour.

## Configuration

Les principales variables d'environnement sont :

| Variable | Valeur par défaut | Rôle |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Port HTTP |
| `DATABASE_URL` | base H2 dans `./data/peons` | URL JDBC |
| `DATABASE_USERNAME` | `sa` | Utilisateur de la base |
| `DATABASE_PASSWORD` | vide | Mot de passe de la base |
| `KAFKA_ENABLED` | `false` | Active Kafka à la place du bus local |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Adresse du broker Kafka |
| `SIMULATION_DELAY_MS` | `1000` | Délai de base entre deux cycles |

Les règles du monde, des peons, des actions et du moteur de décision sont configurables dans [`src/main/resources/application.properties`](src/main/resources/application.properties) sous le préfixe `simulator.game`.

## Topics Kafka

- `peon-simulator.action-requests.v1` transporte les demandes d'action ;
- `peon-simulator.domain-events.v1` transporte les événements du domaine.

Les messages sont partitionnés par `worldId`. Illuvatar déduplique les demandes grâce à leur `actionId`.

## Structure du code

```text
src/main/java/org/infospray/peonsimulator
├── domain                  # Modèle et événements métier
├── application             # Cas d'usage, commandes, DTO et ports
├── adapter/primary         # REST, SSE, Kafka entrant et planificateur
├── adapter/secondary       # Persistance, décision, horloge et Kafka sortant
└── configuration           # Configuration Spring et valeurs du jeu
```

Le frontend est servi depuis `src/main/resources/static` et le schéma SQL se trouve dans `src/main/resources/schema.sql`.
