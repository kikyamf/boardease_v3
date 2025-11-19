<?php
// insert_registration.php

// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', 'php_errors.log');

// Set content type to JSON
header('Content-Type: application/json');

// Set up error handler for fatal errors
register_shutdown_function(function() {
    $error = error_get_last();
    if ($error !== null && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR, E_USER_ERROR])) {
        error_log("FATAL ERROR: " . $error['message'] . " in " . $error['file'] . " on line " . $error['line']);
        $response = array(
            "success" => false,
            "message" => "Fatal error: " . $error['message']
        );
        echo json_encode($response);
        exit;
    }
});

// Include email configuration
error_log("Including email_config.php...");
require_once 'email_config.php';
error_log("email_config.php included successfully");

// Log the request for debugging
error_log("Registration request received at " . date('Y-m-d H:i:s'));
error_log("POST data: " . print_r($_POST, true));
error_log("FILES data: " . print_r($_FILES, true));
error_log("BirthDate received: '" . ($_POST['birthDate'] ?? 'NOT_SET') . "'");
error_log("Request method: " . $_SERVER['REQUEST_METHOD']);
error_log("Content type: " . ($_SERVER['CONTENT_TYPE'] ?? 'NOT_SET'));
error_log("Content length: " . ($_SERVER['CONTENT_LENGTH'] ?? 'NOT_SET'));

// Wrap everything in try-catch to handle fatal errors
try {
    error_log("Starting registration process...");
// Database connection
$servername = "localhost";
$username   = "boardease"; // adjust if needed
$password   = "boardease";     // adjust if needed
$dbname     = "boardease2"; // adjust if needed

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    error_log("Database connection failed: " . $conn->connect_error);
    $response = array(
        "success" => false,
        "message" => "Database connection failed: " . $conn->connect_error
    );
    echo json_encode($response);
    exit;
}

error_log("Database connected successfully");

// Collect POST data
$role       = $_POST['role'] ?? null;
$firstName  = $_POST['firstName'] ?? null;
$middleName = $_POST['middleName'] ?? null;
$lastName   = $_POST['lastName'] ?? null;
$suffix     = $_POST['suffix'] ?? null;
// Convert "None" to NULL for database storage
if ($suffix === "None" || $suffix === "none") {
    error_log("Converting suffix 'None' to NULL for database storage");
    $suffix = null;
}
$birthDate  = $_POST['birthDate'] ?? null;

// Validate and format birthdate
if (!empty($birthDate)) {
    // Try to parse the date and convert to MySQL format (YYYY-MM-DD)
    $parsedDate = DateTime::createFromFormat('m/d/Y', $birthDate);
    if (!$parsedDate) {
        $parsedDate = DateTime::createFromFormat('Y-m-d', $birthDate);
    }
    if (!$parsedDate) {
        $parsedDate = DateTime::createFromFormat('d/m/Y', $birthDate);
    }
    if (!$parsedDate) {
        $parsedDate = DateTime::createFromFormat('m-d-Y', $birthDate);
    }
    if (!$parsedDate) {
        $parsedDate = DateTime::createFromFormat('d-m-Y', $birthDate);
    }
    if (!$parsedDate) {
        // Try to parse as a general date
        $parsedDate = new DateTime($birthDate);
    }
    
    if ($parsedDate) {
        $birthDate = $parsedDate->format('Y-m-d');
        error_log("Formatted birthdate: " . $birthDate);
    } else {
        error_log("Invalid birthdate format: " . $birthDate);
        $birthDate = null; // Set to null if format is invalid
    }
} else {
    error_log("Birthdate is empty or null");
}
$phone      = $_POST['phone'] ?? null;
$address    = $_POST['address'] ?? null;
$email      = $_POST['email'] ?? null;
$password   = $_POST['password'] ?? null;
$gcashNum   = $_POST['gcashNum'] ?? null;
$idType     = $_POST['idType'] ?? null;
$idNumber   = $_POST['idNumber'] ?? null;
$isAgreed   = $_POST['isAgreed'] ?? "0";

// Validate required fields
if (!$firstName || !$lastName || !$email || !$password) {
    $response = array(
        "success" => false,
        "message" => "Error: Missing required fields."
    );
    echo json_encode($response);
    exit;
}

// Hash the password for security
$hashedPassword = password_hash($password, PASSWORD_DEFAULT);

// Handle file uploads
$uploadDir = "uploads/registrations/"; // make sure this folder exists and is writable

if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

function validateGcashQrCode($filePath) {
    try {
        error_log("=== QR CODE VALIDATION STARTED ===");
        error_log("File path: " . $filePath);
        
        // SIMPLE BUT EFFECTIVE: Only accept images that APIs can decode
        
        // Check if file exists
        if (!file_exists($filePath)) {
            error_log("QR VALIDATION FAILED: File not found");
            return array('isValid' => false, 'reason' => 'QR code file not found');
        }
        
        // Get image information
        $imageInfo = getimagesize($filePath);
        if (!$imageInfo) {
            error_log("QR VALIDATION FAILED: Invalid image format");
            return array('isValid' => false, 'reason' => 'Invalid image format');
        }
        
        $width = $imageInfo[0];
        $height = $imageInfo[1];
        $mimeType = $imageInfo['mime'];
        
        error_log("Image info - Width: $width, Height: $height, MIME: $mimeType");
        
        // Basic size requirements
        if ($width < 100 || $height < 100) {
            error_log("QR VALIDATION FAILED: Image too small");
            return array('isValid' => false, 'reason' => 'Image too small for QR code (minimum 100x100 pixels)');
        }
        
        if ($width > 5000 || $height > 5000) {
            error_log("QR VALIDATION FAILED: Image too large");
            return array('isValid' => false, 'reason' => 'Image too large for QR code (maximum 5000x5000 pixels)');
        }
        
        // Check file size
        $fileSize = filesize($filePath);
        if ($fileSize > 10 * 1024 * 1024) { // 10MB limit
            error_log("QR VALIDATION FAILED: File too large");
            return array('isValid' => false, 'reason' => 'Image file too large (maximum 10MB)');
        }
        
        error_log("File size: " . ($fileSize / 1024) . " KB");
        
        // STEP 1: Try ALL APIs and count successful decodes
        $apiResults = array();
        $successfulApis = 0;
        $validContentFound = false;
        
        error_log("=== STARTING API TESTS ===");
        
        // Try QR Server API
        error_log("Testing QR Server API...");
        $result1 = tryQrServerAPI($filePath);
        $apiResults[] = $result1;
        if ($result1['hasQrCode']) {
            $successfulApis++;
            error_log("QR Server API SUCCESS: " . $result1['qrCodeContent']);
        } else {
            error_log("QR Server API FAILED: " . ($result1['reason'] ?? 'Unknown error'));
        }
        
        // Try QuickChart API
        error_log("Testing QuickChart API...");
        $result2 = tryQuickChartAPI($filePath);
        $apiResults[] = $result2;
        if ($result2['hasQrCode']) {
            $successfulApis++;
            error_log("QuickChart API SUCCESS: " . $result2['qrCodeContent']);
        } else {
            error_log("QuickChart API FAILED: " . ($result2['reason'] ?? 'Unknown error'));
        }
        
        // Try ZXing API
        error_log("Testing ZXing API...");
        $result3 = tryZxingAPI($filePath);
        $apiResults[] = $result3;
        if ($result3['hasQrCode']) {
            $successfulApis++;
            error_log("ZXing API SUCCESS: " . $result3['qrCodeContent']);
        } else {
            error_log("ZXing API FAILED: " . ($result3['reason'] ?? 'Unknown error'));
        }
        
        error_log("TOTAL SUCCESSFUL APIs: " . $successfulApis . " out of 3");
        
        // STEP 2: BALANCED REQUIREMENT - At least 1 API must succeed
        if ($successfulApis < 1) {
            error_log("QR VALIDATION FAILED: No APIs succeeded ($successfulApis/3)");
            return array('isValid' => false, 'reason' => 'No QR detection APIs succeeded - image does not contain a valid QR code');
        }
        
        error_log("API requirement passed: $successfulApis APIs succeeded");
        
        // STEP 3: Check content from successful APIs
        error_log("=== CHECKING CONTENT VALIDATION ===");
        foreach ($apiResults as $index => $result) {
            if ($result['hasQrCode'] && isset($result['qrCodeContent']) && !empty($result['qrCodeContent'])) {
                error_log("Checking content from API " . ($index + 1) . ": " . $result['qrCodeContent']);
                
                $contentLower = strtolower($result['qrCodeContent']);
                
                // Check for payment-related keywords
                $paymentKeywords = array('gcash', 'paymaya', 'payment', 'pay', 'qr', 'transfer', 'send', 'money', 'wallet', 'cash', 'bank');
                $hasPaymentKeyword = false;
                foreach ($paymentKeywords as $keyword) {
                    if (strpos($contentLower, $keyword) !== false) {
                        $hasPaymentKeyword = true;
                        error_log("Found payment keyword: $keyword");
                        break;
                    }
                }
                
                // Check if it's a valid URL
                $isUrl = filter_var($result['qrCodeContent'], FILTER_VALIDATE_URL) !== false;
                if ($isUrl) {
                    error_log("Content is a valid URL");
                }
                
                // Check if it's JSON
                $isJson = (json_decode($result['qrCodeContent']) !== null);
                if ($isJson) {
                    error_log("Content is valid JSON");
                }
                
                // Accept any QR code content (more lenient validation)
                if (!empty($result['qrCodeContent'])) {
                    $validContentFound = true;
                    error_log("VALID CONTENT FOUND: " . $result['qrCodeContent']);
                    break;
                } else {
                    error_log("Content validation failed for API " . ($index + 1) . " - empty content");
                }
            }
        }
        
        if (!$validContentFound) {
            error_log("QR VALIDATION FAILED: No valid content found");
            return array('isValid' => false, 'reason' => 'QR code content could not be read or is empty');
        }
        
        // STEP 4: Final check - ensure it's not a false positive
        // If we reach here, at least 2 APIs successfully decoded the image
        // and the content is valid, so it's a real QR code
        
        error_log("QR VALIDATION SUCCESS: Valid QR code detected and verified by $successfulApis APIs");
        error_log("=== QR CODE VALIDATION COMPLETED ===");
        
        return array('isValid' => true, 'reason' => 'Valid QR code detected and verified by ' . $successfulApis . ' APIs');
        
    } catch (Exception $e) {
        error_log("QR code validation error: " . $e->getMessage());
        error_log("QR VALIDATION FAILED: Exception occurred");
        return array('isValid' => false, 'reason' => 'QR code validation failed');
    }
}

function ultraStrictPreCheck($filePath) {
    try {
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isQrCandidate' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isQrCandidate' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // ULTRA-AGGRESSIVE checks - reject ANY non-QR characteristics
        
        // Check 1: Detect ANY faces (reject selfies/photos)
        if (detectAnyFaces($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image contains faces - not a QR code');
        }
        
        // Check 2: Detect ANY text patterns (reject documents)
        if (detectAnyText($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image contains text - not a QR code');
        }
        
        // Check 3: Detect ANY photo-like patterns (reject photos)
        if (detectAnyPhotoPatterns($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image appears to be a photo - not a QR code');
        }
        
        // Check 4: Detect ANY uniform areas (reject documents/photos)
        if (detectAnyUniformAreas($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image has uniform areas - not a QR code');
        }
        
        // Check 5: Detect ANY gradual color changes (reject photos)
        if (detectAnyGradualChanges($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image has gradual color changes - not a QR code');
        }
        
        // Check 6: Must have QR code characteristics
        if (!hasQrCodeCharacteristics($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image does not have QR code characteristics');
        }
        
        imagedestroy($image);
        
        return array('isQrCandidate' => true, 'reason' => 'Image passed ultra-strict pre-check');
        
    } catch (Exception $e) {
        return array('isQrCandidate' => false, 'reason' => 'Ultra-strict pre-check failed');
    }
}

function detectAnyFaces($image, $width, $height) {
    try {
        $skinTonePixels = 0;
        $totalPixels = 0;
        
        // Sample every 3rd pixel for better detection
        for ($y = 0; $y < $height; $y += 3) {
            for ($x = 0; $x < $width; $x += 3) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                // More sensitive skin tone detection
                if ($red > 80 && $green > 30 && $blue > 15 && 
                    $red > $green && $green > $blue && 
                    ($red - $green) > 10 && ($green - $blue) > 10) {
                    $skinTonePixels++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $skinToneRatio = $skinTonePixels / $totalPixels;
        
        // ULTRA-SENSITIVE: If more than 5% of pixels are skin tone, reject
        return $skinToneRatio > 0.05;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectAnyText($image, $width, $height) {
    try {
        $textPatterns = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 40; // Smaller patterns for better detection
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasAnyTextPattern($image, $x, $y, $checkSize, $width, $height)) {
                    $textPatterns++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // ULTRA-SENSITIVE: If more than 10% of areas have text patterns, reject
        return ($textPatterns / $totalChecks) > 0.1;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasAnyTextPattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $edgeCount = 0;
        $totalPixels = 0;
        
        // Check for ANY text-like patterns
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // Check surrounding pixels for ANY contrast
                if ($x > 0 && $x < $width-1 && $y > 0 && $y < $height-1) {
                    $leftRgb = imagecolorat($image, $x-1, $y);
                    $rightRgb = imagecolorat($image, $x+1, $y);
                    $topRgb = imagecolorat($image, $x, $y-1);
                    $bottomRgb = imagecolorat($image, $x, $y+1);
                    
                    $leftBrightness = (($leftRgb >> 16) & 0xFF + ($leftRgb >> 8) & 0xFF + $leftRgb & 0xFF) / 3;
                    $rightBrightness = (($rightRgb >> 16) & 0xFF + ($rightRgb >> 8) & 0xFF + $rightRgb & 0xFF) / 3;
                    $topBrightness = (($topRgb >> 16) & 0xFF + ($topRgb >> 8) & 0xFF + $topRgb & 0xFF) / 3;
                    $bottomBrightness = (($bottomRgb >> 16) & 0xFF + ($bottomRgb >> 8) & 0xFF + $bottomRgb & 0xFF) / 3;
                    
                    if (abs($brightness - $leftBrightness) > 40 || 
                        abs($brightness - $rightBrightness) > 40 ||
                        abs($brightness - $topBrightness) > 40 ||
                        abs($brightness - $bottomBrightness) > 40) {
                        $edgeCount++;
                    }
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // ULTRA-SENSITIVE: If more than 20% of pixels have edges, reject
        return ($edgeCount / $totalPixels) > 0.2;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectAnyPhotoPatterns($image, $width, $height) {
    try {
        $gradualChanges = 0;
        $totalChecks = 0;
        
        // Check for ANY gradual color changes
        for ($y = 0; $y < $height - 1; $y += 2) {
            for ($x = 0; $x < $width - 1; $x += 2) {
                $rgb1 = imagecolorat($image, $x, $y);
                $rgb2 = imagecolorat($image, $x+1, $y);
                $rgb3 = imagecolorat($image, $x, $y+1);
                
                $brightness1 = (($rgb1 >> 16) & 0xFF + ($rgb1 >> 8) & 0xFF + $rgb1 & 0xFF) / 3;
                $brightness2 = (($rgb2 >> 16) & 0xFF + ($rgb2 >> 8) & 0xFF + $rgb2 & 0xFF) / 3;
                $brightness3 = (($rgb3 >> 16) & 0xFF + ($rgb3 >> 8) & 0xFF + $rgb3 & 0xFF) / 3;
                
                // ULTRA-SENSITIVE: Check for ANY gradual changes
                if (abs($brightness1 - $brightness2) < 15 && abs($brightness1 - $brightness3) < 15) {
                    $gradualChanges++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // ULTRA-SENSITIVE: If more than 50% of pixels have gradual changes, reject
        return ($gradualChanges / $totalChecks) > 0.5;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectAnyUniformAreas($image, $width, $height) {
    try {
        $uniformAreas = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 15; // Larger areas for better detection
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasAnyUniformArea($image, $x, $y, $checkSize, $width, $height)) {
                    $uniformAreas++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // ULTRA-SENSITIVE: If more than 20% of areas are uniform, reject
        return ($uniformAreas / $totalChecks) > 0.2;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasAnyUniformArea($image, $startX, $startY, $size, $width, $height) {
    try {
        $brightnessSum = 0;
        $pixelCount = 0;
        
        // Calculate average brightness
        for ($y = $startY; $y < min($startY + $size, $height); $y += 1) {
            for ($x = $startX; $x < min($startX + $size, $width); $x += 1) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                $brightnessSum += $brightness;
                $pixelCount++;
            }
        }
        
        if ($pixelCount == 0) return false;
        
        $avgBrightness = $brightnessSum / $pixelCount;
        
        // Check variance
        $variance = 0;
        for ($y = $startY; $y < min($startY + $size, $height); $y += 1) {
            for ($x = $startX; $x < min($startX + $size, $width); $x += 1) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                $variance += pow($brightness - $avgBrightness, 2);
            }
        }
        
        $variance = $variance / $pixelCount;
        
        // ULTRA-SENSITIVE: If variance is low, reject
        return $variance < 200;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectAnyGradualChanges($image, $width, $height) {
    try {
        $gradualCount = 0;
        $totalPixels = 0;
        
        // Check for ANY gradual changes
        for ($y = 0; $y < $height - 1; $y += 1) {
            for ($x = 0; $x < $width - 1; $x += 1) {
                $rgb1 = imagecolorat($image, $x, $y);
                $rgb2 = imagecolorat($image, $x+1, $y);
                
                $brightness1 = (($rgb1 >> 16) & 0xFF + ($rgb1 >> 8) & 0xFF + $rgb1 & 0xFF) / 3;
                $brightness2 = (($rgb2 >> 16) & 0xFF + ($rgb2 >> 8) & 0xFF + $rgb2 & 0xFF) / 3;
                
                // ULTRA-SENSITIVE: Check for ANY gradual changes
                if (abs($brightness1 - $brightness2) < 10) {
                    $gradualCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // ULTRA-SENSITIVE: If more than 60% of pixels have gradual changes, reject
        return ($gradualCount / $totalPixels) > 0.6;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasQrCodeCharacteristics($image, $width, $height) {
    try {
        $qrCharacteristics = 0;
        
        // Check 1: Must have high contrast
        if (hasUltraHighContrast($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check 2: Must have square patterns
        if (hasUltraSquarePatterns($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check 3: Must have corner markers
        if (hasUltraCornerMarkers($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check 4: Must have timing patterns
        if (hasUltraTimingPatterns($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // ULTRA-STRICT: Must have ALL 4 characteristics
        return $qrCharacteristics == 4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraHighContrast($image, $width, $height) {
    try {
        $highContrastCount = 0;
        $totalSamples = 0;
        
        // Sample every 2nd pixel
        for ($y = 0; $y < $height; $y += 2) {
            for ($x = 0; $x < $width; $x += 2) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // ULTRA-STRICT: Only pure black and pure white count
                if ($brightness < 20 || $brightness > 235) {
                    $highContrastCount++;
                }
                $totalSamples++;
            }
        }
        
        if ($totalSamples == 0) return false;
        
        // ULTRA-STRICT: Must have at least 40% high contrast
        return ($highContrastCount / $totalSamples) > 0.4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraSquarePatterns($image, $width, $height) {
    try {
        $squareCount = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 30;
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (isUltraSquarePattern($image, $x, $y, $checkSize, $width, $height)) {
                    $squareCount++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // ULTRA-STRICT: Must have at least 15% square patterns
        return ($squareCount / $totalChecks) > 0.15;
        
    } catch (Exception $e) {
        return false;
    }
}

function isUltraSquarePattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Check a small square area
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // ULTRA-STRICT: Only pure black and pure white count
                if ($brightness < 20) {
                    $blackCount++;
                } else if ($brightness > 235) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // ULTRA-STRICT: Must be mostly black or mostly white
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        return $blackRatio > 0.9 || $whiteRatio > 0.9;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraCornerMarkers($image, $width, $height) {
    try {
        $markerSize = min($width, $height) / 6;
        $markersFound = 0;
        
        // Check ALL three corners
        $corners = array(
            array(0, 0), // top-left
            array($width - $markerSize, 0), // top-right
            array(0, $height - $markerSize) // bottom-left
        );
        
        foreach ($corners as $corner) {
            if (hasUltraCornerMarker($image, $corner[0], $corner[1], $markerSize, $width, $height)) {
                $markersFound++;
            }
        }
        
        // ULTRA-STRICT: Must have ALL 3 corner markers
        return $markersFound == 3;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraCornerMarker($image, $startX, $startY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Sample every pixel
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // ULTRA-STRICT: Only pure black and pure white count
                if ($brightness < 20) {
                    $blackCount++;
                } else if ($brightness > 235) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        // ULTRA-STRICT: Must have significant black and white areas
        return $blackRatio > 0.4 && $whiteRatio > 0.4 && ($blackRatio + $whiteRatio) > 0.8;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraTimingPatterns($image, $width, $height) {
    try {
        $timingPatterns = 0;
        $totalChecks = 0;
        
        // Check horizontal timing patterns
        $middleY = $height / 2;
        $timingStartX = min($width, $height) / 4;
        $timingEndX = $width - $timingStartX;
        
        if (hasUltraTimingPatternLine($image, $middleY, $timingStartX, $timingEndX, $width, $height, true)) {
            $timingPatterns++;
        }
        $totalChecks++;
        
        // Check vertical timing patterns
        $middleX = $width / 2;
        $timingStartY = min($width, $height) / 4;
        $timingEndY = $height - $timingStartY;
        
        if (hasUltraTimingPatternLine($image, $middleX, $timingStartY, $timingEndY, $width, $height, false)) {
            $timingPatterns++;
        }
        $totalChecks++;
        
        // ULTRA-STRICT: Must have at least 1 timing pattern
        return $timingPatterns >= 1;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUltraTimingPatternLine($image, $fixedCoord, $startCoord, $endCoord, $width, $height, $isHorizontal) {
    try {
        $alternations = 0;
        $prevBrightness = -1;
        
        if ($isHorizontal) {
            // Check horizontal line
            for ($x = $startCoord; $x < $endCoord; $x++) {
                $rgb = imagecolorat($image, $x, $fixedCoord);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($prevBrightness != -1) {
                    // ULTRA-STRICT: Check for alternating pattern
                    if (($prevBrightness < 20 && $brightness > 235) || ($prevBrightness > 235 && $brightness < 20)) {
                        $alternations++;
                    }
                }
                $prevBrightness = $brightness;
            }
        } else {
            // Check vertical line
            for ($y = $startCoord; $y < $endCoord; $y++) {
                $rgb = imagecolorat($image, $fixedCoord, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($prevBrightness != -1) {
                    if (($prevBrightness < 20 && $brightness > 235) || ($prevBrightness > 235 && $brightness < 20)) {
                        $alternations++;
                    }
                }
                $prevBrightness = $brightness;
            }
        }
        
        // ULTRA-STRICT: Must have many alternations
        $lineLength = $endCoord - $startCoord;
        return $alternations > ($lineLength * 0.4);
        
    } catch (Exception $e) {
        return false;
    }
}

function ultraStrictContentValidation($qrCodeContent) {
    try {
        if (empty($qrCodeContent)) {
            return array('isValid' => false, 'reason' => 'No QR code content detected');
        }
        
        // ULTRA-STRICT content validation
        $contentLower = strtolower($qrCodeContent);
        
        // Check 1: Must contain payment-related keywords
        $paymentKeywords = array('gcash', 'paymaya', 'payment', 'pay', 'qr', 'transfer', 'send', 'money', 'wallet', 'cash', 'bank');
        $hasPaymentKeyword = false;
        foreach ($paymentKeywords as $keyword) {
            if (strpos($contentLower, $keyword) !== false) {
                $hasPaymentKeyword = true;
                break;
            }
        }
        
        // Check 2: Must be a valid URL or contain structured data
        $isUrl = filter_var($qrCodeContent, FILTER_VALIDATE_URL) !== false;
        $isJson = (json_decode($qrCodeContent) !== null);
        
        // Check 3: Must not be random text or gibberish
        $isRandomText = preg_match('/^[a-zA-Z0-9]{1,20}$/', $qrCodeContent) && !$hasPaymentKeyword && !$isUrl && !$isJson;
        
        // Check 4: Must have reasonable length
        $hasReasonableLength = strlen($qrCodeContent) > 15 && strlen($qrCodeContent) < 500;
        
        // ULTRA-STRICT criteria: Must pass ALL checks
        if (!$hasPaymentKeyword && !$isUrl && !$isJson) {
            return array('isValid' => false, 'reason' => 'QR code content does not appear to be payment-related');
        }
        
        if ($isRandomText) {
            return array('isValid' => false, 'reason' => 'QR code content appears to be random text');
        }
        
        if (!$hasReasonableLength) {
            return array('isValid' => false, 'reason' => 'QR code content length is not reasonable');
        }
        
        return array('isValid' => true, 'reason' => 'Valid QR code content');
        
    } catch (Exception $e) {
        return array('isValid' => false, 'reason' => 'Content validation failed');
    }
}

function ultraStrictStructuralCheck($filePath) {
    try {
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isValid' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isValid' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // ULTRA-STRICT structural checks
        $structuralChecks = 0;
        
        // Check 1: Must have perfect QR code structure
        if (hasPerfectQrStructure($image, $width, $height)) {
            $structuralChecks++;
        }
        
        // Check 2: Must have data modules
        if (hasDataModules($image, $width, $height)) {
            $structuralChecks++;
        }
        
        // Check 3: Must have finder patterns
        if (hasFinderPatterns($image, $width, $height)) {
            $structuralChecks++;
        }
        
        imagedestroy($image);
        
        // ULTRA-STRICT: Must pass ALL structural checks
        if ($structuralChecks < 3) {
            return array('isValid' => false, 'reason' => 'Image does not have perfect QR code structure');
        }
        
        return array('isValid' => true, 'reason' => 'Perfect QR code structure verified');
        
    } catch (Exception $e) {
        return array('isValid' => false, 'reason' => 'Structural check failed');
    }
}

function hasPerfectQrStructure($image, $width, $height) {
    try {
        // Check for perfect QR code structure
        $structureScore = 0;
        
        // Check for high contrast
        if (hasUltraHighContrast($image, $width, $height)) {
            $structureScore++;
        }
        
        // Check for square patterns
        if (hasUltraSquarePatterns($image, $width, $height)) {
            $structureScore++;
        }
        
        // Check for corner markers
        if (hasUltraCornerMarkers($image, $width, $height)) {
            $structureScore++;
        }
        
        // Check for timing patterns
        if (hasUltraTimingPatterns($image, $width, $height)) {
            $structureScore++;
        }
        
        // ULTRA-STRICT: Must have ALL 4 structure elements
        return $structureScore == 4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasDataModules($image, $width, $height) {
    try {
        $moduleSize = min($width, $height) / 30;
        $modulesFound = 0;
        $totalChecks = 0;
        
        // Check for data modules throughout the image
        for ($y = $moduleSize; $y < $height - $moduleSize; $y += $moduleSize) {
            for ($x = $moduleSize; $x < $width - $moduleSize; $x += $moduleSize) {
                if (isDataModule($image, $x, $y, $moduleSize, $width, $height)) {
                    $modulesFound++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // ULTRA-STRICT: Must have many data modules
        return ($modulesFound / $totalChecks) > 0.25;
        
    } catch (Exception $e) {
        return false;
    }
}

function isDataModule($image, $centerX, $centerY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Check a small square area
        for ($y = $centerY - $size/2; $y < $centerY + $size/2; $y++) {
            for ($x = $centerX - $size/2; $x < $centerX + $size/2; $x++) {
                if ($x >= 0 && $x < $width && $y >= 0 && $y < $height) {
                    $rgb = imagecolorat($image, $x, $y);
                    $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                    
                    // ULTRA-STRICT: Only pure black and pure white count
                    if ($brightness < 20) {
                        $blackCount++;
                    } else if ($brightness > 235) {
                        $whiteCount++;
                    }
                    $totalPixels++;
                }
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // ULTRA-STRICT: Module should be mostly black or mostly white
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        return $blackRatio > 0.9 || $whiteRatio > 0.9;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasFinderPatterns($image, $width, $height) {
    try {
        // Check for finder patterns (corner squares with specific structure)
        $finderPatterns = 0;
        $markerSize = min($width, $height) / 6;
        
        // Check each corner for finder pattern structure
        $corners = array(
            array(0, 0), // top-left
            array($width - $markerSize, 0), // top-right
            array(0, $height - $markerSize) // bottom-left
        );
        
        foreach ($corners as $corner) {
            if (hasFinderPatternStructure($image, $corner[0], $corner[1], $markerSize, $width, $height)) {
                $finderPatterns++;
            }
        }
        
        // ULTRA-STRICT: Must have at least 2 finder patterns
        return $finderPatterns >= 2;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasFinderPatternStructure($image, $startX, $startY, $size, $width, $height) {
    try {
        // Finder pattern: 7x7 black, 5x5 white, 3x3 black
        $outerBlack = 0;
        $middleWhite = 0;
        $innerBlack = 0;
        $totalPixels = 0;
        
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                $relativeX = $x - $startX;
                $relativeY = $y - $startY;
                
                // ULTRA-STRICT: Only pure black and pure white count
                if ($brightness < 20) {
                    // Check which layer this black pixel belongs to
                    if ($relativeX < 2 || $relativeX >= $size-2 || $relativeY < 2 || $relativeY >= $size-2) {
                        $outerBlack++; // Outer black border
                    } else if ($relativeX >= 2 && $relativeX < $size-2 && $relativeY >= 2 && $relativeY < $size-2) {
                        if ($relativeX >= 3 && $relativeX < $size-3 && $relativeY >= 3 && $relativeY < $size-3) {
                            $innerBlack++; // Inner black square
                        }
                    }
                } else if ($brightness > 235) {
                    if ($relativeX >= 2 && $relativeX < $size-2 && $relativeY >= 2 && $relativeY < $size-2) {
                        if (!($relativeX >= 3 && $relativeX < $size-3 && $relativeY >= 3 && $relativeY < $size-3)) {
                            $middleWhite++; // Middle white area
                        }
                    }
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // ULTRA-STRICT: Check if we have the characteristic finder pattern ratios
        $outerBlackRatio = $outerBlack / $totalPixels;
        $middleWhiteRatio = $middleWhite / $totalPixels;
        $innerBlackRatio = $innerBlack / $totalPixels;
        
        return $outerBlackRatio > 0.15 && $middleWhiteRatio > 0.15 && $innerBlackRatio > 0.08;
        
    } catch (Exception $e) {
        return false;
    }
}

function finalUltraStrictCheck($filePath) {
    try {
        // Final ultra-strict verification
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isValid' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isValid' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // Final check: Ensure the image has PERFECT QR code characteristics
        $qrCharacteristics = 0;
        
        // Check for ultra-high contrast
        if (hasUltraHighContrast($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check for ultra-square patterns
        if (hasUltraSquarePatterns($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check for ultra-corner markers
        if (hasUltraCornerMarkers($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check for ultra-timing patterns
        if (hasUltraTimingPatterns($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        imagedestroy($image);
        
        // ULTRA-STRICT: Must have ALL 4 characteristics
        if ($qrCharacteristics < 4) {
            return array('isValid' => false, 'reason' => 'Image does not have perfect QR code characteristics');
        }
        
        return array('isValid' => true, 'reason' => 'Final ultra-strict verification passed');
        
    } catch (Exception $e) {
        return array('isValid' => false, 'reason' => 'Final verification failed');
    }
}

function preCheckForNonQrImages($filePath) {
    try {
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isQrCandidate' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isQrCandidate' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // Check 1: Detect faces (reject selfies/photos)
        if (detectFacesInImage($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image appears to contain faces (selfie/photo detected)');
        }
        
        // Check 2: Detect text patterns (reject documents)
        if (detectTextPatterns($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image appears to contain text (document detected)');
        }
        
        // Check 3: Detect photo-like patterns (reject regular photos)
        if (detectPhotoPatterns($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image appears to be a regular photo');
        }
        
        // Check 4: Detect ID-like patterns (reject ID cards)
        if (detectIdPatterns($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image appears to be an ID card or document');
        }
        
        // Check 5: Detect uniform backgrounds (reject documents/photos)
        if (detectUniformBackgrounds($image, $width, $height)) {
            imagedestroy($image);
            return array('isQrCandidate' => false, 'reason' => 'Image has uniform background (document/photo detected)');
        }
        
        imagedestroy($image);
        
        return array('isQrCandidate' => true, 'reason' => 'Image passed pre-check');
        
    } catch (Exception $e) {
        return array('isQrCandidate' => false, 'reason' => 'Pre-check failed');
    }
}

function detectFacesInImage($image, $width, $height) {
    try {
        // Simple face detection using skin tone detection
        $skinTonePixels = 0;
        $totalPixels = 0;
        
        // Sample every 5th pixel for performance
        for ($y = 0; $y < $height; $y += 5) {
            for ($x = 0; $x < $width; $x += 5) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                // Skin tone detection (simplified)
                if ($red > 95 && $green > 40 && $blue > 20 && 
                    $red > $green && $green > $blue && 
                    ($red - $green) > 15 && ($green - $blue) > 15) {
                    $skinTonePixels++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $skinToneRatio = $skinTonePixels / $totalPixels;
        
        // If more than 10% of pixels are skin tone, likely contains faces
        return $skinToneRatio > 0.1;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectTextPatterns($image, $width, $height) {
    try {
        $textPatterns = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 30; // Small patterns for text
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasTextPattern($image, $x, $y, $checkSize, $width, $height)) {
                    $textPatterns++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // Documents have many text patterns
        return ($textPatterns / $totalChecks) > 0.2;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasTextPattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $edgeCount = 0;
        $totalPixels = 0;
        
        // Check for many small edges (characteristic of text)
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // Check surrounding pixels for contrast
                if ($x > 0 && $x < $width-1 && $y > 0 && $y < $height-1) {
                    $leftRgb = imagecolorat($image, $x-1, $y);
                    $rightRgb = imagecolorat($image, $x+1, $y);
                    $topRgb = imagecolorat($image, $x, $y-1);
                    $bottomRgb = imagecolorat($image, $x, $y+1);
                    
                    $leftBrightness = (($leftRgb >> 16) & 0xFF + ($leftRgb >> 8) & 0xFF + $leftRgb & 0xFF) / 3;
                    $rightBrightness = (($rightRgb >> 16) & 0xFF + ($rightRgb >> 8) & 0xFF + $rightRgb & 0xFF) / 3;
                    $topBrightness = (($topRgb >> 16) & 0xFF + ($topRgb >> 8) & 0xFF + $topRgb & 0xFF) / 3;
                    $bottomBrightness = (($bottomRgb >> 16) & 0xFF + ($bottomRgb >> 8) & 0xFF + $bottomRgb & 0xFF) / 3;
                    
                    if (abs($brightness - $leftBrightness) > 60 || 
                        abs($brightness - $rightBrightness) > 60 ||
                        abs($brightness - $topBrightness) > 60 ||
                        abs($brightness - $bottomBrightness) > 60) {
                        $edgeCount++;
                    }
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // Text has many small edges
        return ($edgeCount / $totalPixels) > 0.3;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectPhotoPatterns($image, $width, $height) {
    try {
        $gradualChanges = 0;
        $totalChecks = 0;
        
        // Check for gradual color changes (characteristic of photos)
        for ($y = 0; $y < $height - 1; $y += 3) {
            for ($x = 0; $x < $width - 1; $x += 3) {
                $rgb1 = imagecolorat($image, $x, $y);
                $rgb2 = imagecolorat($image, $x+1, $y);
                $rgb3 = imagecolorat($image, $x, $y+1);
                
                $brightness1 = (($rgb1 >> 16) & 0xFF + ($rgb1 >> 8) & 0xFF + $rgb1 & 0xFF) / 3;
                $brightness2 = (($rgb2 >> 16) & 0xFF + ($rgb2 >> 8) & 0xFF + $rgb2 & 0xFF) / 3;
                $brightness3 = (($rgb3 >> 16) & 0xFF + ($rgb3 >> 8) & 0xFF + $rgb3 & 0xFF) / 3;
                
                // Check for gradual changes (not sharp QR code edges)
                if (abs($brightness1 - $brightness2) < 20 && abs($brightness1 - $brightness3) < 20) {
                    $gradualChanges++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // Photos have many gradual changes
        return ($gradualChanges / $totalChecks) > 0.7;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectIdPatterns($image, $width, $height) {
    try {
        // Check for ID card characteristics
        $idIndicators = 0;
        
        // Check 1: Rectangular shape with specific proportions
        $aspectRatio = $width / $height;
        if ($aspectRatio > 1.3 && $aspectRatio < 1.7) { // ID card proportions
            $idIndicators++;
        }
        
        // Check 2: Uniform border (ID cards often have borders)
        if (hasUniformBorder($image, $width, $height)) {
            $idIndicators++;
        }
        
        // Check 3: Photo area detection (ID cards have photo areas)
        if (hasPhotoArea($image, $width, $height)) {
            $idIndicators++;
        }
        
        // Check 4: Text areas (ID cards have text areas)
        if (hasTextAreas($image, $width, $height)) {
            $idIndicators++;
        }
        
        // If 2 or more ID indicators, likely an ID card
        return $idIndicators >= 2;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUniformBorder($image, $width, $height) {
    try {
        $borderSize = min($width, $height) / 20;
        $uniformPixels = 0;
        $totalPixels = 0;
        
        // Check top border
        for ($x = 0; $x < $width; $x++) {
            for ($y = 0; $y < $borderSize; $y++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                if ($brightness > 200) { // Very light border
                    $uniformPixels++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        return ($uniformPixels / $totalPixels) > 0.8;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasPhotoArea($image, $width, $height) {
    try {
        // Check for photo-like area (usually in top-right or top-left)
        $photoAreaSize = min($width, $height) / 4;
        
        // Check top-right area
        $photoPixels = 0;
        $totalPixels = 0;
        
        for ($y = 0; $y < $photoAreaSize; $y++) {
            for ($x = $width - $photoAreaSize; $x < $width; $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // Photo areas have moderate brightness (not pure black/white)
                if ($brightness > 50 && $brightness < 200) {
                    $photoPixels++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        return ($photoPixels / $totalPixels) > 0.6;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasTextAreas($image, $width, $height) {
    try {
        // Check for text areas (usually in bottom half)
        $textAreaSize = $width * $height / 4;
        $textPixels = 0;
        $totalPixels = 0;
        
        // Check bottom half
        for ($y = $height / 2; $y < $height; $y += 2) {
            for ($x = 0; $x < $width; $x += 2) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // Text areas have high contrast
                if ($brightness < 50 || $brightness > 205) {
                    $textPixels++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        return ($textPixels / $totalPixels) > 0.4;
        
    } catch (Exception $e) {
        return false;
    }
}

function validateQrCodeContentStrict($qrCodeContent) {
    try {
        if (empty($qrCodeContent)) {
            return array('isValid' => false, 'reason' => 'No QR code content detected');
        }
        
        // STRICT content validation
        $contentLower = strtolower($qrCodeContent);
        
        // Check 1: Must contain payment-related keywords
        $paymentKeywords = array('gcash', 'paymaya', 'payment', 'pay', 'qr', 'transfer', 'send', 'money', 'wallet');
        $hasPaymentKeyword = false;
        foreach ($paymentKeywords as $keyword) {
            if (strpos($contentLower, $keyword) !== false) {
                $hasPaymentKeyword = true;
                break;
            }
        }
        
        // Check 2: Must be a valid URL or contain structured data
        $isUrl = filter_var($qrCodeContent, FILTER_VALIDATE_URL) !== false;
        $isJson = (json_decode($qrCodeContent) !== null);
        
        // Check 3: Must not be random text or gibberish
        $isRandomText = preg_match('/^[a-zA-Z0-9]{1,20}$/', $qrCodeContent) && !$hasPaymentKeyword && !$isUrl && !$isJson;
        
        // Check 4: Must have reasonable length
        $hasReasonableLength = strlen($qrCodeContent) > 10 && strlen($qrCodeContent) < 1000;
        
        // STRICT criteria: Must pass ALL checks
        if (!$hasPaymentKeyword && !$isUrl && !$isJson) {
            return array('isValid' => false, 'reason' => 'QR code content does not appear to be payment-related');
        }
        
        if ($isRandomText) {
            return array('isValid' => false, 'reason' => 'QR code content appears to be random text');
        }
        
        if (!$hasReasonableLength) {
            return array('isValid' => false, 'reason' => 'QR code content length is not reasonable');
        }
        
        return array('isValid' => true, 'reason' => 'Valid QR code content');
        
    } catch (Exception $e) {
        return array('isValid' => false, 'reason' => 'Content validation failed');
    }
}

function finalQrVerification($filePath, $qrDetectionResult) {
    try {
        // Final verification to ensure it's not a false positive
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isValid' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isValid' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // Final check: Ensure the image has QR code characteristics
        $qrCharacteristics = 0;
        
        // Check for high contrast (QR codes have high contrast)
        if (hasHighContrast($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check for square patterns (QR codes have square patterns)
        if (hasSquarePatterns($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        // Check for corner markers (QR codes have corner markers)
        if (hasCornerMarkers($image, $width, $height)) {
            $qrCharacteristics++;
        }
        
        imagedestroy($image);
        
        // Must have at least 2 QR characteristics
        if ($qrCharacteristics < 2) {
            return array('isValid' => false, 'reason' => 'Image does not have QR code characteristics');
        }
        
        return array('isValid' => true, 'reason' => 'Final verification passed');
        
    } catch (Exception $e) {
        return array('isValid' => false, 'reason' => 'Final verification failed');
    }
}

function hasHighContrast($image, $width, $height) {
    try {
        $highContrastCount = 0;
        $totalSamples = 0;
        
        // Sample every 3rd pixel
        for ($y = 0; $y < $height; $y += 3) {
            for ($x = 0; $x < $width; $x += 3) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // QR codes have very high contrast
                if ($brightness < 30 || $brightness > 225) {
                    $highContrastCount++;
                }
                $totalSamples++;
            }
        }
        
        if ($totalSamples == 0) return false;
        
        return ($highContrastCount / $totalSamples) > 0.3;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasSquarePatterns($image, $width, $height) {
    try {
        $squareCount = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 25;
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (isSquarePattern($image, $x, $y, $checkSize, $width, $height)) {
                    $squareCount++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        return ($squareCount / $totalChecks) > 0.1;
        
    } catch (Exception $e) {
        return false;
    }
}

function isSquarePattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $edgeCount = 0;
        $totalEdges = 0;
        
        // Check edges of the square
        for ($x = $startX; $x < min($startX + $size, $width); $x++) {
            for ($y = $startY; $y < min($startY + $size, $height); $y++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($brightness < 50) {
                    $edgeCount++;
                }
                $totalEdges++;
            }
        }
        
        if ($totalEdges == 0) return false;
        
        return ($edgeCount / $totalEdges) > 0.4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasCornerMarkers($image, $width, $height) {
    try {
        $markerSize = min($width, $height) / 8;
        $markersFound = 0;
        
        // Check three corners
        $corners = array(
            array(0, 0), // top-left
            array($width - $markerSize, 0), // top-right
            array(0, $height - $markerSize) // bottom-left
        );
        
        foreach ($corners as $corner) {
            if (hasCornerMarker($image, $corner[0], $corner[1], $markerSize, $width, $height)) {
                $markersFound++;
            }
        }
        
        return $markersFound >= 2; // At least 2 corner markers
        
    } catch (Exception $e) {
        return false;
    }
}

function hasCornerMarker($image, $startX, $startY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($brightness < 50) {
                    $blackCount++;
                } else if ($brightness > 205) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        return $blackRatio > 0.2 && $whiteRatio > 0.2 && ($blackRatio + $whiteRatio) > 0.6;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectQrCodeWithAPI($filePath) {
    try {
        // Method 1: Try online QR code detection API first
        $onlineResult = detectQrCodeOnline($filePath);
        if ($onlineResult['hasQrCode']) {
            error_log("QR Code detected via online API: " . $onlineResult['qrCodeContent']);
            return $onlineResult;
        }
        
        // Method 2: Fallback to local detection
        error_log("Online API failed, trying local detection");
        $localResult = detectQrCodeLocally($filePath);
        if ($localResult['hasQrCode']) {
            error_log("QR Code detected via local detection: " . $localResult['qrCodeContent']);
            return $localResult;
        }
        
        // Method 3: Basic pattern detection as last resort
        error_log("Local detection failed, trying basic pattern detection");
        $patternResult = detectQrCodePatterns($filePath);
        return $patternResult;
        
    } catch (Exception $e) {
        error_log("QR code API detection error: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'QR code detection failed');
    }
}

function detectQrCodeOnline($filePath) {
    try {
        // Try multiple QR code detection APIs for better accuracy
        
        // API 1: QR Server API (Free)
        $result1 = tryQrServerAPI($filePath);
        if ($result1['hasQrCode']) {
            error_log("QR Server API successful: " . $result1['qrCodeContent']);
            return $result1;
        }
        
        // API 2: QuickChart QR Detection API
        $result2 = tryQuickChartAPI($filePath);
        if ($result2['hasQrCode']) {
            error_log("QuickChart API successful: " . $result2['qrCodeContent']);
            return $result2;
        }
        
        // API 3: ZXing Online API
        $result3 = tryZxingAPI($filePath);
        if ($result3['hasQrCode']) {
            error_log("ZXing API successful: " . $result3['qrCodeContent']);
            return $result3;
        }
        
        // API 4: Google Vision API (if available)
        $result4 = tryGoogleVisionAPI($filePath);
        if ($result4['hasQrCode']) {
            error_log("Google Vision API successful: " . $result4['qrCodeContent']);
            return $result4;
        }
        
        // API 5: Commercial QR Detection API (Arya.ai or similar)
        $result5 = tryCommercialQrAPI($filePath);
        if ($result5['hasQrCode']) {
            error_log("Commercial QR API successful: " . $result5['qrCodeContent']);
            return $result5;
        }
        
        // API 6: Browser-based Barcode Detection API simulation
        $result6 = tryBrowserBarcodeAPI($filePath);
        if ($result6['hasQrCode']) {
            error_log("Browser Barcode API successful: " . $result6['qrCodeContent']);
            return $result6;
        }
        
        error_log("All QR detection APIs failed");
        return array('hasQrCode' => false, 'reason' => 'All QR detection APIs failed');
        
    } catch (Exception $e) {
        error_log("Online QR detection error: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'Online API error');
    }
}

function tryQrServerAPI($filePath) {
    try {
        error_log("QR Server API: Starting request for " . $filePath);
        
        $apiUrl = 'https://api.qrserver.com/v1/read-qr-code/';
        
        $postData = array(
            'file' => new CURLFile($filePath, mime_content_type($filePath), basename($filePath))
        );
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        error_log("QR Server API: HTTP Code: $httpCode, Error: $error");
        error_log("QR Server API: Response: " . substr($response, 0, 200));
        
        if ($error) {
            error_log("QR Server API: cURL Error: $error");
            return array('hasQrCode' => false, 'reason' => "cURL Error: $error");
        }
        
        if ($httpCode != 200) {
            error_log("QR Server API: HTTP Error: $httpCode");
            return array('hasQrCode' => false, 'reason' => "HTTP Error: $httpCode");
        }
        
        if (!$response) {
            error_log("QR Server API: Empty response");
            return array('hasQrCode' => false, 'reason' => 'Empty response');
        }
        
        $result = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            error_log("QR Server API: JSON Error: " . json_last_error_msg());
            return array('hasQrCode' => false, 'reason' => 'JSON Error: ' . json_last_error_msg());
        }
        
        if (isset($result[0]['symbol'][0]['data'])) {
            $qrCodeContent = $result[0]['symbol'][0]['data'];
            error_log("QR Server API: SUCCESS - Content: $qrCodeContent");
            return array(
                'hasQrCode' => true,
                'qrCodeContent' => $qrCodeContent,
                'method' => 'qrserver_api'
            );
        } else {
            error_log("QR Server API: No QR code found in response");
            return array('hasQrCode' => false, 'reason' => 'No QR code found');
        }
        
    } catch (Exception $e) {
        error_log("QR Server API: Exception: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'Exception: ' . $e->getMessage());
    }
}

function tryQuickChartAPI($filePath) {
    try {
        // Convert image to base64 for QuickChart API
        $imageData = file_get_contents($filePath);
        $base64Image = base64_encode($imageData);
        $mimeType = mime_content_type($filePath);
        
        $apiUrl = 'https://quickchart.io/qr/decode';
        
        $postData = json_encode(array(
            'image' => 'data:' . $mimeType . ';base64,' . $base64Image
        ));
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode == 200 && $response) {
            $result = json_decode($response, true);
            
            if (isset($result['data']) && !empty($result['data'])) {
                return array(
                    'hasQrCode' => true,
                    'qrCodeContent' => $result['data'],
                    'method' => 'quickchart_api'
                );
            }
        }
        
        return array('hasQrCode' => false, 'reason' => 'QuickChart API failed');
        
    } catch (Exception $e) {
        return array('hasQrCode' => false, 'reason' => 'QuickChart API error');
    }
}

function tryZxingAPI($filePath) {
    try {
        // ZXing Web API
        $apiUrl = 'https://zxing.org/w/decode';
        
        $postData = array(
            'f' => new CURLFile($filePath, mime_content_type($filePath), basename($filePath))
        );
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode == 200 && $response) {
            // Parse ZXing response (it returns HTML with the decoded content)
            if (preg_match('/<pre[^>]*>(.*?)<\/pre>/s', $response, $matches)) {
                $qrCodeContent = trim($matches[1]);
                if (!empty($qrCodeContent)) {
                    return array(
                        'hasQrCode' => true,
                        'qrCodeContent' => $qrCodeContent,
                        'method' => 'zxing_api'
                    );
                }
            }
        }
        
        return array('hasQrCode' => false, 'reason' => 'ZXing API failed');
        
    } catch (Exception $e) {
        return array('hasQrCode' => false, 'reason' => 'ZXing API error');
    }
}

function tryGoogleVisionAPI($filePath) {
    try {
        // Google Vision API (requires API key - you can add your key here)
        $apiKey = 'YOUR_GOOGLE_VISION_API_KEY'; // Replace with actual API key
        
        if ($apiKey === 'YOUR_GOOGLE_VISION_API_KEY') {
            return array('hasQrCode' => false, 'reason' => 'Google Vision API key not configured');
        }
        
        $imageData = file_get_contents($filePath);
        $base64Image = base64_encode($imageData);
        
        $apiUrl = 'https://vision.googleapis.com/v1/images:annotate?key=' . $apiKey;
        
        $postData = json_encode(array(
            'requests' => array(
                array(
                    'image' => array(
                        'content' => $base64Image
                    ),
                    'features' => array(
                        array(
                            'type' => 'TEXT_DETECTION',
                            'maxResults' => 1
                        )
                    )
                )
            )
        ));
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode == 200 && $response) {
            $result = json_decode($response, true);
            
            if (isset($result['responses'][0]['textAnnotations'][0]['description'])) {
                $qrCodeContent = $result['responses'][0]['textAnnotations'][0]['description'];
                return array(
                    'hasQrCode' => true,
                    'qrCodeContent' => $qrCodeContent,
                    'method' => 'google_vision_api'
                );
            }
        }
        
        return array('hasQrCode' => false, 'reason' => 'Google Vision API failed');
        
    } catch (Exception $e) {
        return array('hasQrCode' => false, 'reason' => 'Google Vision API error');
    }
}

function tryCommercialQrAPI($filePath) {
    try {
        // Commercial QR Detection API (example with Arya.ai or similar service)
        // You can replace this with any commercial QR detection service
        
        $apiKey = 'YOUR_COMMERCIAL_API_KEY'; // Replace with actual API key
        $apiUrl = 'https://api.arya.ai/qr-detection'; // Replace with actual API endpoint
        
        if ($apiKey === 'YOUR_COMMERCIAL_API_KEY') {
            return array('hasQrCode' => false, 'reason' => 'Commercial API key not configured');
        }
        
        $imageData = file_get_contents($filePath);
        $base64Image = base64_encode($imageData);
        $mimeType = mime_content_type($filePath);
        
        $postData = json_encode(array(
            'image' => array(
                'data' => $base64Image,
                'type' => $mimeType
            ),
            'options' => array(
                'detect_qr' => true,
                'detect_barcodes' => false,
                'confidence_threshold' => 0.8
            )
        ));
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array(
            'Content-Type: application/json',
            'Authorization: Bearer ' . $apiKey
        ));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode == 200 && $response) {
            $result = json_decode($response, true);
            
            if (isset($result['qr_codes'][0]['data']) && !empty($result['qr_codes'][0]['data'])) {
                return array(
                    'hasQrCode' => true,
                    'qrCodeContent' => $result['qr_codes'][0]['data'],
                    'method' => 'commercial_api',
                    'confidence' => $result['qr_codes'][0]['confidence'] ?? 1.0
                );
            }
        }
        
        return array('hasQrCode' => false, 'reason' => 'Commercial API failed');
        
    } catch (Exception $e) {
        return array('hasQrCode' => false, 'reason' => 'Commercial API error');
    }
}

function tryBrowserBarcodeAPI($filePath) {
    try {
        // Simulate browser-based barcode detection API
        // This uses a web service that mimics browser barcode detection
        
        $apiUrl = 'https://api.barcodelookup.com/v3/products';
        
        $imageData = file_get_contents($filePath);
        $base64Image = base64_encode($imageData);
        
        $postData = json_encode(array(
            'image' => 'data:image/jpeg;base64,' . $base64Image,
            'formats' => array('qr_code'),
            'max_results' => 1
        ));
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode == 200 && $response) {
            $result = json_decode($response, true);
            
            if (isset($result['products'][0]['barcode_number'])) {
                return array(
                    'hasQrCode' => true,
                    'qrCodeContent' => $result['products'][0]['barcode_number'],
                    'method' => 'browser_barcode_api'
                );
            }
        }
        
        return array('hasQrCode' => false, 'reason' => 'Browser Barcode API failed');
        
    } catch (Exception $e) {
        return array('hasQrCode' => false, 'reason' => 'Browser Barcode API error');
    }
}

function detectQrCodeLocally($filePath) {
    try {
        // STRICT QR code detection - must have actual QR code structure
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('hasQrCode' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('hasQrCode' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // STRICT QR code detection - must have ALL characteristics
        $qrIndicators = 0;
        
        // 1. Check for ALL 3 corner markers (QR codes have 3 corner squares)
        if (hasAllQrCornerMarkers($image, $width, $height)) {
            $qrIndicators++;
            error_log("QR Detection: ALL corner markers found");
        } else {
            error_log("QR Detection: Missing corner markers");
        }
        
        // 2. Check for QR code timing patterns (regular grid lines)
        if (hasQrTimingPatterns($image, $width, $height)) {
            $qrIndicators++;
            error_log("QR Detection: Timing patterns found");
        } else {
            error_log("QR Detection: No timing patterns");
        }
        
        // 3. Check for QR code data modules (small squares in grid)
        if (hasQrDataModules($image, $width, $height)) {
            $qrIndicators++;
            error_log("QR Detection: Data modules found");
        } else {
            error_log("QR Detection: No data modules");
        }
        
        // 4. Check for QR code finder patterns (specific corner square structure)
        if (hasQrFinderPatterns($image, $width, $height)) {
            $qrIndicators++;
            error_log("QR Detection: Finder patterns found");
        } else {
            error_log("QR Detection: No finder patterns");
        }
        
        // 5. Check that it's NOT a document or photo (reject if looks like text/image)
        if (!isDocumentOrPhoto($image, $width, $height)) {
            $qrIndicators++;
            error_log("QR Detection: Not a document/photo");
        } else {
            error_log("QR Detection: Appears to be document/photo - REJECTING");
        }
        
        imagedestroy($image);
        
        // STRICT: Must have ALL 5 indicators for QR code
        $hasQrCode = $qrIndicators == 5;
        
        error_log("STRICT QR Detection: Indicators=$qrIndicators/5, HasQR=$hasQrCode");
        
        if (!$hasQrCode) {
            return array('hasQrCode' => false, 'reason' => 'Image does not contain a valid QR code structure');
        }
        
        return array(
            'hasQrCode' => $hasQrCode,
            'qrCodeContent' => $hasQrCode ? 'QR_CODE_DETECTED_LOCALLY' : null,
            'method' => 'strict_local_detection',
            'indicators' => $qrIndicators
        );
        
    } catch (Exception $e) {
        error_log("Local QR detection error: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'Local detection error');
    }
}

function hasAllQrCornerMarkers($image, $width, $height) {
    try {
        $markerSize = min($width, $height) / 7; // Slightly larger for better detection
        $markersFound = 0;
        
        // Check ALL three corners (QR codes have exactly 3 corner markers)
        $corners = array(
            array(0, 0), // top-left
            array($width - $markerSize, 0), // top-right
            array(0, $height - $markerSize) // bottom-left
        );
        
        foreach ($corners as $corner) {
            if (checkStrictQrCornerMarker($image, $corner[0], $corner[1], $markerSize, $width, $height)) {
                $markersFound++;
            }
        }
        
        // STRICT: Must have ALL 3 corner markers
        return $markersFound == 3;
        
    } catch (Exception $e) {
        return false;
    }
}

function checkStrictQrCornerMarker($image, $startX, $startY, $size, $width, $height) {
    try {
        // QR corner markers have a specific structure: 7x7 black square with 5x5 white square inside and 3x3 black square in center
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Sample every pixel for accuracy
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                $brightness = ($red + $green + $blue) / 3;
                
                // STRICT: Only pure black and pure white count
                if ($brightness < 40) {
                    $blackCount++;
                } else if ($brightness > 215) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        // STRICT: QR corner markers must have significant black and white areas
        $hasGoodRatio = $blackRatio > 0.3 && $whiteRatio > 0.3 && ($blackRatio + $whiteRatio) > 0.7;
        
        return $hasGoodRatio;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasQrTimingPatterns($image, $width, $height) {
    try {
        // QR codes have timing patterns - alternating black/white modules in specific rows/columns
        $timingPatterns = 0;
        $totalChecks = 0;
        
        // Check horizontal timing patterns (should be in middle area)
        $middleY = $height / 2;
        $timingStartX = min($width, $height) / 4;
        $timingEndX = $width - $timingStartX;
        
        if (hasTimingPatternLine($image, $middleY, $timingStartX, $timingEndX, $width, $height, true)) {
            $timingPatterns++;
        }
        $totalChecks++;
        
        // Check vertical timing patterns
        $middleX = $width / 2;
        $timingStartY = min($width, $height) / 4;
        $timingEndY = $height - $timingStartY;
        
        if (hasTimingPatternLine($image, $middleX, $timingStartY, $timingEndY, $width, $height, false)) {
            $timingPatterns++;
        }
        $totalChecks++;
        
        return $timingPatterns >= 1; // At least one timing pattern
        
    } catch (Exception $e) {
        return false;
    }
}

function hasTimingPatternLine($image, $fixedCoord, $startCoord, $endCoord, $width, $height, $isHorizontal) {
    try {
        $alternations = 0;
        $prevBrightness = -1;
        
        if ($isHorizontal) {
            // Check horizontal line
            for ($x = $startCoord; $x < $endCoord; $x++) {
                $rgb = imagecolorat($image, $x, $fixedCoord);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($prevBrightness != -1) {
                    // Check for alternating pattern (black to white or white to black)
                    if (($prevBrightness < 50 && $brightness > 205) || ($prevBrightness > 205 && $brightness < 50)) {
                        $alternations++;
                    }
                }
                $prevBrightness = $brightness;
            }
        } else {
            // Check vertical line
            for ($y = $startCoord; $y < $endCoord; $y++) {
                $rgb = imagecolorat($image, $fixedCoord, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($prevBrightness != -1) {
                    if (($prevBrightness < 50 && $brightness > 205) || ($prevBrightness > 205 && $brightness < 50)) {
                        $alternations++;
                    }
                }
                $prevBrightness = $brightness;
            }
        }
        
        // QR timing patterns should have many alternations
        $lineLength = $endCoord - $startCoord;
        return $alternations > ($lineLength * 0.3); // At least 30% alternations
        
    } catch (Exception $e) {
        return false;
    }
}

function hasQrDataModules($image, $width, $height) {
    try {
        // QR codes have data modules - small squares arranged in a grid
        $moduleSize = min($width, $height) / 25; // Approximate module size
        $modulesFound = 0;
        $totalChecks = 0;
        
        // Check for small square modules throughout the image
        for ($y = $moduleSize; $y < $height - $moduleSize; $y += $moduleSize) {
            for ($x = $moduleSize; $x < $width - $moduleSize; $x += $moduleSize) {
                if (isQrDataModule($image, $x, $y, $moduleSize, $width, $height)) {
                    $modulesFound++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // QR codes should have many data modules
        return ($modulesFound / $totalChecks) > 0.2; // At least 20% modules
        
    } catch (Exception $e) {
        return false;
    }
}

function isQrDataModule($image, $centerX, $centerY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Check a small square area
        for ($y = $centerY - $size/2; $y < $centerY + $size/2; $y++) {
            for ($x = $centerX - $size/2; $x < $centerX + $size/2; $x++) {
                if ($x >= 0 && $x < $width && $y >= 0 && $y < $height) {
                    $rgb = imagecolorat($image, $x, $y);
                    $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                    
                    if ($brightness < 50) {
                        $blackCount++;
                    } else if ($brightness > 205) {
                        $whiteCount++;
                    }
                    $totalPixels++;
                }
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // Module should be mostly black or mostly white
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        return $blackRatio > 0.8 || $whiteRatio > 0.8; // Mostly one color
        
    } catch (Exception $e) {
        return false;
    }
}

function hasQrFinderPatterns($image, $width, $height) {
    try {
        // QR finder patterns are the corner squares with specific structure
        $finderPatterns = 0;
        $markerSize = min($width, $height) / 7;
        
        // Check each corner for finder pattern structure
        $corners = array(
            array(0, 0), // top-left
            array($width - $markerSize, 0), // top-right
            array(0, $height - $markerSize) // bottom-left
        );
        
        foreach ($corners as $corner) {
            if (hasFinderPatternStructure($image, $corner[0], $corner[1], $markerSize, $width, $height)) {
                $finderPatterns++;
            }
        }
        
        // Must have at least 2 finder patterns
        return $finderPatterns >= 2;
        
    } catch (Exception $e) {
        return false;
    }
}


function isDocumentOrPhoto($image, $width, $height) {
    try {
        // Detect if image looks like a document or photo (not a QR code)
        
        // 1. Check for text-like patterns (documents have lots of small text)
        if (hasTextPatterns($image, $width, $height)) {
            error_log("Document detection: Text patterns found");
            return true;
        }
        
        // 2. Check for photo-like patterns (photos have gradual color changes)
        if (hasPhotoPatterns($image, $width, $height)) {
            error_log("Photo detection: Photo patterns found");
            return true;
        }
        
        // 3. Check for uniform backgrounds (documents/photos often have large uniform areas)
        if (hasUniformBackgrounds($image, $width, $height)) {
            error_log("Document/Photo detection: Uniform backgrounds found");
            return true;
        }
        
        // 4. Check for non-QR aspect ratios (QR codes are usually square or close to square)
        $aspectRatio = $width / $height;
        if ($aspectRatio < 0.5 || $aspectRatio > 2.0) {
            error_log("Document/Photo detection: Non-QR aspect ratio: $aspectRatio");
            return true;
        }
        
        return false;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasTextPatterns($image, $width, $height) {
    try {
        $smallPatterns = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 50; // Very small patterns for text
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasSmallTextPattern($image, $x, $y, $checkSize, $width, $height)) {
                    $smallPatterns++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // Documents have many small text patterns
        return ($smallPatterns / $totalChecks) > 0.3;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasSmallTextPattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $edgeCount = 0;
        $totalPixels = 0;
        
        // Check for many small edges (characteristic of text)
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            for ($x = $startX; $x < min($startX + $size, $width); $x++) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                // Check surrounding pixels for contrast
                if ($x > 0 && $x < $width-1 && $y > 0 && $y < $height-1) {
                    $leftRgb = imagecolorat($image, $x-1, $y);
                    $rightRgb = imagecolorat($image, $x+1, $y);
                    $topRgb = imagecolorat($image, $x, $y-1);
                    $bottomRgb = imagecolorat($image, $x, $y+1);
                    
                    $leftBrightness = (($leftRgb >> 16) & 0xFF + ($leftRgb >> 8) & 0xFF + $leftRgb & 0xFF) / 3;
                    $rightBrightness = (($rightRgb >> 16) & 0xFF + ($rightRgb >> 8) & 0xFF + $rightRgb & 0xFF) / 3;
                    $topBrightness = (($topRgb >> 16) & 0xFF + ($topRgb >> 8) & 0xFF + $topRgb & 0xFF) / 3;
                    $bottomBrightness = (($bottomRgb >> 16) & 0xFF + ($bottomRgb >> 8) & 0xFF + $bottomRgb & 0xFF) / 3;
                    
                    if (abs($brightness - $leftBrightness) > 50 || 
                        abs($brightness - $rightBrightness) > 50 ||
                        abs($brightness - $topBrightness) > 50 ||
                        abs($brightness - $bottomBrightness) > 50) {
                        $edgeCount++;
                    }
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        // Text has many small edges
        return ($edgeCount / $totalPixels) > 0.4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasPhotoPatterns($image, $width, $height) {
    try {
        $gradualChanges = 0;
        $totalChecks = 0;
        
        // Check for gradual color changes (characteristic of photos)
        for ($y = 0; $y < $height - 1; $y += 5) {
            for ($x = 0; $x < $width - 1; $x += 5) {
                $rgb1 = imagecolorat($image, $x, $y);
                $rgb2 = imagecolorat($image, $x+1, $y);
                $rgb3 = imagecolorat($image, $x, $y+1);
                
                $brightness1 = (($rgb1 >> 16) & 0xFF + ($rgb1 >> 8) & 0xFF + $rgb1 & 0xFF) / 3;
                $brightness2 = (($rgb2 >> 16) & 0xFF + ($rgb2 >> 8) & 0xFF + $rgb2 & 0xFF) / 3;
                $brightness3 = (($rgb3 >> 16) & 0xFF + ($rgb3 >> 8) & 0xFF + $rgb3 & 0xFF) / 3;
                
                // Check for gradual changes (not sharp QR code edges)
                if (abs($brightness1 - $brightness2) < 30 && abs($brightness1 - $brightness3) < 30) {
                    $gradualChanges++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // Photos have many gradual changes
        return ($gradualChanges / $totalChecks) > 0.6;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUniformBackgrounds($image, $width, $height) {
    try {
        $uniformAreas = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 10; // Large areas
        
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasUniformArea($image, $x, $y, $checkSize, $width, $height)) {
                    $uniformAreas++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        // Documents/photos have large uniform areas
        return ($uniformAreas / $totalChecks) > 0.4;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasUniformArea($image, $startX, $startY, $size, $width, $height) {
    try {
        $brightnessSum = 0;
        $pixelCount = 0;
        
        // Calculate average brightness
        for ($y = $startY; $y < min($startY + $size, $height); $y += 2) {
            for ($x = $startX; $x < min($startX + $size, $width); $x += 2) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                $brightnessSum += $brightness;
                $pixelCount++;
            }
        }
        
        if ($pixelCount == 0) return false;
        
        $avgBrightness = $brightnessSum / $pixelCount;
        
        // Check variance
        $variance = 0;
        for ($y = $startY; $y < min($startY + $size, $height); $y += 2) {
            for ($x = $startX; $x < min($startX + $size, $width); $x += 2) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                $variance += pow($brightness - $avgBrightness, 2);
            }
        }
        
        $variance = $variance / $pixelCount;
        
        // Uniform areas have low variance
        return $variance < 100;
        
    } catch (Exception $e) {
        return false;
    }
}

function checkCornerForQrMarker($image, $startX, $startY, $size, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        for ($y = $startY; $y < min($startY + $size, $height); $y += 2) {
            for ($x = $startX; $x < min($startX + $size, $width); $x += 2) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                $brightness = ($red + $green + $blue) / 3;
                
                if ($brightness < 60) {
                    $blackCount++;
                } else if ($brightness > 195) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        return $blackRatio > 0.2 && $whiteRatio > 0.2 && ($blackRatio + $whiteRatio) > 0.5;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasHighContrastPatterns($image, $width, $height) {
    try {
        $highContrastCount = 0;
        $totalSamples = 0;
        
        // Sample every 5th pixel
        for ($y = 0; $y < $height; $y += 5) {
            for ($x = 0; $x < $width; $x += 5) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                $brightness = ($red + $green + $blue) / 3;
                
                if ($brightness < 50 || $brightness > 205) {
                    $highContrastCount++;
                }
                $totalSamples++;
            }
        }
        
        if ($totalSamples == 0) return false;
        
        $contrastRatio = $highContrastCount / $totalSamples;
        return $contrastRatio > 0.2;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasGridStructure($image, $width, $height) {
    try {
        $gridLines = 0;
        $totalChecks = 0;
        $gridSize = min($width, $height) / 15;
        
        // Check for vertical lines
        for ($x = 0; $x < $width; $x += $gridSize) {
            if ($x >= $width) break;
            if (hasVerticalLine($image, $x, $width, $height)) {
                $gridLines++;
            }
            $totalChecks++;
        }
        
        // Check for horizontal lines
        for ($y = 0; $y < $height; $y += $gridSize) {
            if ($y >= $height) break;
            if (hasHorizontalLine($image, $y, $width, $height)) {
                $gridLines++;
            }
            $totalChecks++;
        }
        
        if ($totalChecks == 0) return false;
        
        $gridRatio = $gridLines / $totalChecks;
        return $gridRatio > 0.15;
        
    } catch (Exception $e) {
        return false;
    }
}

function hasVerticalLine($image, $x, $width, $height) {
    try {
        $contrastChanges = 0;
        $prevBrightness = -1;
        
        for ($y = 0; $y < $height; $y++) {
            $rgb = imagecolorat($image, $x, $y);
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            
            if ($prevBrightness != -1) {
                if (abs($brightness - $prevBrightness) > 80) {
                    $contrastChanges++;
                }
            }
            $prevBrightness = $brightness;
        }
        
        return $contrastChanges > ($height * 0.1);
        
    } catch (Exception $e) {
        return false;
    }
}

function hasHorizontalLine($image, $y, $width, $height) {
    try {
        $contrastChanges = 0;
        $prevBrightness = -1;
        
        for ($x = 0; $x < $width; $x++) {
            $rgb = imagecolorat($image, $x, $y);
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            
            if ($prevBrightness != -1) {
                if (abs($brightness - $prevBrightness) > 80) {
                    $contrastChanges++;
                }
            }
            $prevBrightness = $brightness;
        }
        
        return $contrastChanges > ($width * 0.1);
        
    } catch (Exception $e) {
        return false;
    }
}

function validateQrCodeContent($qrCodeContent) {
    try {
        if (empty($qrCodeContent)) {
            return array('isValid' => false, 'reason' => 'No QR code content detected');
        }
        
        // Check if it's a payment-related QR code
        $paymentKeywords = array('gcash', 'paymaya', 'payment', 'pay', 'qr', 'code', 'transfer', 'send', 'money');
        $contentLower = strtolower($qrCodeContent);
        
        $hasPaymentKeyword = false;
        foreach ($paymentKeywords as $keyword) {
            if (strpos($contentLower, $keyword) !== false) {
                $hasPaymentKeyword = true;
                break;
            }
        }
        
        // Check for URL patterns (many payment QR codes contain URLs)
        $isUrl = filter_var($qrCodeContent, FILTER_VALIDATE_URL) !== false;
        
        // Check for JSON-like content
        $isJson = (json_decode($qrCodeContent) !== null);
        
        // Accept if it has payment keywords, is a URL, or is JSON
        if ($hasPaymentKeyword || $isUrl || $isJson) {
            return array('isValid' => true, 'reason' => 'Valid payment QR code content');
        }
        
        // For generic QR codes, be more lenient
        if (strlen($qrCodeContent) > 10) {
            return array('isValid' => true, 'reason' => 'Valid QR code content');
        }
        
        return array('isValid' => false, 'reason' => 'QR code content does not appear to be payment-related');
        
    } catch (Exception $e) {
        error_log("QR content validation error: " . $e->getMessage());
        return array('isValid' => false, 'reason' => 'Content validation failed');
    }
}

function detectQrCodePatterns($filePath) {
    try {
        // Basic pattern detection as fallback
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('hasQrCode' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('hasQrCode' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // Very basic QR code pattern detection
        $blackWhiteRatio = calculateBlackWhiteRatio($image, $width, $height);
        $hasSquarePatterns = detectSquarePatterns($image, $width, $height);
        
        imagedestroy($image);
        
        // Very lenient criteria for basic detection
        $hasQrCode = $blackWhiteRatio > 0.1 && $hasSquarePatterns;
        
        error_log("Pattern detection: BlackWhiteRatio=$blackWhiteRatio, HasSquarePatterns=$hasSquarePatterns, HasQR=$hasQrCode");
        
        return array(
            'hasQrCode' => $hasQrCode,
            'qrCodeContent' => $hasQrCode ? 'QR_CODE_DETECTED_PATTERN' : null,
            'method' => 'pattern_detection'
        );
        
    } catch (Exception $e) {
        error_log("Pattern detection error: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'Pattern detection error');
    }
}

function calculateBlackWhiteRatio($image, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Sample every 10th pixel for performance
        for ($y = 0; $y < $height; $y += 10) {
            for ($x = 0; $x < $width; $x += 10) {
                $rgb = imagecolorat($image, $x, $y);
                $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
                
                if ($brightness < 80) {
                    $blackCount++;
                } else if ($brightness > 175) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return 0;
        
        return ($blackCount + $whiteCount) / $totalPixels;
        
    } catch (Exception $e) {
        return 0;
    }
}

function detectSquarePatterns($image, $width, $height) {
    try {
        $squareCount = 0;
        $totalChecks = 0;
        $checkSize = min($width, $height) / 20;
        
        // Check for square-like patterns
        for ($y = 0; $y < $height - $checkSize; $y += $checkSize) {
            for ($x = 0; $x < $width - $checkSize; $x += $checkSize) {
                if (hasSquarePattern($image, $x, $y, $checkSize, $width, $height)) {
                    $squareCount++;
                }
                $totalChecks++;
            }
        }
        
        if ($totalChecks == 0) return false;
        
        return ($squareCount / $totalChecks) > 0.05; // At least 5% square patterns
        
    } catch (Exception $e) {
        return false;
    }
}

function hasSquarePattern($image, $startX, $startY, $size, $width, $height) {
    try {
        $edgeCount = 0;
        $totalEdges = 0;
        
        // Check edges of the square
        // Top edge
        for ($x = $startX; $x < min($startX + $size, $width); $x++) {
            $rgb = imagecolorat($image, $x, $startY);
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            if ($brightness < 100) $edgeCount++;
            $totalEdges++;
        }
        
        // Bottom edge
        for ($x = $startX; $x < min($startX + $size, $width); $x++) {
            $rgb = imagecolorat($image, $x, min($startY + $size - 1, $height - 1));
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            if ($brightness < 100) $edgeCount++;
            $totalEdges++;
        }
        
        // Left edge
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            $rgb = imagecolorat($image, $startX, $y);
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            if ($brightness < 100) $edgeCount++;
            $totalEdges++;
        }
        
        // Right edge
        for ($y = $startY; $y < min($startY + $size, $height); $y++) {
            $rgb = imagecolorat($image, min($startX + $size - 1, $width - 1), $y);
            $brightness = (($rgb >> 16) & 0xFF + ($rgb >> 8) & 0xFF + $rgb & 0xFF) / 3;
            if ($brightness < 100) $edgeCount++;
            $totalEdges++;
        }
        
        if ($totalEdges == 0) return false;
        
        return ($edgeCount / $totalEdges) > 0.3; // At least 30% dark edges
        
    } catch (Exception $e) {
        return false;
    }
}

function detectActualQrCode($filePath) {
    try {
        // Create image resource based on file type
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('hasQrCode' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('hasQrCode' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // STRICT QR code detection - must have ALL indicators
        $cornerMarkers = detectQrCornerMarkers($image, $width, $height);
        $dataPatterns = detectQrDataPatterns($image, $width, $height);
        $contrastAreas = detectHighContrastAreas($image, $width, $height);
        $gridStructure = detectQrGridStructure($image, $width, $height);
        
        imagedestroy($image);
        
        // STRICT criteria - must have ALL 4 indicators for QR code
        $indicators = 0;
        if ($cornerMarkers) $indicators++;
        if ($dataPatterns) $indicators++;
        if ($contrastAreas) $indicators++;
        if ($gridStructure) $indicators++;
        
        // Only accept if ALL 4 indicators are present
        $hasQrCode = $indicators == 4;
        
        error_log("QR Detection Results: CornerMarkers=$cornerMarkers, DataPatterns=$dataPatterns, ContrastAreas=$contrastAreas, GridStructure=$gridStructure, Indicators=$indicators/4");
        
        return array(
            'hasQrCode' => $hasQrCode,
            'cornerMarkers' => $cornerMarkers,
            'dataPatterns' => $dataPatterns,
            'contrastAreas' => $contrastAreas,
            'gridStructure' => $gridStructure,
            'indicators' => $indicators
        );
        
    } catch (Exception $e) {
        error_log("QR code detection error: " . $e->getMessage());
        return array('hasQrCode' => false, 'reason' => 'QR code detection failed');
    }
}

function detectQrCornerMarkers($image, $width, $height) {
    try {
        // Check for QR code corner markers (three squares in corners)
        $markerSize = min($width, $height) / 6; // Slightly larger marker size
        
        // Check top-left corner
        $topLeft = checkCornerRegion($image, 0, 0, $markerSize, $markerSize, $width, $height);
        
        // Check top-right corner
        $topRight = checkCornerRegion($image, $width - $markerSize, 0, $markerSize, $markerSize, $width, $height);
        
        // Check bottom-left corner
        $bottomLeft = checkCornerRegion($image, 0, $height - $markerSize, $markerSize, $markerSize, $width, $height);
        
        // STRICT: ALL 3 corner markers must be detected
        $markerCount = 0;
        if ($topLeft) $markerCount++;
        if ($topRight) $markerCount++;
        if ($bottomLeft) $markerCount++;
        
        error_log("Corner Markers: TopLeft=$topLeft, TopRight=$topRight, BottomLeft=$bottomLeft, Count=$markerCount/3");
        
        return $markerCount == 3; // Must have ALL 3 corner markers
        
    } catch (Exception $e) {
        return false;
    }
}

function checkCornerRegion($image, $startX, $startY, $sizeX, $sizeY, $width, $height) {
    try {
        $blackCount = 0;
        $whiteCount = 0;
        $totalPixels = 0;
        
        // Sample pixels in the corner region
        for ($y = $startY; $y < min($startY + $sizeY, $height); $y += 1) {
            for ($x = $startX; $x < min($startX + $sizeX, $width); $x += 1) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                $brightness = ($red + $green + $blue) / 3;
                
                // STRICT: Only pure black and pure white count
                if ($brightness < 50) {
                    $blackCount++;
                } else if ($brightness > 205) {
                    $whiteCount++;
                }
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return false;
        
        $blackRatio = $blackCount / $totalPixels;
        $whiteRatio = $whiteCount / $totalPixels;
        
        // STRICT: QR code corner markers must have significant black and white areas
        $hasGoodRatio = $blackRatio > 0.25 && $whiteRatio > 0.25 && ($blackRatio + $whiteRatio) > 0.6;
        
        return $hasGoodRatio;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectQrDataPatterns($image, $width, $height) {
    try {
        // Look for regular grid patterns typical of QR codes
        $gridPatterns = 0;
        $totalSamples = 0;
        
        // Sample every 3rd pixel for better accuracy
        for ($y = 0; $y < $height; $y += 3) {
            for ($x = 0; $x < $width; $x += 3) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                $brightness = ($red + $green + $blue) / 3;
                
                // STRICT: Only pure black and pure white count as QR patterns
                if ($brightness < 40 || $brightness > 215) {
                    $gridPatterns++;
                }
                
                $totalSamples++;
            }
        }
        
        if ($totalSamples == 0) return false;
        
        $patternRatio = $gridPatterns / $totalSamples;
        
        // STRICT: QR codes should have significant pattern areas
        $hasPatterns = $patternRatio > 0.35;
        
        error_log("Data Patterns: PatternRatio=$patternRatio, HasPatterns=$hasPatterns");
        
        return $hasPatterns;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectHighContrastAreas($image, $width, $height) {
    try {
        $highContrastCount = 0;
        $totalSamples = 0;
        
        // Sample every 4th pixel for better accuracy
        for ($y = 0; $y < $height; $y += 4) {
            for ($x = 0; $x < $width; $x += 4) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                $brightness = ($red + $green + $blue) / 3;
                
                // STRICT: Only very dark or very bright areas count
                if ($brightness < 30 || $brightness > 225) {
                    $highContrastCount++;
                }
                
                $totalSamples++;
            }
        }
        
        if ($totalSamples == 0) return false;
        
        $contrastRatio = $highContrastCount / $totalSamples;
        
        // STRICT: QR codes should have significant high contrast areas
        $hasContrast = $contrastRatio > 0.25;
        
        error_log("High Contrast: ContrastRatio=$contrastRatio, HasContrast=$hasContrast");
        
        return $hasContrast;
        
    } catch (Exception $e) {
        return false;
    }
}

function detectQrGridStructure($image, $width, $height) {
    try {
        // Look for regular grid structure typical of QR codes
        $gridLines = 0;
        $totalChecks = 0;
        
        // Check for vertical and horizontal grid lines
        $gridSize = min($width, $height) / 20; // Approximate grid cell size
        
        // Check vertical lines
        for ($x = 0; $x < $width; $x += $gridSize) {
            if ($x >= $width) break;
            $lineStrength = checkGridLine($image, $x, 0, $width, $height, true); // vertical
            if ($lineStrength > 0.3) {
                $gridLines++;
            }
            $totalChecks++;
        }
        
        // Check horizontal lines
        for ($y = 0; $y < $height; $y += $gridSize) {
            if ($y >= $height) break;
            $lineStrength = checkGridLine($image, 0, $y, $width, $height, false); // horizontal
            if ($lineStrength > 0.3) {
                $gridLines++;
            }
            $totalChecks++;
        }
        
        if ($totalChecks == 0) return false;
        
        $gridRatio = $gridLines / $totalChecks;
        
        // STRICT: QR codes should have regular grid structure
        $hasGrid = $gridRatio > 0.2;
        
        error_log("Grid Structure: GridLines=$gridLines, TotalChecks=$totalChecks, GridRatio=$gridRatio, HasGrid=$hasGrid");
        
        return $hasGrid;
        
    } catch (Exception $e) {
        return false;
    }
}

function checkGridLine($image, $startX, $startY, $width, $height, $isVertical) {
    try {
        $contrastChanges = 0;
        $totalPixels = 0;
        $prevBrightness = -1;
        
        if ($isVertical) {
            // Check vertical line
            for ($y = $startY; $y < $height; $y++) {
                $rgb = imagecolorat($image, $startX, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                $brightness = ($red + $green + $blue) / 3;
                
                if ($prevBrightness != -1) {
                    $contrast = abs($brightness - $prevBrightness);
                    if ($contrast > 100) { // High contrast change
                        $contrastChanges++;
                    }
                }
                
                $prevBrightness = $brightness;
                $totalPixels++;
            }
        } else {
            // Check horizontal line
            for ($x = $startX; $x < $width; $x++) {
                $rgb = imagecolorat($image, $x, $startY);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                $brightness = ($red + $green + $blue) / 3;
                
                if ($prevBrightness != -1) {
                    $contrast = abs($brightness - $prevBrightness);
                    if ($contrast > 100) { // High contrast change
                        $contrastChanges++;
                    }
                }
                
                $prevBrightness = $brightness;
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) return 0;
        
        return $contrastChanges / $totalPixels;
        
    } catch (Exception $e) {
        return 0;
    }
}

function detectPaymentQrCode($filePath) {
    try {
        // Create image resource based on file type
        $image = null;
        $mimeType = mime_content_type($filePath);
        
        switch ($mimeType) {
            case 'image/jpeg':
                $image = imagecreatefromjpeg($filePath);
                break;
            case 'image/png':
                $image = imagecreatefrompng($filePath);
                break;
            case 'image/gif':
                $image = imagecreatefromgif($filePath);
                break;
            default:
                return array('isPaymentQr' => false, 'reason' => 'Unsupported image format');
        }
        
        if (!$image) {
            return array('isPaymentQr' => false, 'reason' => 'Cannot process image');
        }
        
        $width = imagesx($image);
        $height = imagesy($image);
        
        // More lenient payment QR detection
        $colorAnalysis = analyzePaymentQrColors($image, $width, $height);
        
        imagedestroy($image);
        
        // Payment QR codes typically have:
        // 1. Good contrast (not too uniform)
        // 2. Not too much of any single color
        // 3. Reasonable color variation
        
        $isPaymentQr = $colorAnalysis['hasGoodContrast'] && 
                      $colorAnalysis['hasReasonableVariation'] && 
                      !$colorAnalysis['isTooUniform'];
        
        return array(
            'isPaymentQr' => $isPaymentQr,
            'colorAnalysis' => $colorAnalysis
        );
        
    } catch (Exception $e) {
        error_log("Payment QR detection error: " . $e->getMessage());
        return array('isPaymentQr' => false, 'reason' => 'Payment QR detection failed');
    }
}

function analyzePaymentQrColors($image, $width, $height) {
    try {
        $totalPixels = 0;
        $darkCount = 0;
        $brightCount = 0;
        $colorVariation = 0;
        $uniformCount = 0;
        
        // Sample every 15th pixel for performance
        for ($y = 0; $y < $height; $y += 15) {
            for ($x = 0; $x < $width; $x += 15) {
                $rgb = imagecolorat($image, $x, $y);
                $red = ($rgb >> 16) & 0xFF;
                $green = ($rgb >> 8) & 0xFF;
                $blue = $rgb & 0xFF;
                
                $brightness = ($red + $green + $blue) / 3;
                
                // Count dark and bright areas
                if ($brightness < 60) {
                    $darkCount++;
                } else if ($brightness > 195) {
                    $brightCount++;
                }
                
                // Check for color variation
                $maxColor = max($red, $green, $blue);
                $minColor = min($red, $green, $blue);
                $variation = $maxColor - $minColor;
                $colorVariation += $variation;
                
                // Check for uniform colors (potential fake images)
                if ($variation < 30) {
                    $uniformCount++;
                }
                
                $totalPixels++;
            }
        }
        
        if ($totalPixels == 0) {
            return array(
                'hasGoodContrast' => false,
                'hasReasonableVariation' => false,
                'isTooUniform' => true
            );
        }
        
        $darkRatio = $darkCount / $totalPixels;
        $brightRatio = $brightCount / $totalPixels;
        $avgVariation = $colorVariation / $totalPixels;
        $uniformRatio = $uniformCount / $totalPixels;
        
        // Good contrast: has both dark and bright areas
        $hasGoodContrast = $darkRatio > 0.05 && $brightRatio > 0.05;
        
        // Reasonable variation: not too uniform
        $hasReasonableVariation = $avgVariation > 20 && $uniformRatio < 0.8;
        
        // Not too uniform: should have some variation
        $isTooUniform = $uniformRatio > 0.9 || $avgVariation < 10;
        
        return array(
            'hasGoodContrast' => $hasGoodContrast,
            'hasReasonableVariation' => $hasReasonableVariation,
            'isTooUniform' => $isTooUniform,
            'darkRatio' => $darkRatio,
            'brightRatio' => $brightRatio,
            'avgVariation' => $avgVariation,
            'uniformRatio' => $uniformRatio
        );
        
    } catch (Exception $e) {
        return array(
            'hasGoodContrast' => false,
            'hasReasonableVariation' => false,
            'isTooUniform' => true
        );
    }
}

function saveFile($fileKey, $uploadDir) {
    error_log("saveFile called for key: " . $fileKey);
    error_log("FILES array keys: " . implode(', ', array_keys($_FILES)));
    error_log("Checking for file key: " . $fileKey);
    
    if (!isset($_FILES[$fileKey])) {
        error_log("File key '$fileKey' not found in FILES array");
        return null;
    }
    
    if ($_FILES[$fileKey]['error'] !== UPLOAD_ERR_OK) {
        error_log("File upload error for '$fileKey': " . $_FILES[$fileKey]['error']);
        return null;
    }
    
    $fileTmp  = $_FILES[$fileKey]['tmp_name'];
    $fileName = uniqid() . "_" . basename($_FILES[$fileKey]['name']);
    $filePath = $uploadDir . $fileName;
    
    error_log("Attempting to move file from '$fileTmp' to '$filePath'");

    if (move_uploaded_file($fileTmp, $filePath)) {
        error_log("File saved successfully: " . $filePath);
        return $filePath;
    }
    
    error_log("Failed to move uploaded file");
    return null;
}

// Check if email already exists BEFORE processing files
error_log("=== REGISTRATION DEBUG ===");
error_log("Registration attempt for email: " . $email);

$emailCheckStmt = $conn->prepare("SELECT id, status FROM registrations WHERE email = ?");
$emailCheckStmt->bind_param("s", $email);
$emailCheckStmt->execute();
$emailResult = $emailCheckStmt->get_result();

error_log("Email check result: " . $emailResult->num_rows . " rows found");

if ($emailResult->num_rows > 0) {
    $existingUser = $emailResult->fetch_assoc();
    error_log("Found existing user - ID: " . $existingUser['id'] . ", Status: " . $existingUser['status']);
    
    $response = array(
        "success" => false,
        "message" => "This email is already registered. Please use a different email or try logging in."
    );
    error_log("Registration BLOCKED - email already exists");
    echo json_encode($response);
    exit;
}

error_log("Email is new - proceeding with registration");

$idFrontPath = saveFile("idFrontFile", $uploadDir);
$idBackPath  = saveFile("idBackFile", $uploadDir);
$gcashQRPath = saveFile("qrFile", $uploadDir);

error_log("File upload results - Front: " . ($idFrontPath ?: "null") . ", Back: " . ($idBackPath ?: "null") . ", QR: " . ($gcashQRPath ?: "null"));

// Check role - GCash QR code is only required for BH Owner
$isBoarder = ($role === "Boarder");

// Validate GCash QR code if uploaded (required for BH Owner only)
if ($gcashQRPath) {
    error_log("=== QR VALIDATION CALLED ===");
    error_log("QR file path: " . $gcashQRPath);
    error_log("QR file exists: " . (file_exists($gcashQRPath) ? 'YES' : 'NO'));
    
    $qrValidationResult = validateGcashQrCode($gcashQRPath);
    
    error_log("QR validation result: " . json_encode($qrValidationResult));
    
    if (!$qrValidationResult['isValid']) {
        // Clean up uploaded files
        if ($idFrontPath && file_exists($idFrontPath)) unlink($idFrontPath);
        if ($idBackPath && file_exists($idBackPath)) unlink($idBackPath);
        if ($gcashQRPath && file_exists($gcashQRPath)) unlink($gcashQRPath);
        
        $response = array(
            "success" => false,
            "message" => "GCash QR code validation failed: " . $qrValidationResult['reason']
        );
        error_log("GCash QR validation failed: " . $qrValidationResult['reason']);
        echo json_encode($response);
        exit;
    } else {
        error_log("QR validation PASSED - proceeding with registration");
    }
} else {
    // QR code is required for BH Owner
    if (!$isBoarder) {
        // Clean up uploaded files
        if ($idFrontPath && file_exists($idFrontPath)) unlink($idFrontPath);
        if ($idBackPath && file_exists($idBackPath)) unlink($idBackPath);
        
        $response = array(
            "success" => false,
            "message" => "GCash QR code is required for BH Owner. Please upload your GCash QR code."
        );
        error_log("GCash QR code validation failed: QR code is required for BH Owner");
        echo json_encode($response);
        exit;
    }
    error_log("No QR file uploaded - allowed for Boarder role");
}

// Insert into DB with unverified status (requires email verification first)
$sql = "INSERT INTO registrations
    (role, first_name, middle_name, last_name, suffix, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr, status, created_at) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'unverified', NOW())";
$stmt = $conn->prepare($sql);
if (!$stmt) {
    error_log("SQL prepare error: " . $conn->error);
    throw new Exception("SQL prepare error: " . $conn->error);
}

// For Boarder: GCash number and QR code can be null
// For BH Owner: GCash number and QR code are required
if ($isBoarder) {
    // Set GCash to null for Boarder
    $gcashNum = null;
    $gcashQRPath = null;
    error_log("Boarder role detected - Setting GCash number and QR code to null");
} else {
    // Validate GCash number for BH Owner
    if (empty($gcashNum)) {
        // Clean up uploaded files
        if ($idFrontPath && file_exists($idFrontPath)) unlink($idFrontPath);
        if ($idBackPath && file_exists($idBackPath)) unlink($idBackPath);
        if ($gcashQRPath && file_exists($gcashQRPath)) unlink($gcashQRPath);
        
        $response = array(
            "success" => false,
            "message" => "GCash number is required for BH Owner"
        );
        error_log("GCash number validation failed: GCash number is required for BH Owner");
        echo json_encode($response);
        exit;
    }
}

$bindResult = $stmt->bind_param("sssssssssssssssss",
    $role, $firstName, $middleName, $lastName, $suffix, $birthDate,
    $phone, $address, $email, $hashedPassword, $gcashNum,
    $idType, $idNumber, $isAgreed,
    $idFrontPath, $idBackPath, $gcashQRPath
);

if (!$bindResult) {
    error_log("Bind param error: " . $stmt->error);
    throw new Exception("Bind param error: " . $stmt->error);
}
if ($stmt->execute()) {
    $userId = $conn->insert_id;
    
    // Generate and send verification code
    $verificationCode = str_pad(rand(0, 999999), 6, '0', STR_PAD_LEFT);
    $expiryTime = date('Y-m-d H:i:s', strtotime('+30 minutes'));
    
    // Insert verification record
    $verificationSql = "INSERT INTO email_verifications (user_id, email, verification_code, expiry_time, created_at) 
                       VALUES (?, ?, ?, ?, NOW())";
    $verificationStmt = $conn->prepare($verificationSql);
    $verificationStmt->bind_param("isss", $userId, $email, $verificationCode, $expiryTime);
    
    if ($verificationStmt->execute()) {
        error_log("Verification record created successfully");
        
        // Send response immediately, then send email in background
        $response = array(
            "success" => true,
            "message" => "Registration successful! Please check your email for verification code. You have 30 minutes to verify your account.",
            "requires_verification" => true
        );
        error_log("Registration SUCCESS - sending immediate response");
        
        // Send response first to prevent timeout
        error_log("=== SENDING IMMEDIATE RESPONSE ===");
        error_log("Response array: " . print_r($response, true));
        error_log("JSON response: " . json_encode($response));
        
        // Ensure clean output
        ob_clean();
        echo json_encode($response);
        error_log("=== IMMEDIATE RESPONSE SENT ===");
        
        // Close database connections
        $verificationStmt->close();
        $stmt->close();
        $conn->close();
        
        // Send email in background (don't wait for it)
        error_log("Sending verification email in background to: " . $email);
        $emailSent = sendVerificationEmail($email, $firstName, $verificationCode);
        error_log("Background email send result: " . ($emailSent ? "SUCCESS" : "FAILED"));
        
        exit; // Exit immediately after sending response
    } else {
        $response = array(
            "success" => false,
            "message" => "Registration failed to create verification record."
        );
    }
    
    // This code is now handled above in the immediate response section
} else {
    $errorMsg = "Database insert error: " . $stmt->error;
    error_log("Registration failed: " . $errorMsg);
    $response = array(
        "success" => false,
        "message" => $errorMsg
    );
    echo json_encode($response);
    
    // Close resources after error response
    $stmt->close();
    $conn->close();
    exit; // Exit to prevent further execution
}

} catch (Exception $e) {
    error_log("Registration error: " . $e->getMessage());
    error_log("Registration error trace: " . $e->getTraceAsString());
    $response = array(
        "success" => false,
        "message" => "Server error: " . $e->getMessage()
    );
    echo json_encode($response);
}

function sendVerificationEmail($email, $firstName, $verificationCode) {
    $subject = "Email Verification - BoardEase";
    $message = "
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
            .content { padding: 20px; background-color: #f9f9f9; }
            .verification-code { 
                background-color: #4CAF50; 
                color: white; 
                font-size: 24px; 
                font-weight: bold; 
                padding: 15px; 
                text-align: center; 
                margin: 20px 0;
                border-radius: 5px;
                letter-spacing: 3px;
            }
            .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
            .warning { background-color: #ffeb3b; padding: 10px; border-left: 4px solid #ff9800; margin: 15px 0; }
        </style>
    </head>
    <body>
        <div class='container'>
            <div class='header'>
                <h1>Email Verification</h1>
            </div>
            <div class='content'>
                <h2>Hello " . htmlspecialchars($firstName) . "!</h2>
                <p>Thank you for registering with BoardEase. To complete your registration, please verify your email address using the code below:</p>
                
                <div class='verification-code'>" . $verificationCode . "</div>
                
                <div class='verification-link'>
                    <p><strong>Quick Access:</strong> Click the link below to open the verification screen directly in the BoardEase app:</p>
                    <p><a href='https://boardease.app/verify?email=" . urlencode($email) . "' style='color: #007bff; text-decoration: underline; font-weight: bold; font-size: 16px; display: block; margin: 10px 0; padding: 10px; background-color: #f8f9fa; border: 1px solid #007bff; border-radius: 5px;'>Open Verification Screen in BoardEase App</a></p>
                    <p style='font-size: 12px; color: #666;'>If the link doesn't work, make sure you have the BoardEase app installed on your device.</p>
                </div>
                
                <div class='warning'>
                    <strong>Important:</strong> This verification code will expire in 30 minutes. If you don't verify your email within this time, your account will be automatically deleted.
                </div>
                
                <p>If you didn't create an account with BoardEase, please ignore this email.</p>
            </div>
            <div class='footer'>
                <p>This is an automated message from BoardEase. Please do not reply to this email.</p>
            </div>
        </div>
    </body>
    </html>
    ";
    
    // Use the configured email system (Gmail SMTP)
    return sendEmail($email, $subject, $message);
}
?>
