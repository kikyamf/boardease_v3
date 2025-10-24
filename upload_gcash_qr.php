<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Increase limits for image upload
ini_set('upload_max_filesize', '10M');
ini_set('post_max_size', '10M');
ini_set('max_execution_time', 300);
ini_set('memory_limit', '256M');

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
$gcash_qr = $_POST["gcash_qr"] ?? null;

// Debug logging
error_log("DEBUG: Upload GCash QR - user_id: " . $user_id);
error_log("DEBUG: GCash QR length: " . strlen($gcash_qr));

// Validate required fields
if (!$user_id || !$gcash_qr) {
    echo json_encode(["success" => false, "error" => "User ID and GCash QR are required"]);
    exit;
}

// Validate base64 data length (max 5MB)
if (strlen($gcash_qr) > 5 * 1024 * 1024) {
    echo json_encode(["success" => false, "error" => "Image too large. Maximum size is 5MB."]);
    exit;
}

try {
    // Decode base64 image
    $imageData = base64_decode($gcash_qr);
    if ($imageData === false) {
        echo json_encode(["success" => false, "error" => "Invalid image data"]);
        exit;
    }
    
    // Generate unique filename
    $filename = "gcash_qr_" . $user_id . "_" . time() . ".jpg";
    $filepath = "uploads/gcash_qr/" . $filename;
    $fullPath = __DIR__ . "/" . $filepath;
    
    // Ensure directory exists
    $uploadDir = __DIR__ . "/uploads/gcash_qr/";
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }
    
    // Save image file
    if (file_put_contents($fullPath, $imageData) === false) {
        echo json_encode(["success" => false, "error" => "Failed to save image file"]);
        exit;
    }
    
    // Update database - Fixed to use correct table structure
    $sql = "UPDATE registrations r 
            JOIN users u ON r.id = u.reg_id 
            SET r.gcash_qr = ? 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("si", $filepath, $user_id);
    
    if ($stmt->execute()) {
        // Get user information for response
        $userSql = "SELECT r.first_name, r.last_name, r.email, r.gcash_num 
                   FROM users u 
                   JOIN registrations r ON u.reg_id = r.id 
                   WHERE u.user_id = ?";
        $userStmt = $conn->prepare($userSql);
        $userStmt->bind_param("i", $user_id);
        $userStmt->execute();
        $userResult = $userStmt->get_result();
        $userData = $userResult->fetch_assoc();
        $userStmt->close();
        
        echo json_encode([
            "success" => true,
            "message" => "GCash QR code uploaded successfully",
            "gcash_qr_path" => $filepath,
            "user_info" => [
                "user_id" => $user_id,
                "name" => ($userData ? $userData['first_name'] . " " . $userData['last_name'] : "Unknown"),
                "email" => ($userData ? $userData['email'] : "Unknown"),
                "gcash_number" => ($userData ? $userData['gcash_num'] : "Not set")
            ]
        ]);
    } else {
        // Delete the uploaded file if database update fails
        unlink($fullPath);
        echo json_encode(["success" => false, "error" => "Failed to update database: " . $stmt->error]);
    }
    
    $stmt->close();
    
} catch (Exception $e) {
    error_log("ERROR: Upload GCash QR failed: " . $e->getMessage());
    echo json_encode(["success" => false, "error" => "Upload failed: " . $e->getMessage()]);
} finally {
    $conn->close();
}
?>