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

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die(json_encode(["success" => false, "error" => "Connection failed: " . $conn->connect_error]));
}

// Get parameters from POST request
$bhr_id = $_POST["bhr_id"] ?? null;
$room_name = $_POST["room_name"] ?? null;
$additional_units = $_POST["additional_units"] ?? null; // Number of additional units to add

if (!$bhr_id || !$room_name || !$additional_units) {
    echo json_encode(["success" => false, "error" => "Room ID, room name, and additional units count are required"]);
    exit;
}

// Debug logging
error_log("DEBUG: Add room units parameters:");
error_log("bhr_id: " . $bhr_id);
error_log("room_name: " . $room_name);
error_log("additional_units: " . $additional_units);

try {
    $conn->begin_transaction();
    
    // Get current highest unit number for this room
    $stmt = $conn->prepare("SELECT room_number FROM room_units WHERE bhr_id = ? ORDER BY room_number DESC LIMIT 1");
    $stmt->bind_param("i", $bhr_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $lastUnitNumber = 0;
    if ($row = $result->fetch_assoc()) {
        $lastRoomNumber = $row['room_number'];
        // Extract number from room number (e.g., "PR-5" -> 5)
        if (preg_match('/-(\d+)$/', $lastRoomNumber, $matches)) {
            $lastUnitNumber = intval($matches[1]);
        }
    }
    $stmt->close();
    
    // Generate prefix from room name
    $words = explode(' ', $room_name);
    $prefix = '';
    foreach ($words as $w) {
        if (!empty($w)) $prefix .= strtoupper($w[0]);
    }
    
    // Add new room units
    $stmt = $conn->prepare("INSERT INTO room_units (bhr_id, room_number, status) VALUES (?, ?, 'Available')");
    $addedCount = 0;
    
    for ($i = 1; $i <= intval($additional_units); $i++) {
        $newUnitNumber = $lastUnitNumber + $i;
        $room_number = $prefix . '-' . $newUnitNumber;
        
        $stmt->bind_param("is", $bhr_id, $room_number);
        
        if ($stmt->execute()) {
            $addedCount++;
            error_log("DEBUG: Added room unit: $room_number");
        } else {
            error_log("DEBUG: Failed to add room unit: $room_number - " . $stmt->error);
        }
    }
    
    $stmt->close();
    
    if ($addedCount > 0) {
        $conn->commit();
        echo json_encode([
            "success" => true, 
            "message" => "Successfully added $addedCount room unit(s)",
            "added_count" => $addedCount
        ]);
    } else {
        $conn->rollback();
        echo json_encode([
            "success" => false, 
            "error" => "Failed to add any room units"
        ]);
    }
    
} catch (Exception $e) {
    $conn->rollback();
    echo json_encode([
        "success" => false, 
        "error" => "Database error: " . $e->getMessage()
    ]);
} finally {
    $conn->close();
}
?>























