Tu travailles sur le projet Node26 dans `{{PROJECT_PATH}}`, pendant le cycle autonome {{CYCLE_NUMBER}}.

Le rapport d'analyse comportementale à examiner est `{{REPORT_PATH}}`.

Objectif :

1. Lire entièrement le rapport et vérifier chaque préconisation dans le code et les événements cités.
2. Identifier une seule cause racine importante et reproductible.
3. Appliquer la correction minimale qui améliore le comportement des péons.
4. Ajouter ou adapter un test déterministe reproduisant le défaut.
5. Exécuter les tests pertinents.
6. Terminer par un résumé des constats, fichiers modifiés, tests et risques restants.

Contraintes impératives :

- La branche courante doit rester `analyse`. N'exécute jamais `git switch`, `git checkout`, `git branch`, `git reset`, `git clean`, `git commit`, `git merge`, `git rebase` ou `git push`.
- Respecte intégralement `AGENTS.md` et l'architecture hexagonale.
- Ne modifie jamais `.env`, `docker-compose.yml`, `Dockerfile`, le superviseur automatique, son prompt ou les rapports.
- Ne redéploie rien et ne lance pas une nouvelle simulation : le superviseur Java s'en charge après validation.
- Ne suppose pas que les recommandations du rapport sont justes. Confirme-les dans le code avant toute modification.
- Ne corrige pas plusieurs problèmes indépendants pendant le même cycle.
- Préserve les modifications préexistantes de l'utilisateur.
- Si le défaut n'est pas démontrable ou qu'aucune correction sûre n'est possible, ne modifie aucun fichier et explique précisément pourquoi.
