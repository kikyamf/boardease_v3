<?php
// insert_registration.php

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Log the request for debugging
error_log("Registration request received at " . date('Y-m-d H:i:s'));
error_log("POST data: " . print_r($_POST, true));
error_log("FILES data: " . print_r($_FILES, true));

// Database connection
$servername = "localhost";
$username   = "root"; // adjust if needed
$password   = "";     // adjust if needed
$dbname     = "boardease_testing"; // adjust if needed

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
$uploadDir = "uploads/"; // make sure this folder exists and is writable

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

// Insert into DB
$sql = "INSERT INTO registration
    (role, first_name, middle_name, last_name, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    die("SQL error: " . $conn->error);
}

$stmt->bind_param("ssssssssssssssss",
    $role, $firstName, $middleName, $lastName, $birthDate,
    $phone, $address, $email, $hashedPassword, $gcashNum,
    $idType, $idNumber, $isAgreed,
    $idFrontPath, $idBackPath, $gcashQRPath
);

if ($stmt->execute()) {
    $response = array(
        "success" => true,
        "message" => "Registration successful!"
    );
    error_log("Registration successful for user: " . $email);
    echo json_encode($response);
} else {
    $errorMsg = "Database insert error: " . $stmt->error;
    error_log("Registration failed: " . $errorMsg);
    $response = array(
        "success" => false,
        "message" => $errorMsg
    );
    echo json_encode($response);
}

$stmt->close();
$conn->close();
?>
