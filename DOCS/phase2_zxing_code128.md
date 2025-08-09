# Phase 2 — Intégration ZXing pour Code 128

- Statut: DONE ✅ (décodage Code 128 via ZXing, MLKit exclut Code 128)
- Notes finales:
  - Ajout d’un pivot explicite du plan Y (90°/270°) avant création de la `LuminanceSource` pour fiabiliser les 1D.
  - ROI centrale (paramétrable), essais multi-rotations, inversion, et fallback GlobalHistogramBinarizer.
  - Déduplication des résultats (debounce court) et indication de source dans l’UI: `Code 128 (ZXing)`.

---

- Objectif: décoder les Code 128 via ZXing tout en désactivant ce format côté MLKit, en préparation de la phase 3 (décodeur MSI maison).
- Contrainte: conserver CameraX + `ImageAnalysis` comme pipeline vidéo existant.

## Choix de la bibliothèque
- Recommandé: `com.google.zxing:core` (léger, sans couche caméra) — s’intègre proprement avec `ImageProxy` de CameraX.
- Alternative: `com.journeyapps:zxing-android-embedded` — bien documentée, mais gère la caméra; utilisée ici comme source d’exemples seulement.

### Dépendance
```gradle
dependencies {
    implementation 'com.google.zxing:core:3.5.1'
}
```

## Configuration MLKit (exclure Code 128)
- Adapter `BarcodeScannerOptions` pour retirer `Barcode.FORMAT_CODE_128`.

## Formats d’image et attentes ZXing
- ZXing attend une `LuminanceSource` (niveau de gris) → `PlanarYUVLuminanceSource`.
- Avec CameraX (`YUV_420_888`): extraire le plan Y, gérer `rowStride`/`pixelStride`.

## Pipeline CameraX → ZXing (implémenté)
1. `analyze(ImageProxy)` avec verrou `isProcessing` et backpressure KEEP_ONLY_LATEST
2. Extraire le plan Y
3. Si rotation capteur 90/270: pivoter explicitement le buffer Y en 90° CW pour aligner l’échantillonnage 1D
4. Créer `PlanarYUVLuminanceSource` plein cadre et ROI centrale (paramétrable via fractions)
5. Tenter décodage avec `HybridBinarizer`, puis fallback `GlobalHistogramBinarizer`
6. Hints: `POSSIBLE_FORMATS=[CODE_128]`, `TRY_HARDER=true` (option: `ASSUME_GS1`)
7. En cas de succès: émettre `Code 128 (ZXing)` avec debounce
8. Sinon: fallback MLKit pour autres formats

## Performance & UX
- ROI stricte (80% x 30% par défaut) et pivot Y pour robustesse 1D.
- Debounce ~1.2s pour éviter les répétitions.
- Logs détaillés pour diag (frame info, ROI, base=ROI(rot)/FULL(rot)/ROI/FULL, orientationIndex).

## Tests
- Codes 128 variés testés; succès confirmé en rotation 90° via ROI(rot).

## Références
- ZXing Core: https://github.com/zxing/zxing
- `Code128Reader`: https://zxing.github.io/zxing/apidocs/com/google/zxing/oned/Code128Reader.html
- JourneyApps: https://github.com/journeyapps/zxing-android-embedded
