package com.example.msidecoder.scanner;

import android.annotation.SuppressLint;
import android.util.Log;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.DecodeHintType;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "BarcodeAnalyzer";

    private final BarcodeResultListener listener;
    private final BarcodeScanner mlKitScanner;
    private final RoiProvider roiProvider;

    // ZXing reader configuré pour Code 128 uniquement
    private final MultiFormatReader zxingReader;
    private final Map<DecodeHintType, Object> zxingHints;

    private boolean isProcessing = false;
    private String lastEmittedValue = null;
    private long lastEmittedAtMs = 0L;
    private static final long DEBOUNCE_MS = 1200L;

    public BarcodeAnalyzer(BarcodeResultListener listener) {
        this(listener, null);
    }

    public BarcodeAnalyzer(BarcodeResultListener listener, RoiProvider roiProvider) {
        this.listener = listener;
        this.roiProvider = roiProvider;

        // MLKit: exclure Code 128 (géré par ZXing)
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        // Barcode.FORMAT_CODE_128, // exclu
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_ITF
                )
                .build();
        this.mlKitScanner = BarcodeScanning.getClient(options);

        // ZXing: préparer le reader et les hints pour Code 128
        this.zxingReader = new MultiFormatReader();
        EnumMap<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.CODE_128));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        this.zxingHints = hints;
        this.zxingReader.setHints(this.zxingHints);
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        isProcessing = true;

        // 1) Tentative ZXing (Code 128 uniquement) sur la luminance Y du frame
        try {
            String code128 = tryDecodeCode128WithZXing(imageProxy);
            if (code128 != null && !code128.isEmpty()) {
                Log.d(TAG, "ZXing Code128 détecté: " + code128);
                emitIfNotDuplicate("Code 128 (ZXing)", code128);
                isProcessing = false;
                imageProxy.close();
                return;
            }
        } catch (Exception e) {
            Log.d(TAG, "ZXing tentative échouée: " + e.getMessage());
            // On poursuit avec MLKit
        }

        // 2) MLKit pour les autres formats (Code 128 exclu)
        @SuppressLint("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        mlKitScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        String type = getBarcodeTypeName(barcode.getFormat());
                        String value = barcode.getDisplayValue();

                        Log.d(TAG, "MLKit détecté - Type: " + type + ", Valeur: " + value);

                        if (value != null && !value.isEmpty()) {
                            emitIfNotDuplicate(type, value);
                        } else {
                            listener.onNoBarcodeDetected();
                        }
                    } else {
                        listener.onNoBarcodeDetected();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur de scan MLKit: " + e.getMessage());
                    listener.onNoBarcodeDetected();
                })
                .addOnCompleteListener(task -> {
                    isProcessing = false;
                    imageProxy.close();
                });
    }

    private String getBarcodeTypeName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE:
                return "QR Code";
            case Barcode.FORMAT_CODE_128:
                return "Code 128";
            case Barcode.FORMAT_CODE_39:
                return "Code 39";
            case Barcode.FORMAT_CODE_93:
                return "Code 93";
            case Barcode.FORMAT_CODABAR:
                return "Codabar";
            case Barcode.FORMAT_EAN_13:
                return "EAN-13";
            case Barcode.FORMAT_EAN_8:
                return "EAN-8";
            case Barcode.FORMAT_UPC_A:
                return "UPC-A";
            case Barcode.FORMAT_UPC_E:
                return "UPC-E";
            case Barcode.FORMAT_PDF417:
                return "PDF417";
            case Barcode.FORMAT_AZTEC:
                return "Aztec";
            case Barcode.FORMAT_DATA_MATRIX:
                return "Data Matrix";
            case Barcode.FORMAT_ITF:
                return "ITF";
            default:
                return "Unknown (" + format + ")";
        }
    }

    private String tryDecodeCode128WithZXing(@NonNull ImageProxy imageProxy) throws NotFoundException {
        if (imageProxy.getImage() == null) {
            return null;
        }

        // Extraire le plan Y en buffer contigu
        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer().duplicate();
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int rowStride = yPlane.getRowStride();
        int pixelStride = yPlane.getPixelStride(); // souvent 1 pour Y

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        Log.d(TAG, "ZXing frame info - w=" + width + ", h=" + height + ", rowStride=" + rowStride + ", pixelStride=" + pixelStride + ", rotation=" + rotation);

        byte[] yData = new byte[width * height];
        if (pixelStride == 1 && rowStride == width) {
            // Chemin rapide: data déjà contiguë
            yBuffer.rewind();
            yBuffer.get(yData, 0, Math.min(yData.length, yBuffer.remaining()));
        } else {
            // Recopie ligne par ligne en tenant compte du stride
            byte[] row = new byte[rowStride];
            for (int y = 0; y < height; y++) {
                int pos = y * rowStride;
                if (pos >= yBuffer.limit()) break;
                yBuffer.position(pos);
                int len = Math.min(rowStride, yBuffer.remaining());
                yBuffer.get(row, 0, len);
                for (int x = 0; x < width; x++) {
                    int srcIndex = x * pixelStride;
                    if (srcIndex < len) {
                        yData[y * width + x] = row[srcIndex];
                    }
                }
            }
        }

        // Préparer les sources: si rotation 90/270, on génère aussi un buffer Y explicitement pivoté
        LuminanceSource[] bases;
        String[] baseLabels;
        if (rotation == 90 || rotation == 270) {
            int rotatedWidth = height;
            int rotatedHeight = width;
            byte[] yRot = new byte[yData.length];
            // Rotation 90° CW pour aligner les lignes d'échantillonnage 1D
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int newX = y;
                    int newY = (rotatedHeight - 1) - x;
                    yRot[newY * rotatedWidth + newX] = yData[y * width + x];
                }
            }

            LuminanceSource fullRot = new PlanarYUVLuminanceSource(
                    yRot, rotatedWidth, rotatedHeight,
                    0, 0, rotatedWidth, rotatedHeight,
                    false
            );
            int roiW = Math.max(1, (int) (rotatedWidth * 0.8f));
            int roiH = Math.max(1, (int) (rotatedHeight * 0.3f));
            int roiL = Math.max(0, (rotatedWidth - roiW) / 2);
            int roiT = Math.max(0, (rotatedHeight - roiH) / 2);
            if (roiProvider != null) {
                RectF f = roiProvider.getRoiFraction();
                if (f != null) {
                    roiL = clampToInt(f.left * rotatedWidth, 0, Math.max(0, rotatedWidth - 1));
                    roiT = clampToInt(f.top * rotatedHeight, 0, Math.max(0, rotatedHeight - 1));
                    roiW = clampToInt(f.width() * rotatedWidth, 1, rotatedWidth - roiL);
                    roiH = clampToInt(f.height() * rotatedHeight, 1, rotatedHeight - roiT);
                }
            }
            LuminanceSource roiRot = new PlanarYUVLuminanceSource(
                    yRot, rotatedWidth, rotatedHeight,
                    roiL, roiT, roiW, roiH,
                    false
            );
            Log.d(TAG, "ZXing ROI (rotated) - left=" + roiL + ", top=" + roiT + ", w=" + roiW + ", h=" + roiH + ", rotW=" + rotatedWidth + ", rotH=" + rotatedHeight);

            // Créer aussi les sources sur le buffer original en fallback
            LuminanceSource fullSource = new PlanarYUVLuminanceSource(
                    yData, width, height,
                    0, 0, width, height,
                    false
            );
            int roiWidth = Math.max(1, (int) (width * 0.8f));
            int roiHeight = Math.max(1, (int) (height * 0.3f));
            int roiLeft = Math.max(0, (width - roiWidth) / 2);
            int roiTop = Math.max(0, (height - roiHeight) / 2);
            if (roiProvider != null) {
                RectF f = roiProvider.getRoiFraction();
                if (f != null) {
                    roiLeft = clampToInt(f.left * width, 0, Math.max(0, width - 1));
                    roiTop = clampToInt(f.top * height, 0, Math.max(0, height - 1));
                    roiWidth = clampToInt(f.width() * width, 1, width - roiLeft);
                    roiHeight = clampToInt(f.height() * height, 1, height - roiTop);
                }
            }
            LuminanceSource roiSource = new PlanarYUVLuminanceSource(
                    yData, width, height,
                    roiLeft, roiTop, roiWidth, roiHeight,
                    false
            );
            Log.d(TAG, "ZXing ROI - left=" + roiLeft + ", top=" + roiTop + ", w=" + roiWidth + ", h=" + roiHeight);

            bases = new LuminanceSource[] { roiRot, fullRot, roiSource, fullSource };
            baseLabels = new String[] { "ROI(rot)", "FULL(rot)", "ROI", "FULL" };
        } else {
            // Cas 0°/180°: sources sur buffer original
            LuminanceSource fullSource = new PlanarYUVLuminanceSource(
                    yData, width, height,
                    0, 0, width, height,
                    false
            );
            int roiWidth = Math.max(1, (int) (width * 0.8f));
            int roiHeight = Math.max(1, (int) (height * 0.3f));
            int roiLeft = Math.max(0, (width - roiWidth) / 2);
            int roiTop = Math.max(0, (height - roiHeight) / 2);
            LuminanceSource roiSource = new PlanarYUVLuminanceSource(
                    yData, width, height,
                    roiLeft, roiTop, roiWidth, roiHeight,
                    false
            );
            Log.d(TAG, "ZXing ROI - left=" + roiLeft + ", top=" + roiTop + ", w=" + roiWidth + ", h=" + roiHeight);

            bases = new LuminanceSource[] { roiSource, fullSource };
            baseLabels = new String[] { "ROI", "FULL" };
        }
        for (int b = 0; b < bases.length; b++) {
            LuminanceSource base = bases[b];
            String baseLabel = baseLabels[b];
            LuminanceSource[] candidates;
            if (base.isRotateSupported()) {
                // Ordre préféré selon rotation pour augmenter les chances dès la 1ère tentative
                if (rotation == 90) {
                    candidates = new LuminanceSource[] {
                            base.rotateCounterClockwise(), // 90°
                            base,                           // 0°
                            base.rotateCounterClockwise().rotateCounterClockwise(), // 180°
                            base.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise() // 270°
                    };
                } else if (rotation == 270) {
                    candidates = new LuminanceSource[] {
                            base.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise(), // 270°
                            base,                                                                           // 0°
                            base.rotateCounterClockwise(),                                                  // 90°
                            base.rotateCounterClockwise().rotateCounterClockwise()                          // 180°
                    };
                } else {
                    candidates = new LuminanceSource[] {
                            base,                           // 0°
                            base.rotateCounterClockwise(),  // 90°
                            base.rotateCounterClockwise().rotateCounterClockwise(), // 180°
                            base.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise() // 270°
                    };
                }
            } else {
                candidates = new LuminanceSource[] { base };
            }

            for (int i = 0; i < candidates.length; i++) {
                LuminanceSource src = candidates[i];
                String label = (i == 0 ? "0/90 selon ordre" : (i == 1 ? "1" : (i == 2 ? "2" : "3")));
                try {
                    String text = decodeOnce(src);
                    if (text != null) {
                        Log.d(TAG, "ZXing succès orientationIndex=" + i + ", base=" + baseLabel);
                        return text;
                    }
                } catch (NotFoundException e) {
                    Log.d(TAG, "ZXing échec orientationIndex=" + i + ", base=" + baseLabel + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }

                // Tenter en inversé
                if (src.invert() != null) {
                    try {
                        String textInv = decodeOnce(src.invert());
                        if (textInv != null) {
                            Log.d(TAG, "ZXing succès (inversé) orientationIndex=" + i + ", base=" + baseLabel);
                            return textInv;
                        }
                    } catch (NotFoundException e) {
                        Log.d(TAG, "ZXing échec (inversé) orientationIndex=" + i + ", base=" + baseLabel + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                    }
                }
            }
        }

        throw NotFoundException.getNotFoundInstance();
    }

    private String decodeOnce(LuminanceSource source) throws NotFoundException {
        // 1) Essai HybridBinarizer (par défaut, généralement meilleur)
        BinaryBitmap hybrid = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = zxingReader.decode(hybrid);
            return result != null ? result.getText() : null;
        } catch (NotFoundException first) {
            zxingReader.reset();
            // 2) Fallback GlobalHistogramBinarizer (peut aider en faible contraste)
            BinaryBitmap global = new BinaryBitmap(new com.google.zxing.common.GlobalHistogramBinarizer(source));
            try {
                Result result2 = zxingReader.decode(global);
                return result2 != null ? result2.getText() : null;
            } catch (NotFoundException second) {
                throw second;
            } finally {
                zxingReader.reset();
            }
        }
    }

    private void emitIfNotDuplicate(String type, String value) {
        long now = System.currentTimeMillis();
        if (value != null && value.equals(lastEmittedValue) && (now - lastEmittedAtMs) < DEBOUNCE_MS) {
            Log.d(TAG, "Résultat dupliqué ignoré: " + value);
            return;
        }
        lastEmittedValue = value;
        lastEmittedAtMs = now;
        listener.onBarcodeDetected(type, value);
    }

    private static int clampToInt(float v, int min, int max) {
        int i = Math.round(v);
        if (i < min) return min;
        if (i > max) return max;
        return i;
    }

    public interface RoiProvider {
        RectF getRoiFraction();
    }

    public void close() {
        if (mlKitScanner != null) {
            mlKitScanner.close();
        }
    }
}