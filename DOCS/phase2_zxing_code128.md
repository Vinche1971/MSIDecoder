# Phase 2 — Intégration ZXing pour Code 128

- **Objectif**: décoder les Code 128 via ZXing tout en désactivant ce format côté MLKit, en préparation de la phase 3 (décodeur MSI maison).
- **Contrainte**: conserver CameraX + `ImageAnalysis` comme pipeline vidéo existant.

## Choix de la bibliothèque
- **Recommandé**: `com.google.zxing:core` (léger, sans couche caméra) — s’intègre proprement avec `ImageProxy` de CameraX.
- **Alternative**: `com.journeyapps:zxing-android-embedded` — très bien documentée et pratique (vues & utilitaires), mais embarque sa propre gestion caméra. À utiliser seulement comme source d’exemples/`RGBLuminanceSource` si besoin.

### Dépendance
```gradle
dependencies {
    implementation 'com.google.zxing:core:3.5.1' // ou version 3.5.x la plus récente
}
```

## Configuration MLKit (exclure Code 128)
- Adapter `BarcodeScannerOptions` pour retirer `Barcode.FORMAT_CODE_128`.
- Laisser MLKit gérer les autres formats (EAN, QR, etc.).

## Formats d’image et attentes ZXing
- ZXing attend une `LuminanceSource` (niveau de gris) → utiliser `PlanarYUVLuminanceSource`.
- Avec CameraX, `ImageProxy` est en `YUV_420_888`:
  - Extraire le **plan Y** (luminance), gérer `rowStride`/`pixelStride`.
  - Produire un buffer Y contigu de taille `width*height`.
  - Définir une **ROI** (région d’intérêt) mappée sur l’overlay pour améliorer la perf/fiabilité.

## Pipeline CameraX → ZXing
1. `ImageAnalysis.Analyzer.analyze(ImageProxy)`
2. Si déjà en cours: sortir (`isProcessing`)
3. Extraire le plan Y de `imageProxy.getImage()` (format `YUV_420_888`)
4. Construire la **ROI** à partir des dimensions du `PreviewView` et de la vue overlay
5. Créer `PlanarYUVLuminanceSource(yBuffer, width, height, left, top, roiWidth, roiHeight, false)`
6. `BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource))`
7. `MultiFormatReader` avec `DecodeHintType` configurés (voir ci-dessous)
8. `reader.decode(bitmap)` et gérer exceptions (`NotFoundException`, `ChecksumException`, `FormatException`)
9. Publier le résultat en callback UI

### Hints ZXing (fortement recommandés)
- `POSSIBLE_FORMATS = [BarcodeFormat.CODE_128]`
- `TRY_HARDER = true`
- Optionnel: `ASSUME_GS1 = true` selon le domaine

## Exemple (extrait Java adapté)
> Note: cet extrait illustre le chemin critique sans tout le contrôle d’erreurs/boilerplate.

```java
// 1) Extraire le plan Y (luminance) en buffer contigu
ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
ByteBuffer yBufferRaw = yPlane.getBuffer();
int width = imageProxy.getWidth();
int height = imageProxy.getHeight();
int rowStride = yPlane.getRowStride();
int pixelStride = yPlane.getPixelStride(); // souvent 1 pour Y

byte[] yData;
if (pixelStride == 1 && rowStride == width) {
    yData = new byte[width * height];
    yBufferRaw.get(yData);
} else {
    yData = new byte[width * height];
    byte[] row = new byte[rowStride];
    for (int y = 0; y < height; y++) {
        yBufferRaw.position(y * rowStride);
        yBufferRaw.get(row, 0, Math.min(rowStride, yBufferRaw.remaining()));
        for (int x = 0; x < width; x++) {
            yData[y * width + x] = row[x * pixelStride];
        }
    }
}

// 2) Définir la ROI (ex: centrée, proportionnelle à l’overlay)
int roiWidth = (int) (width * 0.7f);
int roiHeight = (int) (height * 0.3f);
int left = (width - roiWidth) / 2;
int top = (height - roiHeight) / 2;

// 3) Construire la LuminanceSource
LuminanceSource source = new PlanarYUVLuminanceSource(
        yData, width, height,
        left, top, roiWidth, roiHeight,
        false
);
BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

// 4) Configurer les hints et décoder
Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.CODE_128));
hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

Result result;
try {
    MultiFormatReader reader = new MultiFormatReader();
    reader.setHints(hints);
    result = reader.decode(binaryBitmap);
    String text = result.getText();
    // TODO: publier le résultat (type=Code 128, value=text)
} catch (NotFoundException | ChecksumException | FormatException e) {
    // TODO: aucun code 128 trouvé ou invalide
}
```

## Performance & UX
- Continuer à utiliser `STRATEGY_KEEP_ONLY_LATEST` (déjà en place).
- Conserver un verrou léger (`isProcessing`) pour éviter la pression.
- ROI stricte = moins de pixels à binariser donc plus rapide.
- Éviter conversions `Bitmap/RGB`: rester en Y (luminance) pour meilleures perfs.
- Option: debouncer de résultats pour réduire les doublons UI.

## Tests
- Échantillons réels de Code 128 (tailles, contrastes, orientations variées).
- Vérifier la robustesse en basse lumière et avec reflets.
- Cas limites: ROI trop petite, flou, distance excessive.
- Mesurer latence avg (ms) et taux de succès (%).

## Erreurs & Journalisation
- `NotFoundException`: aucun code lisible dans la ROI → ignorer silencieusement.
- `ChecksumException` / `FormatException`: code détecté mais invalide → journaliser en debug.
- Loggers discrets pour ne pas inonder la console.

## Roadmap vers Phase 3 (MSI maison)
- Réutiliser la même extraction Y + ROI.
- Remplacer la phase ZXing par un décodeur MSI custom (bandes fines + check digit) dans une branche conditionnelle.
- Garder MLKit pour les autres formats.

## Références
- ZXing Core (API/Code): [zxing/zxing](https://github.com/zxing/zxing)
- `Code128Reader` (API): [zxing.github.io Code128Reader](https://zxing.github.io/zxing/apidocs/com/google/zxing/oned/Code128Reader.html)
- JourneyApps (exemples Android): [journeyapps/zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded)
