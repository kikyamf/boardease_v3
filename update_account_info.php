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
$email = $_POST["email"] ?? null;
$current_password = $_POST["current_password"] ?? "";
$new_password = $_POST["new_password"] ?? "";

// Validate required fields
if (!$user_id || !$email) {
    echo json_encode(["success" => false, "error" => "Required fields missing"]);
    exit;
}

try {
    $conn->begin_transaction();
    
    // Check if password change is requested
    if (!empty($new_password)) {
        if (empty($current_password)) {
            echo json_encode(["success" => false, "error" => "Current password is required to change password"]);
            exit;
        }
        
        // Verify current password
        $checkSql = "SELECT r.password FROM registrations r 
                     JOIN users u ON r.id = u.reg_id 
                     WHERE u.user_id = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->bind_param("i", $user_id);
        $checkStmt->execute();
        $checkResult = $checkStmt->get_result();
        
        if ($checkResult->num_rows > 0) {
            $row = $checkResult->fetch_assoc();
            $storedPassword = $row["password"];
            
            // Debug: Check if password is hashed or plain text
            $isHashed = password_get_info($storedPassword)['algo'] !== null;
            
            if ($isHashed) {
                // Password is hashed, use password_verify
                if (!password_verify($current_password, $storedPassword)) {
                    echo json_encode(["success" => false, "error" => "Current password is incorrect"]);
                    exit;
                }
            } else {
                // Password is plain text, compare directly
                if ($current_password !== $storedPassword) {
                    echo json_encode(["success" => false, "error" => "Current password is incorrect"]);
                    exit;
                }
            }
        } else {
            echo json_encode(["success" => false, "error" => "User not found"]);
            exit;
        }
        
        // Update password
        $hashedPassword = password_hash($new_password, PASSWORD_DEFAULT);
        $updatePasswordSql = "UPDATE registrations r 
                              JOIN users u ON r.id = u.reg_id 
                              SET r.password = ? 
                              WHERE u.user_id = ?";
        $updatePasswordStmt = $conn->prepare($updatePasswordSql);
        $updatePasswordStmt->bind_param("si", $hashedPassword, $user_id);
        $updatePasswordStmt->execute();
        $updatePasswordStmt->close();
    }
    
    // Update email
    $updateSql = "UPDATE registrations r 
                  JOIN users u ON r.id = u.reg_id 
                  SET r.email = ? 
                  WHERE u.user_id = ?";
    $updateStmt = $conn->prepare($updateSql);
    $updateStmt->bind_param("si", $email, $user_id);
    
    if ($updateStmt->execute()) {
        $conn->commit();
        echo json_encode([
            "success" => true,
            "message" => "Account information updated successfully"
        ]);
    } else {
        $conn->rollback();
        echo json_encode(["success" => false, "error" => "Failed to update account information"]);
    }
    
    $updateStmt->close();
    
} catch (Exception $e) {
    $conn->rollback();
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
} finally {
    $conn->close();
}
?>
























