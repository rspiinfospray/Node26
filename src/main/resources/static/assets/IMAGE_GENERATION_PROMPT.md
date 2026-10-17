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

## Rivière et pont

Les fichiers `terrain-river.png` et `terrain-bridge.png` ont été produits avec les prompts suivants, puis leur fond magenta uniforme a été converti en transparence.

### Rivière

> Create one simple river terrain tile for an isometric RPG game. Use `terrain-grass-flat.png` as the exact style, silhouette, camera-angle, scale and composition reference. The entire diamond-shaped cell is calm blue water flowing continuously from the upper-left edge to the lower-right edge, so identical neighboring tiles connect seamlessly. Use only very small subtle stylized ripples. Clean colorful low-detail 3D RPG style. Same isometric axes and footprint as the reference, filling almost the whole canvas, without soil thickness or raised base. No grass banks, rocks, plants, bridge, land, text or watermark. Render outside the tile on a perfectly uniform solid `#ff00ff` chroma-key background.

### Pont

> Create an isolated isometric RPG terrain tile: a simple sturdy wooden plank bridge over a full diamond-shaped calm blue water tile. The bridge runs exactly from the lower-left edge to the upper-right edge and touches both edges. Use a colorful low-detail RPG style matching `terrain-grass-flat.png` and the river tile, with chunky readable wooden planks and two simple side beams, without railings. Keep the exact clean diamond boundary, with no terrain thickness. Only water and wood inside the tile; no land, grass, rocks, plants, characters, text or watermark. Every pixel outside the diamond must be exactly uniform solid pure `#ff00ff`, without shadow, glow, vignette, gradient or floor plane.

### Correction hexagonale finale

La rivière a ensuite été régénérée en imposant comme invariant le contour extérieur exact à six côtés de `terrain-grass-flat.png`, sans losange à quatre côtés. Le pont final reprend exactement cette tuile hexagonale et demande un véritable ouvrage large et surélevé : tablier cohérent, poutres porteuses, poteaux robustes et garde-corps continus sur les deux côtés. Il traverse la rivière du bord inférieur gauche au bord supérieur droit et conserve l’eau visible de part et d’autre.

Le rendu conserve le style RPG simple, les axes, la projection isométrique, la palette et l'éclairage des images de référence. Chaque surface est centrée sur un fond transparent avec une marge minimale.
