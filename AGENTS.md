# Conventions de code

- Toute référence à un attribut ou à une méthode d'instance pouvant être
  préfixée par `this.` doit obligatoirement utiliser ce préfixe.
- Chaque instruction de log doit être écrite intégralement sur une seule ligne
  dans le code source, y compris lorsque le message ou la liste des arguments
  est long.
- Le projet doit respecter une architecture hexagonale : le domaine et les cas
  d'usage ne doivent dépendre d'aucune implémentation technique. Les interactions
  avec l'extérieur doivent passer par des ports, implémentés par des adaptateurs
  primaires ou secondaires. Les anciens termes `in` et `out` ne doivent pas être
  utilisés dans la nomenclature : utiliser `primary` pour les ports et adaptateurs
  qui pilotent l'application, et `secondary` pour ceux pilotés par l'application.
- Une commande peut orchestrer des services, mais elle ne doit jamais appeler
  une autre commande. Un service ne doit dépendre ni d'une commande ni d'un
  autre service.
- La méthode principale exposée par une commande doit toujours s'appeler
  `execute()`. Elle peut ne recevoir aucun paramètre ou recevoir les paramètres
  nécessaires au cas d'usage.
- Une déclaration de méthode ou de constructeur comportant au maximum cinq
  paramètres doit être écrite sur une seule ligne. Au-delà de cinq paramètres,
  les paramètres peuvent être répartis sur plusieurs lignes, mais le `) {` doit
  toujours se trouver sur la même ligne que le dernier paramètre.
- Lors de l'appel d'une méthode ou d'un constructeur, les arguments doivent être
  écrits sur une seule ligne lorsqu'ils sont au nombre de cinq ou moins. Au-delà
  de cinq arguments, ils peuvent être répartis sur plusieurs lignes.
- Une méthode ne doit pas être créée si son unique instruction consiste à
  retourner telle quelle la valeur d'une méthode sous-jacente. Les appelants
  doivent invoquer directement la méthode sous-jacente. Seules les méthodes
  imposées par un contrat externe ou ajoutant un comportement réel sont admises.
