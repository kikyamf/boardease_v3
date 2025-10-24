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

// Get parameters from POST request
$user_id = $_POST["user_id"] ?? null;
$f_name = $_POST["f_name"] ?? null;
$m_name = $_POST["m_name"] ?? null;
$l_name = $_POST["l_name"] ?? null;
$birthdate = $_POST["birthdate"] ?? null;
$phone_number = $_POST["phone_number"] ?? null;
$p_address = $_POST["p_address"] ?? null;
$profile_picture = $_POST["profile_picture"] ?? null;

// Debug logging
error_log("DEBUG: Update owner profile parameters:");
error_log("user_id: " . $user_id);
error_log("f_name: " . $f_name);
error_log("m_name: " . $m_name);
error_log("l_name: " . $l_name);
error_log("birthdate: " . $birthdate);
error_log("phone_number: " . $phone_number);
error_log("p_address: " . $p_address);
error_log("profile_picture: " . ($profile_picture ? "provided" : "not provided"));

// Validate required fields
if (!$user_id || !$f_name || !$l_name || !$birthdate || !$phone_number || !$p_address) {
    echo json_encode(["success" => false, "error" => "Required fields missing"]);
    exit;
}

try {
    $conn->begin_transaction();
    
    // Update registrations table with profile data
    $sql_registrations = "UPDATE registrations r 
                        JOIN users u ON r.id = u.reg_id 
                        SET r.first_name = ?, r.middle_name = ?, r.last_name = ?, r.birth_date = ?, r.phone = ?, r.address = ?
                        WHERE u.user_id = ?";
    
    $stmt_reg = $conn->prepare($sql_registrations);
    $stmt_reg->bind_param("ssssssi", $f_name, $m_name, $l_name, $birthdate, $phone_number, $p_address, $user_id);
    
    if (!$stmt_reg->execute()) {
        throw new Exception("Failed to update registrations: " . $stmt_reg->error);
    }
    $stmt_reg->close();
    
    // Update users table with profile picture
    if ($profile_picture) {
        $sql_users = "UPDATE users SET profile_picture = ? WHERE user_id = ?";
        $stmt_users = $conn->prepare($sql_users);
        $stmt_users->bind_param("si", $profile_picture, $user_id);
        
        if (!$stmt_users->execute()) {
            throw new Exception("Failed to update profile picture: " . $stmt_users->error);
        }
        $stmt_users->close();
    }
    
    $conn->commit();
    
    echo json_encode([
        "success" => true,
        "message" => "Profile updated successfully"
    ]);
    
} catch (Exception $e) {
    $conn->rollback();
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
}

$conn->close();
?>



































