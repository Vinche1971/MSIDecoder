# MSI Decoder (Android)

Application Android simple pour scanner des codes-barres en temps réel avec CameraX et ML Kit. Le projet sert de base pour l’identification de codes (dont MSI) et l’affichage du type et de la valeur détectés.

## Fonctionnalités
- **Scan en temps réel**: flux caméra avec `CameraX` et analyse via `ImageAnalysis`.
- **Détection ML Kit**: support de nombreux formats (QR, Code 128/39/93, Codabar, EAN-13/8, UPC-A/E, PDF417, Aztec, Data Matrix, ITF).
- **UI claire**: `PreviewView` plein écran, overlay de cadrage, panneau de résultats.
- **Callbacks structurés**: via `BarcodeResultListener`.
- **Base pour MSI**: doc intégrée et emplacement prévu pour une phase MSI dédiée.

## Aperçu de l’architecture
- `app/src/main/java/com/example/msidecoder/MainActivity.java`
  - Gère la permission caméra, l’initialisation CameraX, le binding `Preview` + `ImageAnalysis`, et met à jour l’UI via `BarcodeResultListener`.
- `app/src/main/java/com/example/msidecoder/scanner/BarcodeAnalyzer.java`
  - Analyse chaque frame avec ML Kit (`BarcodeScanning`) et notifie les résultats.
- `app/src/main/java/com/example/msidecoder/scanner/BarcodeResultListener.java`
  - Contrat de callbacks: `onBarcodeDetected(type, value)` et `onNoBarcodeDetected()`.
- `app/src/main/java/com/example/msidecoder/models/BarcodeResult.java`
  - Modèle optionnel pour encapsuler type/valeur/source/validité (non encore branché au flux UI).
- `app/src/main/res/layout/activity_main.xml`
  - Contient `PreviewView`, overlay `scan_overlay`, et panneau de résultats.
- `app/src/main/AndroidManifest.xml`
  - Permission `CAMERA`, meta-data ML Kit pour le téléchargement auto du modèle.

## Prérequis
- Android Studio récent (Giraffe/Koala/Iguana ou supérieur)
- Android SDK `compileSdk=34`, `targetSdk=34`
- JDK 17 recommandé
- Un appareil Android (ou émulateur avec caméra virtuelle)

## Installation
1. Cloner le repo dans votre environnement de dev.
2. Ouvrir le dossier `MSIDecoder/` dans Android Studio.
3. Laisser Android Studio synchroniser Gradle et télécharger les dépendances.
4. Brancher un appareil (mode débogage USB activé) ou démarrer un émulateur.
5. Lancer la configuration `app`.

### Build en ligne de commande
- Windows:
  ```bat
  gradlew.bat assembleDebug
  ```
- macOS/Linux:
  ```bash
  ./gradlew assembleDebug
  ```

## Exécution
- Au premier lancement, l’app demande la **permission caméra**.
- Cadrez le code-barres dans l’overlay: le type et la valeur s’affichent quand un code est détecté.

## Dépendances clés
- CameraX:
  - `androidx.camera:camera-core:1.3.1`
  - `androidx.camera:camera-camera2:1.3.1`
  - `androidx.camera:camera-lifecycle:1.3.1`
  - `androidx.camera:camera-view:1.3.1`
- ML Kit:
  - `com.google.mlkit:barcode-scanning:17.2.0`
- UI / AndroidX:
  - `androidx.appcompat:appcompat:1.6.1`
  - `com.google.android.material:material:1.11.0`
  - `androidx.constraintlayout:constraintlayout:2.1.4`

## Structure du projet (simplifiée)
```
MSIDecoder/
  app/
    src/main/
      java/com/example/msidecoder/
        MainActivity.java
        scanner/
          BarcodeAnalyzer.java
          BarcodeResultListener.java
        models/
          BarcodeResult.java
      res/
        layout/activity_main.xml
        drawable/scan_overlay.xml
        values/{strings.xml, colors.xml, themes.xml}
      AndroidManifest.xml
    build.gradle
  build.gradle
  settings.gradle
  DOCS/lecture_msi_fines_band.md
```

## Notes sur MSI
- La documentation de lecture/validation MSI est fournie dans `DOCS/lecture_msi_fines_band.md`.
- Points clés:
  - Découpage en « fines bandes » (noir=1, blanc=0), start=`110`, stop=`1001`.
  - Chiffres codés sur 12 bits; check digit (mod 10 ou 11/10) selon le contexte.
  - Contexte Pharmony: seuls les 7 premiers chiffres sont utilisés pour l’identification.
- Évolution prévue: ajouter une « Phase 2 » de décodage MSI spécifique après la détection ML Kit (validation + normalisation).

## Dépannage
- « Permission caméra requise »: accepter la permission dans le dialogue système ou via les paramètres.
- ML Kit ne détecte rien:
  - Vérifier la luminosité et la mise au point.
  - Laisser le temps au téléchargement auto du modèle ML Kit (premier lancement).
  - Essayer avec un autre format supporté (EAN-13, Code 128, etc.).
- Crash ou build AGP/JDK:
  - Utiliser JDK 17.
  - Nettoyer/rebuild: `./gradlew clean assembleDebug`.

## Licence
À définir.

## Crédits
- CameraX par AndroidX
- Barcode Scanning par Google ML Kit
