package com.example.msidecoder.scanner;

public interface BarcodeResultListener {
    void onBarcodeDetected(String type, String value);
    void onNoBarcodeDetected();
}