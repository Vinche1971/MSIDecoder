# MSI Decoder - Android App

Application Android pour décoder les codes-barres MSI (MSI Plessey) en adaptant l'algorithme Code39 de ZXing.

## 🎯 Objectif du Projet

Les codes MSI ne sont pas nativement supportés par Google MLKit. Ce projet vise à créer un décodeur MSI personnalisé en s'appuyant sur :
- **MLKit** pour la détection d'images de codes-barres
- **ZXing** comme base algorithmique (Code39 → MSI)
- **Adaptation personnalisée** pour les spécificités MSI

## 🚀 Plan d'Implémentation (3 Phases)

### Phase 1 : Décodage Standard MLKit
- ✅ Configuration MLKit dans l'app Android
- ✅ Interface de scan basique avec caméra
- ✅ Décodage des codes-barres standards (QR, Code128, etc.)
- ✅ Validation du fonctionnement de base

**Objectif :** S'assurer que la détection et le décodage fonctionnent correctement avec MLKit

### Phase 2 : Intégration ZXing pour Code39
- ✅ Intégration de la librairie ZXing
- ✅ Passerelle MLKit → ZXing pour images Code39
- ✅ Décodage Code39 avec ZXing
- ✅ Validation du pipeline hybride MLKit/ZXing

**Objectif :** Valider que l'approche hybride fonctionne avec Code39 comme preuve de concept

### Phase 3 : Décodeur MSI Personnalisé
- ✅ Adaptation du `Code39Reader` de ZXing
- ✅ Implémentation des patterns MSI
- ✅ Support des checksums MSI (Mod10, Mod11, etc.)
- ✅ Intégration complète dans l'app

**Objectif :** Créer un décodeur MSI fonctionnel basé sur l'architecture ZXing

## 📋 Spécifications Techniques

### Codes MSI vs Code39

| Aspect | MSI (MSI Plessey) | Code39 |
|--------|-------------------|--------|
| **Caractères** | Numérique uniquement (0-9) | Alphanumérique (A-Z, 0-9, symboles) |
| **Structure** | 4 barres + 4 espaces (8 éléments) | 5 barres + 4 espaces (9 éléments) |
| **Start/Stop** | Symboles MSI dédiés | Astérisque (*) |
| **Checksum** | Mod10/Mod11 (facultatif) | Mod43 (facultatif) |
| **Encodage** | Binaire (barres=1, espaces=0) | Largeur (narrow/wide) |

### Patterns MSI
- **Format :** 1 bit préfixe + 4 bits données + 2 bits suffixes (0)
- **Représentation :** 
  - 0 bit = 1/3 barre + 2/3 espace
  - 1 bit = 2/3 barre + 1/3 espace

## 🛠️ Architecture Technique

```
MSIDecoderApp/
├── app/src/main/java/
│   ├── scanner/
│   │   ├── CameraActivity.java          # Interface scan caméra
│   │   ├── MLKitBarcodeScanner.java     # Wrapper MLKit
│   │   └── BarcodeProcessor.java        # Traitement résultats
│   ├── decoder/
│   │   ├── ZXingIntegration.java        # Bridge MLKit→ZXing
│   │   ├── Code39Decoder.java           # Décodeur Code39 (Phase 2)
│   │   └── MSIDecoder.java              # Décodeur MSI (Phase 3)
│   ├── models/
│   │   ├── BarcodeResult.java           # Résultat de décodage
│   │   └── MSIChecksum.java             # Algorithmes checksum
│   └── utils/
│       ├── PatternUtils.java            # Utilitaires patterns
│       └── ImageProcessor.java          # Traitement d'images
└── build.gradle                         # Dépendances projet
```

## 📚 Ressources de Référence

### Documentation ZXing
- **Code39Reader :** `https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/oned/Code39Reader.java`
- **Patterns Code39 :** CHARACTER_ENCODINGS, recordPattern(), toNarrowWidePattern()

### Implémentation MSI Existante
- **MSI Java :** `https://github.com/barnhill/barcode-java/blob/main/src/main/java/com/pnuema/java/barcode/symbologies/MSI.java`
- **Patterns MSI :** {"100100100100", "100100100110", "100100110100"...}

### Algorithmes Checksum MSI
1. **Mod 10** (Luhn) - Le plus courant
2. **Mod 11 IBM** - Pondération (2,3,4,5,6,7)
3. **Double checksum** - Mod10+Mod10 ou Mod11+Mod10

## 🚦 États d'Avancement

- [ ] **Phase 1** - Configuration MLKit et scan de base
- [ ] **Phase 2** - Intégration ZXing pour Code39
- [ ] **Phase 3** - Développement décodeur MSI personnalisé

## 🔧 Prérequis

- Android Studio installé
- API Level minimum : 21 (Android 5.0)
- Permissions caméra
- Dépendances :
  - `com.google.mlkit:barcode-scanning`
  - `com.journeyapps:zxing-android-embedded`

## 🎯 Cas d'Usage

Application destinée au secteur pharmaceutique belge pour le décodage de codes MSI sur les médicaments et dispositifs médicaux.

---

*Projet développé en collaboration avec Claude Code pour l'adaptation d'algorithmes de décodage de codes-barres.*