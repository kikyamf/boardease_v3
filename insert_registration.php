<?php
// insert_registration.php

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

// Insert into DB
$sql = "INSERT INTO registrations
    (role, first_name, middle_name, last_name, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
    $response = array(
        "success" => true,
        "message" => "Registration successful!"
    );
    error_log("Registration successful for user: " . $email);
    error_log("Sending success response: " . json_encode($response));
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
?>
