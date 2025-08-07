package com.example.msidecoder.scanner;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "BarcodeAnalyzer";
    
    private final BarcodeResultListener listener;
    private final BarcodeScanner scanner;
    
    private boolean isProcessing = false;

    public BarcodeAnalyzer(BarcodeResultListener listener) {
        this.listener = listener;
        
        // Configure MLKit barcode scanner options
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
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
        
        this.scanner = BarcodeScanning.getClient(options);
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        isProcessing = true;
        
        @SuppressLint("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), 
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        // Process first detected barcode
                        Barcode barcode = barcodes.get(0);
                        String type = getBarcodeTypeName(barcode.getFormat());
                        String value = barcode.getDisplayValue();
                        
                        Log.d(TAG, "Barcode détecté - Type: " + type + ", Valeur: " + value);
                        
                        if (value != null && !value.isEmpty()) {
                            listener.onBarcodeDetected(type, value);
                        } else {
                            listener.onNoBarcodeDetected();
                        }
                    } else {
                        listener.onNoBarcodeDetected();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur de scan: " + e.getMessage());
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

    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}