# CLAUDE.md - MSI Decoder Project Technical Context

## Project Overview
Android app to decode MSI (MSI Plessey) barcodes by adapting ZXing's Code39 algorithm. Three-phase implementation approach using MLKit for detection and custom ZXing-based decoder for MSI recognition.

## Current Status
- Project is in initial setup phase
- Documentation created, no Android code yet
- Ready to begin Phase 1 implementation

## Development Commands
### Build Commands
```bash
# Android Gradle build
./gradlew build
./gradlew assembleDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

### Lint and Quality
```bash
# Android lint
./gradlew lint

# Static analysis
./gradlew check
```

## Project Structure
```
MSIDecoder/
├── README.md              # Main project documentation
├── CLAUDE.md              # This file - technical context for Claude
├── DOCS/                  # Research documentation
│   └── ReadmeFirst.txt    # MSI vs Code39 specs, patterns, references
├── app/                   # Android application (to be created)
├── build.gradle           # Project dependencies (to be created)
└── settings.gradle        # Project settings (to be created)
```

## Technical Implementation Details

### Key Dependencies to Add
- `com.google.mlkit:barcode-scanning` - MLKit barcode scanning
- `com.journeyapps:zxing-android-embedded` - ZXing integration
- `androidx.camera:camera-*` - Camera functionality

### Critical Code References
1. **ZXing Code39Reader**: `https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/oned/Code39Reader.java`
2. **MSI Reference Implementation**: `https://github.com/barnhill/barcode-java/blob/main/src/main/java/com/pnuema/java/barcode/symbologies/MSI.java`

### MSI Decoder Specifications
- **Character set**: Numeric only (0-9)
- **Structure**: 4 bars + 4 spaces (8 elements per digit)
- **Encoding**: Binary representation (bars=1, spaces=0)
- **Format**: 1 prefix bit + 4 data bits + 2 suffix bits (0)
- **Checksum options**: Mod10 (Luhn), Mod11 IBM, Double checksum

### MSI vs Code39 Key Differences
| Aspect | MSI | Code39 |
|--------|-----|--------|
| Elements per char | 8 (4 bars + 4 spaces) | 9 (5 bars + 4 spaces) |
| Encoding method | Binary patterns | Narrow/wide patterns |
| Character set | 0-9 only | Alphanumeric + symbols |
| Start/Stop | MSI-specific symbols | Asterisk (*) |

### Phase Implementation Strategy

#### Phase 1: MLKit Basic Scanning
**Goal**: Validate MLKit integration and basic barcode detection
**Files to create**:
- `MainActivity.java` - Main activity with camera preview
- `BarcodeAnalyzer.java` - MLKit analyzer
- `activity_main.xml` - Layout with camera preview
- `AndroidManifest.xml` - Permissions and activities

#### Phase 2: ZXing Code39 Integration
**Goal**: Prove hybrid MLKit→ZXing pipeline works
**Files to create**:
- `ZXingBridge.java` - Convert MLKit image to ZXing format
- `Code39Processor.java` - Handle Code39 specifically
**Key logic**: Extract barcode region from MLKit, pass to ZXing Code39Reader

#### Phase 3: MSI Custom Decoder
**Goal**: Replace Code39 patterns with MSI patterns
**Files to create**:
- `MSIDecoder.java` - Custom MSI decoder based on Code39Reader
- `MSIPatterns.java` - MSI-specific pattern definitions
- `MSIChecksum.java` - Checksum algorithms (Mod10, Mod11)
**Key adaptation**: Replace Code39's CHARACTER_ENCODINGS with MSI patterns

### Pattern Tables to Implement

#### MSI Pattern Encodings (from reference)
```java
// MSI digit patterns (binary representation)
private static final String[] MSI_PATTERNS = {
    "100100100100",  // 0
    "100100100110",  // 1
    "100100110100",  // 2
    "100100110110",  // 3
    "100110100100",  // 4
    "100110100110",  // 5
    "100110110100",  // 6
    "100110110110",  // 7
    "110100100100",  // 8
    "110100100110"   // 9
};
```

#### Code39 Reference (for comparison)
```java
// From ZXing Code39Reader - narrow/wide pattern encoding
private static final int[] CHARACTER_ENCODINGS = {
    0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, // 0-9
    // ... more patterns for A-Z and symbols
};
```

## Development Notes

### Android Permissions Required
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

### Testing Strategy
1. **Phase 1**: Test with standard barcodes (QR, Code128) to validate MLKit setup
2. **Phase 2**: Test with Code39 samples to validate ZXing bridge
3. **Phase 3**: Test with MSI samples, validate checksum calculations

### Known Challenges
1. **Image preprocessing**: MLKit provides different image formats than ZXing expects
2. **Pattern matching**: MSI uses different bar/space ratios than Code39
3. **Checksum validation**: MSI checksums are more complex than Code39
4. **Performance**: Dual-processing pipeline (MLKit + custom decoder) may impact speed

### Next Steps
1. Create Android project structure with proper Gradle configuration
2. Implement Phase 1: Basic MLKit scanning with standard barcodes
3. Add ZXing dependency and create bridge functionality
4. Adapt Code39Reader algorithm for MSI patterns

## Useful Commands for Development
```bash
# Create new Android project (if using command line)
# Or use Android Studio "Create New Project" with API level 21+

# Test on device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -s MSIDecoder

# Performance profiling
./gradlew :app:assembleDebug
# Use Android Studio Profiler for camera/image processing optimization
```