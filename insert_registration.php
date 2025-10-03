<?php
// insert_registration.php

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

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
$firstName  = $_POST['first_name'] ?? null;
$middleName = $_POST['middle_name'] ?? null;
$lastName   = $_POST['last_name'] ?? null;
$birthDate  = $_POST['birth_date'] ?? null;
$phone      = $_POST['phone'] ?? null;
$address    = $_POST['address'] ?? null;
$email      = $_POST['email'] ?? null;
$password   = $_POST['password'] ?? null;
$gcashNum   = $_POST['gcash_num'] ?? null;
$idType     = $_POST['valid_id_type'] ?? null;
$idNumber   = $_POST['id_umber'] ?? null;
$isAgreed   = $_POST['agreed_at'] ?? "0";

// Validate required fields
if (!$firstName || !$lastName || !$email || !$password) {
    echo "Error: Missing required fields.";
    exit;
}

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
$gcashQRPath = saveFile("gcashQR", $uploadDir);

// Insert into DB
$stmt = $conn->prepare("INSERT INTO registration
    (role, first_name, middle_name, last_name, birth_date, phone, address, email, password, gcash_num, valid_id_type, id_number, cb_agreed, idFrontFile, idBackFile, gcash_qr) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

    $stmt = $conn->prepare($sql);
        if (!$stmt) {
            die("SQL error: " . $conn->error);
        }

$stmt->bind_param("ssssssssssssssss",
    $role, $firstName, $middleName, $lastName, $birthDate,
    $phone, $address, $email, $password, $gcashNum,
    $idType, $idNumber, $isAgreed,
    $idFrontPath, $idBackPath, $gcashQRPath
);

if ($stmt->execute()) {
    echo "Registration successful!";
} else {
    echo "Database insert error: " . $stmt->error;
}

$stmt->close();
$conn->close();
?>
