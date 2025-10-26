<?php
// insert_registration.php

// Include email configuration
require_once 'email_config.php';

// Disable error display to prevent HTML output
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

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

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("DB Connection failed: " . $conn->connect_error);
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

function saveFile($fileKey, $uploadDir) {
    if (!isset($_FILES[$fileKey]) || $_FILES[$fileKey]['error'] !== UPLOAD_ERR_OK) {
        return null;
    }
    $fileTmp  = $_FILES[$fileKey]['tmp_name'];
    $fileName = uniqid() . "_" . basename($_FILES[$fileKey]['name']);
    $filePath = $uploadDir . $fileName;

    if (move_uploaded_file($fileTmp, $filePath)) {
        return $filePath;
    }
    return null;
}

$idFrontPath = saveFile("idFrontFile", $uploadDir);
$idBackPath  = saveFile("idBackFile", $uploadDir);
$gcashQRPath = saveFile("qrFile", $uploadDir);

error_log("File upload results - Front: " . ($idFrontPath ?: "null") . ", Back: " . ($idBackPath ?: "null") . ", QR: " . ($gcashQRPath ?: "null"));

// Insert into DB with unverified status (requires email verification first)
$sql = "INSERT INTO registrations
    (role, first_name, middle_name, last_name, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr, status, created_at) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'unverified', NOW())";
$stmt = $conn->prepare($sql);
if (!$stmt) {
    error_log("SQL prepare error: " . $conn->error);
    throw new Exception("SQL prepare error: " . $conn->error);
}

$bindResult = $stmt->bind_param("ssssssssssssssss",
    $role, $firstName, $middleName, $lastName, $birthDate,
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
        // Send verification email
        $emailSent = sendVerificationEmail($email, $firstName, $verificationCode);
        
        if ($emailSent) {
            $response = array(
                "success" => true,
                "message" => "Registration successful! Please check your email for verification code. You have 30 minutes to verify your account.",
                "requires_verification" => true
            );
        } else {
            $response = array(
                "success" => false,
                "message" => "Registration created but failed to send verification email. Please contact support."
            );
        }
    } else {
        $response = array(
            "success" => false,
            "message" => "Registration failed to create verification record."
        );
    }
    
    $verificationStmt->close();
    error_log("Registration submitted for verification - user: " . $email);
    error_log("Sending response: " . json_encode($response));
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
