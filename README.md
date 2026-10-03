# Peon Simulator — Illuvatar

Simulateur de vie hexagonal au tour par tour. Le backend Spring Boot orchestre les décisions autonomes des peons, Kafka transporte les demandes d'action et leurs événements, PostgreSQL conserve les mondes et leurs snapshots, et une interface Canvas permet de suivre ou rejouer la simulation.

La description fonctionnelle et les décisions détaillées se trouvent dans [ARCHITECTURE.md](ARCHITECTURE.md).

## Lancement complet

Prérequis : Docker avec Compose.

```bash
docker compose up --build
```

Ouvrir ensuite <http://localhost:8080>.

Ce mode démarre PostgreSQL, Kafka et l'application avec `KAFKA_ENABLED=true`.

## Lancement de développement sans Kafka

Avec Java 21 et Maven installés :

```bash
mvn spring-boot:run
```

Le profil par défaut utilise une base H2 locale dans `./data` et un bus d'événements local. Les mêmes publishers/listeners applicatifs sont utilisés, mais aucun broker n'est requis. Pour activer Kafka :

```bash
KAFKA_ENABLED=true KAFKA_BOOTSTRAP_SERVERS=localhost:9092 mvn spring-boot:run
```

## API principale

- `POST /api/worlds` : créer un monde ;
- `POST /api/worlds/{id}/start` et `/pause` : contrôler la lecture ;
- `POST /api/worlds/{id}/step` : jouer le tour d'un peon ;
- `GET /api/worlds/{id}?sequence=N` : consulter un snapshot historique ;
- `GET /api/worlds/{id}/events` : lire le journal ;
- `GET /api/worlds/{id}/stream` : recevoir les événements SSE.

## Topics Kafka

- `peon-simulator.action-requests.v1`
- `peon-simulator.domain-events.v1`

Les messages sont partitionnés par `worldId`. Illuvatar déduplique chaque demande grâce à son `actionId`.
