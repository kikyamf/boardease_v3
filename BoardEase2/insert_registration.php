<?php
// insert_registration.php

// Start output buffering to catch any unwanted output
ob_start();

// Disable error display to prevent HTML output
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Set content type to JSON
header('Content-Type: application/json');

// Include email configuration (with error handling)
$emailConfigPath = '../email_config.php';
if (!file_exists($emailConfigPath)) {
    error_log("Warning: email_config.php not found at: " . $emailConfigPath);
    // Define a fallback function if email config doesn't exist
    if (!function_exists('sendEmail')) {
        function sendEmail($email, $subject, $message) {
            error_log("sendEmail called but email_config.php not loaded");
            return false;
        }
    }
} else {
    require_once $emailConfigPath;
}

// Log the request for debugging
error_log("Registration request received at " . date('Y-m-d H:i:s'));
error_log("POST data: " . print_r($_POST, true));
error_log("FILES data: " . print_r($_FILES, true));
error_log("BirthDate received: '" . ($_POST['birthDate'] ?? 'NOT_SET') . "'");

try {
// Database connection
$servername = "localhost";
$username   = "boardease"; // adjust if needed
$password   = "boardease";     // adjust if needed
$dbname     = "boardease2"; // adjust if needed

// Enable mysqli exception mode
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    error_log("Database connection failed: " . $conn->connect_error);
    $response = array(
        "success" => false,
        "message" => "Database connection failed. Please try again later.",
        "permits_received" => 0
    );
    ob_clean();
    echo json_encode($response);
    exit;
}

// Collect POST data
$role       = $_POST['role'] ?? null;
$firstName  = $_POST['firstName'] ?? null;
$middleName = $_POST['middleName'] ?? null;
$lastName   = $_POST['lastName'] ?? null;
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
        "message" => "Error: Missing required fields.",
        "permits_received" => 0
    );
    ob_clean();
    echo json_encode($response);
    exit;
}

// Hash the password for security
$hashedPassword = password_hash($password, PASSWORD_DEFAULT);

// Handle file uploads
$uploadDir = "../uploads/registrations/"; // make sure this folder exists and is writable

if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

function saveFile($fileKey, $uploadDir) {
    if (!isset($_FILES[$fileKey])) {
        error_log("File key '" . $fileKey . "' not found in FILES array");
        return null;
    }
    
    if ($_FILES[$fileKey]['error'] !== UPLOAD_ERR_OK) {
        error_log("File upload error for '" . $fileKey . "': " . $_FILES[$fileKey]['error']);
        return null;
    }
    
    $fileTmp  = $_FILES[$fileKey]['tmp_name'];
    $fileName = uniqid() . "_" . basename($_FILES[$fileKey]['name']);
    $filePath = $uploadDir . $fileName;
    
    error_log("Attempting to save file: " . $fileKey . " from " . $fileTmp . " to " . $filePath);

    if (move_uploaded_file($fileTmp, $filePath)) {
        error_log("File successfully moved to: " . $filePath);
        // Verify file exists after move
        if (file_exists($filePath)) {
            error_log("File verified to exist at: " . $filePath . " (size: " . filesize($filePath) . " bytes)");
            return $filePath;
        } else {
            error_log("ERROR: File was moved but does not exist at: " . $filePath);
            return null;
        }
    } else {
        error_log("ERROR: Failed to move uploaded file from " . $fileTmp . " to " . $filePath);
        return null;
    }
}

$idFrontPath = saveFile("idFrontFile", $uploadDir);
$idBackPath  = saveFile("idBackFile", $uploadDir);
$gcashQRPath = saveFile("qrFile", $uploadDir);

// Handle business permit uploads (for BH Owner only, up to 3 permits)
$permitUploadDir = "../uploads/business_permits/";
if (!is_dir($permitUploadDir)) {
    mkdir($permitUploadDir, 0777, true);
}

$permitFiles = array();
for ($i = 1; $i <= 3; $i++) {
    $permitKey = "permitFile" . $i;
    error_log("Checking for permit file key: " . $permitKey);
    if (isset($_FILES[$permitKey])) {
        error_log("Permit file " . $i . " found in FILES array. Error code: " . ($_FILES[$permitKey]['error'] ?? 'not set'));
    } else {
        error_log("Permit file " . $i . " NOT found in FILES array");
    }
    $permitPath = saveFile($permitKey, $permitUploadDir);
    if ($permitPath) {
        $permitFiles[$i] = $permitPath;
        error_log("Successfully saved permit file " . $i . " to: " . $permitPath);
    } else {
        error_log("Failed to save permit file " . $i);
    }
}

error_log("File upload results - Front: " . ($idFrontPath ?: "null") . ", Back: " . ($idBackPath ?: "null") . ", QR: " . ($gcashQRPath ?: "null"));
error_log("Business permit files uploaded: " . count($permitFiles));
error_log("Role received: '" . $role . "'");
error_log("Permit files array: " . print_r($permitFiles, true));

// Insert into DB with unverified status (requires email verification first)
$sql = "INSERT INTO registrations
    (role, first_name, middle_name, last_name, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr, status, created_at) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'unverified', NOW())";

try {
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        error_log("SQL prepare error: " . $conn->error);
        throw new Exception("SQL prepare error: " . $conn->error);
    }
} catch (mysqli_sql_exception $e) {
    error_log("SQL prepare exception: " . $e->getMessage());
    $response = array(
        "success" => false,
        "message" => "Database error: " . $e->getMessage(),
        "permits_received" => isset($permitFiles) ? count($permitFiles) : 0
    );
    ob_clean();
    echo json_encode($response);
    $conn->close();
    exit;
}

$bindResult = $stmt->bind_param("ssssssssssssssss",
    $role, $firstName, $middleName, $lastName, $birthDate,
    $phone, $address, $email, $hashedPassword, $gcashNum,
    $idType, $idNumber, $isAgreed,
    $idFrontPath, $idBackPath, $gcashQRPath
);

if (!$bindResult) {
    error_log("Bind param error: " . $stmt->error);
    $response = array(
        "success" => false,
        "message" => "Database error: " . $stmt->error,
        "permits_received" => isset($permitFiles) ? count($permitFiles) : 0
    );
    ob_clean();
    echo json_encode($response);
    $stmt->close();
    $conn->close();
    exit;
}

try {
    $executeResult = $stmt->execute();
} catch (mysqli_sql_exception $e) {
    error_log("SQL execution error: " . $e->getMessage());
    $errorMessage = $e->getMessage();
    
    // Handle specific error cases
    if (strpos($errorMessage, "Duplicate entry") !== false && strpos($errorMessage, "unique_email") !== false) {
        $errorMessage = "This email is already registered. Please use a different email or try logging in.";
    }
    
    $response = array(
        "success" => false,
        "message" => $errorMessage,
        "permits_received" => isset($permitFiles) ? count($permitFiles) : 0
    );
    ob_clean();
    echo json_encode($response);
    $stmt->close();
    $conn->close();
    exit;
}

if ($executeResult) {
    $userId = $conn->insert_id;
    error_log("Registration inserted successfully. User ID: " . $userId);
    
    // Insert business permits if any were uploaded (for BH Owner)
    // Check if role is NOT Boarder (could be "BH Owner" or other values)
    $isBHOwner = ($role !== "Boarder" && $role !== null);
    error_log("Is BH Owner check - Role: '" . $role . "', isBHOwner: " . ($isBHOwner ? "true" : "false"));
    error_log("Permit files count: " . count($permitFiles));
    
    $permitsInserted = 0;
    if (!empty($permitFiles) && $isBHOwner) {
        error_log("Attempting to insert " . count($permitFiles) . " business permit(s) for user " . $userId);
        $permitSql = "INSERT INTO bs_permits (reg_id, permit_file, permit_number, created_at) VALUES (?, ?, ?, NOW())";
        
        foreach ($permitFiles as $permitNumber => $permitPath) {
            error_log("Inserting permit " . $permitNumber . " with path: " . $permitPath);
            $permitStmt = $conn->prepare($permitSql);
            
            if ($permitStmt) {
                $permitStmt->bind_param("isi", $userId, $permitPath, $permitNumber);
                if (!$permitStmt->execute()) {
                    error_log("Failed to insert business permit " . $permitNumber . ": " . $permitStmt->error);
                } else {
                    $permitsInserted++;
                    error_log("Successfully inserted business permit " . $permitNumber . " for user " . $userId . " (permit_id: " . $conn->insert_id . ")");
                }
                $permitStmt->close();
            } else {
                error_log("Failed to prepare permit insert statement for permit " . $permitNumber . ": " . $conn->error);
            }
        }
    } else {
        if (empty($permitFiles)) {
            error_log("No permit files to insert (empty array)");
        }
        if (!$isBHOwner) {
            error_log("User is not a BH Owner (role: '" . $role . "'), skipping permit insertion");
        }
    }
    
    // Generate and send verification code
    $verificationCode = str_pad(rand(0, 999999), 6, '0', STR_PAD_LEFT);
    $expiryTime = date('Y-m-d H:i:s', strtotime('+30 minutes'));
    
    // Insert verification record
    $verificationSql = "INSERT INTO email_verifications (user_id, email, verification_code, expiry_time, created_at) 
                       VALUES (?, ?, ?, ?, NOW())";
    $verificationStmt = $conn->prepare($verificationSql);
    $verificationStmt->bind_param("isss", $userId, $email, $verificationCode, $expiryTime);
    
    if ($verificationStmt->execute()) {
        // Send verification email
        $emailSent = sendVerificationEmail($email, $firstName, $verificationCode);
        
        if ($emailSent) {
            $response = array(
                "success" => true,
                "message" => "Registration successful! Please check your email for verification code. You have 30 minutes to verify your account.",
                "requires_verification" => true,
                "permits_received" => count($permitFiles),
                "permits_inserted" => $permitsInserted
            );
        } else {
            $response = array(
                "success" => false,
                "message" => "Registration created but failed to send verification email. Please contact support.",
                "permits_received" => count($permitFiles)
            );
        }
    } else {
        $response = array(
            "success" => false,
            "message" => "Registration failed to create verification record.",
            "permits_received" => count($permitFiles)
        );
    }
    
    $verificationStmt->close();
    error_log("Registration submitted for verification - user: " . $email);
    error_log("Sending response: " . json_encode($response));
    
    // Clear any buffered output and send JSON
    ob_clean();
    echo json_encode($response);
    
    // Close resources after successful response
    $stmt->close();
    $conn->close();
    exit; // Exit to prevent further execution
} else {
    $errorMsg = "Database insert error: " . $stmt->error;
    error_log("Registration failed: " . $errorMsg);
    $response = array(
        "success" => false,
        "message" => $errorMsg
    );
    // Clear any buffered output and send JSON
    ob_clean();
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
        "message" => "Server error: " . $e->getMessage(),
        "permits_received" => isset($permitFiles) ? count($permitFiles) : 0
    );
    // Clear any buffered output and send JSON
    ob_clean();
    echo json_encode($response);
    exit;
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

