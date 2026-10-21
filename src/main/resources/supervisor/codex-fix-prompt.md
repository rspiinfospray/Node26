Tu travailles sur le projet Node26 dans `{{PROJECT_PATH}}`, pendant le cycle autonome {{CYCLE_NUMBER}}.

Le rapport d'analyse comportementale à examiner est `{{REPORT_PATH}}`.

Objectif :

1. Lire entièrement le rapport et vérifier chaque préconisation dans le code et les événements cités.
2. Recenser les préconisations formulées pour chaque péon et regrouper celles qui recommandent la même évolution ou qui découlent d'une même cause fonctionnelle.
3. Classer les groupes de recommandations par priorité en tenant compte du nombre de péons concernés, de la gravité pour leur survie ou leur cohérence, de la fréquence observée et de la confiance apportée par les événements du rapport et le code.
4. Sélectionner les cinq groupes de recommandations prioritaires et vérifier que chacun correspond à un défaut démontrable dans le code actuel.
5. Implémenter les cinq corrections prioritaires dans le même cycle. Si un groupe est déjà correctement traité ou n'est pas démontrable, l'écarter avec une justification précise et sélectionner le groupe prioritaire suivant afin de conserver cinq corrections effectives lorsque le rapport le permet.
6. Ajouter ou adapter des tests déterministes couvrant séparément chacune des cinq corrections.
7. Exécuter les tests pertinents puis la suite complète.
8. Terminer par un compte rendu structuré indiquant les regroupements identifiés, leur classement, les cinq corrections réalisées, les fichiers modifiés, les tests exécutés et les recommandations restantes.

Contraintes impératives :

- N'exécute jamais `git switch`, `git checkout`, `git branch`, `git reset`, `git clean`, `git commit`, `git merge`, `git rebase` ou `git push`.
- Respecte intégralement `AGENTS.md` et l'architecture hexagonale.
- Ne modifie jamais `.env`, `docker-compose.yml`, `Dockerfile`, le superviseur automatique, son prompt ou les rapports.
- Ne redéploie rien et ne lance pas une nouvelle simulation : le superviseur Java s'en charge après validation.
- Ne suppose pas que les recommandations du rapport sont justes. Confirme-les dans le code avant toute modification.
- Traite dans le même cycle les cinq groupes de recommandations prioritaires, même lorsqu'ils correspondent à des problèmes indépendants, tout en gardant des changements clairement séparés et testables.
- Préserve les modifications préexistantes de l'utilisateur.
- Si le défaut n'est pas démontrable ou qu'aucune correction sûre n'est possible, ne modifie aucun fichier et explique précisément pourquoi.
