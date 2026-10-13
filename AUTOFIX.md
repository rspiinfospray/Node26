# Superviseur autonome de correction comportementale

Le superviseur est un programme Java indépendant du backend. Tout son code est isolé sous le package `org.infospray.peonsimulator.supervisor`. Il surveille les rapports Markdown créés dans `reports`, demande à Codex de corriger un seul défaut, valide le projet, redéploie le backend et relance une simulation construite à partir du dernier monde terminé.

## Séquence d'un cycle

1. Sélection du rapport terminé le plus récent et encore non traité.
2. Vérification que la branche Git courante est exactement `analyse`.
3. Exécution non interactive de `codex exec` avec le sandbox `workspace-write`.
4. Vérification obligatoire d'un compte rendu Codex non vide et d'au moins une modification dans `src/main/java` ou `src/test/java`.
5. Nouvelle vérification de la branche.
6. Exécution de `docker compose build backend`, qui compile et lance les tests Maven.
7. Redéploiement par `docker compose up -d backend`.
8. Attente de la disponibilité de l'API.
9. Création et démarrage d'un monde utilisant la même configuration et la même graine que le dernier monde terminé.

Le superviseur ne crée, ne change et ne fusionne aucune branche. Toute divergence par rapport à `analyse` arrête immédiatement le cycle.

## Prérequis

- Docker Desktop est démarré.
- Le backend Node26 et ses dépendances sont accessibles par Docker Compose.
- Codex CLI est installé et authentifié. Une authentification locale existante est réutilisée automatiquement par `codex exec`.
- Java et Maven sont disponibles. Le script cherche également le Maven intégré à IntelliJ IDEA.

La clé OpenAI utilisée par le backend reste dans `.env`. Le superviseur ne lit pas ce fichier et ne transmet pas son contenu à Codex. Pour une authentification Codex par clé plutôt que par session locale, définir `CODEX_API_KEY` dans l'environnement du processus qui lance le superviseur.

## Démarrage

Depuis PowerShell, à la racine du projet :

```powershell
.\run-autofix.cmd
```

Le processus reste actif et vérifie les nouveaux rapports toutes les 15 secondes. Pour effectuer une seule vérification puis quitter :

```powershell
.\run-autofix.cmd -Once
```

## Configuration

Les variables suivantes sont facultatives :

| Variable | Valeur par défaut | Fonction |
|---|---:|---|
| `AUTOFIX_PROJECT_DIRECTORY` | `.` | Racine du dépôt |
| `AUTOFIX_REPORT_DIRECTORY` | `reports` | Répertoire surveillé |
| `AUTOFIX_REQUIRED_BRANCH` | `analyse` | Branche obligatoire |
| `AUTOFIX_MAXIMUM_CYCLES` | `3` | Nombre maximal de corrections |
| `AUTOFIX_POLL_SECONDS` | `15` | Fréquence de surveillance |
| `AUTOFIX_API_BASE_URL` | `http://localhost:8080` | URL du backend |
| `AUTOFIX_RUN_ONCE` | `false` | Quitter après une vérification |

Les marqueurs de succès et d'échec sont enregistrés dans `reports/autofix-state`. Les réponses finales de Codex sont enregistrées dans `reports/codex-results`.

## Arrêt et erreurs

Le superviseur s'arrête sans redéployer si Codex échoue, si la branche change, si la compilation ou les tests échouent, si Docker échoue ou si la nouvelle simulation ne peut pas être lancée. Un rapport en échec reçoit un marqueur `.failed` et n'est pas retraité automatiquement.
