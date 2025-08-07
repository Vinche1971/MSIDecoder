package com.example.msidecoder.models;

public class BarcodeResult {
    private String type;
    private String value;
    private String source; // MLKit, ZXing, or Custom MSI
    private boolean isValid;
    private String errorMessage;
    
    public BarcodeResult(String type, String value, String source) {
        this.type = type;
        this.value = value;
        this.source = source;
        this.isValid = true;
        this.errorMessage = null;
    }
    
    public BarcodeResult(String errorMessage) {
        this.type = null;
        this.value = null;
        this.source = null;
        this.isValid = false;
        this.errorMessage = errorMessage;
    }

    // Getters
    public String getType() { return type; }
    public String getValue() { return value; }
    public String getSource() { return source; }
    public boolean isValid() { return isValid; }
    public String getErrorMessage() { return errorMessage; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setValue(String value) { this.value = value; }
    public void setSource(String source) { this.source = source; }
    public void setValid(boolean valid) { this.isValid = valid; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        if (!isValid) {
            return "BarcodeResult{error='" + errorMessage + "'}";
        }
        return "BarcodeResult{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}