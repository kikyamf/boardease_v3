<?php
// login.php

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Set content type to JSON
header('Content-Type: application/json');

// Database connection
$servername = "localhost";
$username   = "root";
$password   = "";
$dbname     = "boardease_testing";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    $response = array(
        "success" => false,
        "message" => "Database connection failed: " . $conn->connect_error
    );
    echo json_encode($response);
    exit;
}

// Get POST data
$email = $_POST['email'] ?? null;
$password = $_POST['password'] ?? null;

// Validate input
if (!$email || !$password) {
    $response = array(
        "success" => false,
        "message" => "Email and password are required"
    );
    echo json_encode($response);
    exit;
}

// Sanitize input
$email = trim($email);
$password = trim($password);

// Prepare SQL statement to prevent SQL injection
$stmt = $conn->prepare("SELECT id, role, first_name, last_name, email, password FROM registration WHERE email = ?");
if (!$stmt) {
    $response = array(
        "success" => false,
        "message" => "Database error: " . $conn->error
    );
    echo json_encode($response);
    exit;
}

$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $response = array(
        "success" => false,
        "message" => "Invalid email or password"
    );
    echo json_encode($response);
} else {
    $user = $result->fetch_assoc();
    
    // Verify password
    // Check if password is hashed (starts with $2y$) or plain text
    if (strpos($user['password'], '$2y$') === 0) {
        // Password is hashed, use password_verify
        $passwordValid = password_verify($password, $user['password']);
    } else {
        // Password is plain text (for backward compatibility)
        $passwordValid = ($password === $user['password']);
    }
    
    if ($passwordValid) {
        $response = array(
            "success" => true,
            "message" => "Login successful",
            "user" => array(
                "id" => $user['id'],
                "role" => $user['role'],
                "firstName" => $user['first_name'],
                "lastName" => $user['last_name'],
                "email" => $user['email']
            )
        );
        echo json_encode($response);
    } else {
        $response = array(
            "success" => false,
            "message" => "Invalid email or password"
        );
        echo json_encode($response);
    }
}

$stmt->close();
$conn->close();
?>
