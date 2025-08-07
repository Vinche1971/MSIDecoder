package com.example.msidecoder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.msidecoder.scanner.BarcodeAnalyzer;
import com.example.msidecoder.scanner.BarcodeResultListener;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements BarcodeResultListener {

    private static final String TAG = "MSIDecoder";
    
    private PreviewView previewView;
    private TextView statusText;
    private TextView barcodeTypeText;
    private TextView barcodeValueText;
    private TextView phaseInfoText;
    
    private ProcessCameraProvider cameraProvider;
    private BarcodeAnalyzer barcodeAnalyzer;
    private ExecutorService executor;

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    showPermissionDeniedMessage();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        executor = Executors.newSingleThreadExecutor();
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        barcodeTypeText = findViewById(R.id.barcodeTypeText);
        barcodeValueText = findViewById(R.id.barcodeValueText);
        phaseInfoText = findViewById(R.id.phaseInfoText);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Erreur d'initialisation caméra: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        // Unbind any previous use cases
        cameraProvider.unbindAll();

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis use case for barcode scanning
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        barcodeAnalyzer = new BarcodeAnalyzer(this);
        imageAnalysis.setAnalyzer(executor, barcodeAnalyzer);

        // Camera selector - back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Toast.makeText(this, "Erreur de liaison caméra: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPermissionDeniedMessage() {
        statusText.setText(R.string.camera_permission_required);
        Toast.makeText(this, R.string.grant_camera_permission, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBarcodeDetected(String type, String value) {
        runOnUiThread(() -> {
            statusText.setText(R.string.barcode_detected);
            barcodeTypeText.setText("Type: " + type);
            barcodeValueText.setText("Valeur: " + value);
            
            barcodeTypeText.setVisibility(TextView.VISIBLE);
            barcodeValueText.setVisibility(TextView.VISIBLE);
        });
    }

    @Override
    public void onNoBarcodeDetected() {
        runOnUiThread(() -> {
            statusText.setText(R.string.no_barcode_detected);
            barcodeTypeText.setVisibility(TextView.GONE);
            barcodeValueText.setVisibility(TextView.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}