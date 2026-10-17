# Prompt de génération des images

Ce texte restitue l'intention du prompt original. Il ne s'agit pas d'une transcription mot pour mot.

## Prompt restitué

> Génère des images simples, sans trop de détails, dans un style RPG. Les assets doivent utiliser des axes et un angle de vue compatibles avec le rendu 3D isométrique du jeu. Ils doivent partager une orientation, une perspective, une échelle visuelle et un éclairage cohérents afin de pouvoir être intégrés ensemble sur la même carte. Chaque élément doit être centré, entièrement visible, facilement reconnaissable à petite taille et présenté seul sur un fond transparent. Génère séparément une tuile d'herbe, une tuile rocheuse, une tuile avec un arbre, un paysan, un mouton, une maison, un grenier à grains, un champ cultivé, une tombe, une réserve de nourriture et un fagot de bois.

## Fichiers concernés

- `terrain-grass.png`
- `terrain-rock.png`
- `terrain-tree.png`
- `peon-topdown.png`
- `sheep.png`
- `house.png`
- `granary.png`
- `field.png`
- `grave.png`
- `food-cache.png`
- `wood-bundle.png`

## Corrections des surfaces planes

Les deux fichiers suivants ont été générés ensuite pour supprimer les tranches de terre incompatibles avec les cellules centrales :

- `terrain-grass-flat.png` : surface d'herbe hexagonale plane, sans paroi, socle ou extrusion, agrandie pour remplir la cellule ;
- `field-flat.png` : surface de champ hexagonale plane, sans paroi, socle ou extrusion, agrandie pour remplir la cellule.

Le rendu conserve le style RPG simple, les axes, la projection isométrique, la palette et l'éclairage des images de référence. Chaque surface est centrée sur un fond transparent avec une marge minimale.
