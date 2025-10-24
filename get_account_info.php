<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Database configuration
$servername = "localhost";
$username = "boardease";
$password = "boardease";
$database = "boardease2";

$conn = new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
    die(json_encode(["success" => false, "error" => "Connection failed: " . $conn->connect_error]));
}

// Get user_id from POST request
$user_id = $_POST["user_id"] ?? null;

if (!$user_id) {
    echo json_encode(["success" => false, "error" => "User ID is required"]);
    exit;
}

try {
    // Get account information from registrations table
    $sql = "SELECT r.email, r.gcash_num, r.gcash_qr 
            FROM registrations r 
            JOIN users u ON r.id = u.reg_id 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        
        echo json_encode([
            "success" => true,
            "email" => $row["email"],
            "gcash_number" => $row["gcash_num"],
            "gcash_qr" => $row["gcash_qr"]
        ]);
    } else {
        echo json_encode(["success" => false, "error" => "User not found"]);
    }
    
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
} finally {
    $conn->close();
}
?>



























