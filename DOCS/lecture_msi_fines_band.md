# Lecture et vérification d’un code MSI par découpe en fines bandes

Un code MSI peut être analysé comme une suite de **bandes verticales fines** de largeur égale :

- **Bande noire** → `1`
- **Bande blanche** → `0`

Une barre ou un espace **large** est simplement représenté par **deux bandes fines consécutives** de la même couleur.  
Il n’existe donc jamais plus de deux `1` ou deux `0` d’affilée.

---

## 1️⃣ Quiet zones (QZ)

Le code est encadré à gauche et à droite par une **quiet zone** :

- Zone blanche continue d’au moins plus de trois bandes fines
- Permet d’identifier le début et la fin du code

---

## 2️⃣ Start et Stop

- **Start** : immédiatement après la quiet zone gauche, le motif est toujours `110`  
  Noir (1), noir (1), blanc (0)
- **Stop** : juste avant la quiet zone droite, le motif est toujours `1001`  
  Noir (1), blanc (0), blanc (0), noir (1)

---

## 3️⃣ Chiffres

Entre le start et le stop, chaque chiffre est codé sur **12 bits** (fines bandes) selon la table suivante :

```python
msi_map = {
    '0': "100100100100",
    '1': "100100100110",
    '2': "100100110100",
    '3': "100100110110",
    '4': "100110100100",
    '5': "100110100110",
    '6': "100110110100",
    '7': "100110110110",
    '8': "110100100100",
    '9': "110100100110",
}
start_bits = "110"
stop_bits  = "1001"
```

---

## 4️⃣ Exemple : MSI `48334890`

Découpé en fines bandes (`1` = noir, `0` = blanc) :

```
(QZ) 110   100110100100   110100100100   100100110110   100100110110  
     100110100100   110100100100   110100100110   100100100100   1001 (QZ)
```

Ce qui donne :

- **Start** : `110`
- **4** : `100110100100`
- **8** : `110100100100`
- **3** : `100100110110`
- **3** : `100100110110`
- **4** : `100110100100`
- **8** : `110100100100`
- **9** : `110100100110`
- **0** : `100100100100`
- **Stop** : `1001`

---

## 5️⃣ Vérification par **check digit**

Les codes MSI incluent généralement **un chiffre de contrôle** :

- **Modulo 10** : le plus courant
- **Modulo 11/10** : utilisé dans certains secteurs spécifiques

Le chiffre de contrôle se trouve à la **fin** du code, juste avant le stop.  
Pour valider un MSI, on recalcule ce check digit selon la méthode spécifiée et on compare au chiffre lu.

---

## 6️⃣ Cas spécifique à Pharmony

Dans l’usage **Pharmony**, **seuls les 7 premiers chiffres** du MSI sont utilisés pour l’identification du produit.  
Le check digit et tout chiffre supplémentaire peuvent être ignorés pour cette utilisation.

---

## 💡 Avantages de cette méthode

1. Identifier la largeur d’une bande fine
2. Découper un code MSI visuellement ou depuis une image
3. Transformer les bandes en suite binaire
4. Lire et vérifier le code sans se soucier explicitement des notions “large/étroit”
5. Valider via le check digit pour confirmer qu’il s’agit bien d’un MSI
