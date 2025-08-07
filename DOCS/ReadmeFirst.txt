## **1. Code source ZXing Code39Reader :**

Le code source complet du décodeur Code39 de ZXing est disponible sur GitHub  :

- **URL principale :** `https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/oned/Code39Reader.java`

**Éléments clés à retenir du Code39Reader :**

- Pattern encodings : `CHARACTER_ENCODINGS = { 0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070... }` 
- Structure : détection pattern asterisk, décodage caractère par caractère, vérification checksum 
- Gestion des largeurs de barres avec méthode `recordPattern()` et `toNarrowWidePattern()` 

## **2. Spécifications MSI pour ton décodeur :**

MSI utilise uniquement des chiffres 0-9, chaque chiffre étant converti en 4 bits binaires . Structure :

- Format : 1 bit préfixé + 4 bits données + 2 bits suffixés (0) 
- Représentation : 0 bit = 1/3 barre + 2/3 espace, 1 bit = 2/3 barre + 1/3 espace 

## **3. Implémentation MSI Java prête (inspiration) :**

Il existe déjà une implémentation MSI complète en Java  que tu peux adapter :

- **URL :** `https://github.com/barnhill/barcode-java/blob/main/src/main/java/com/pnuema/java/barcode/symbologies/MSI.java`
- Pattern encodings MSI : `{"100100100100", "100100100110", "100100110100"...}` 
- Support des différents checksums : Mod10, Mod11, 2Mod10, Mod11_Mod10 

## **4. Algorithmes de checksum MSI :**

MSI supporte 5 types de checksum différents  :

- Mod 10 (le plus courant) : utilise l’algorithme Luhn 
- Mod 11 IBM : pattern de pondération (2,3,4,5,6,7) 
- Double checksum : Mod10+Mod10 ou Mod11+Mod10

## **Plan d’action recommandé :**

1. **Étudier le Code39Reader** de ZXing pour comprendre la logique générale
1. **Adapter les patterns** : remplacer les patterns Code39 par les patterns MSI
1. **Simplifier le décodage** : MSI étant uniquement numérique, tu peux optimiser
1. **Intégrer avec MLKit** : utiliser MLKit pour la détection, ton décodeur pour la reconnaissance

Tu as maintenant toute la doc nécessaire ! Le combo MLKit + décodeur MSI custom inspiré de ZXing sera parfait pour ton projet pharmaceutique belge.


Voici un tableau markdown résumant toutes les différences essentielles entre MSI (MSI Plessey) et Code39, conçu pour t’aider à structurer le portage ou l’écriture d’un décodeur MSI depuis une base Code39 :

| Caractéristique       | MSI / MSI Plessey                                | Code39                                     |
|-----------------------|--------------------------------------------------|--------------------------------------------|
| Encodage supporté     | Numérique (0-9) uniquement[6][3][4]             | Alphanumérique : chiffres, majuscules, certains symboles (–, ., $, /, +, %, espace)[1][2][8] |
| Structure caractère   | 4 barres + 4 espaces (8 éléments par chiffre)[4] | 5 barres + 4 espaces (9 éléments par caractère), codage par largeur (narrow/wide)[1][8] |
| Start/Stop            | Symboles spécifiques à MSI, fixes et simples[4]  | * (astérisque) comme start/stop, pattern dédié (identique au A-Z, 0-9, mais réservé)[2][1] |
| Check digit           | Facultatif, Modulo 10 ou Modulo 11 (ou double check Mod10/11), pas d’auto-contrôle natif[3][4] | Facultatif, généralement Mod 43, auto-contrôlé grâce à la structure du code[2][5] |
| Auto-contrôle         | Non (dépend du check digit)[3][4]                | Oui (structure intrinsèquement auto-contrôlée)[2][5]        |
| Encodage binaire      | Oui, séquences de barres = "1", espaces = "0"[4] | Largeur (narrow/wide), non binaire pur     |
| Longueur              | Variable, aucune limite officielle[4]            | Variable, mais par structure de caractères[1]               |
| Ambiguïté visuelle    | Parfois confondu avec Plessey                   | Distinct par start/stop et patterns plus larges             |
| Utilisation           | Etiquetage inventaire, retail, entrepôts[4][6]   | Industrie, logistique, santé, documents, objets[1][2]       |
| Densité               | Bonne pour numérique, peu compacte               | Assez faible (code-barres étendu)[1][8]                     |
| Élément clé à implémenter | Table de patterns chiffre (8 bits), check digit personnalisé | Table patterns 9 éléments, mapping alphanumérique, start/stop astérisque  |
| Licence/IP            | Libre de droits, non breveté                     | Libre de droits, non breveté[1][2]                          |

**Différences notables sur l’implémentation :**
- La gestion des patterns (motifs barres/espaces) est entièrement différente : il faudra recréer ou substituer la table.
- Le découpage lors de la détection d’un chiffre MSI se fait sur 8 éléments, pas 9 comme Code39.
- La gestion du start/stop est unique : MSI a des symboles dédiés différents de l’astérisque Code39.
- Le check digit MSI est souvent essentiel à l’intégrité, contrairement à Code39 où la correction d’erreur est intrinsèque.
