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
$room_numbers = $_POST["room_numbers"] ?? null; // JSON array of room numbers to delete

if (!$bhr_id || !$room_numbers) {
    echo json_encode(["success" => false, "error" => "Room ID and room numbers are required"]);
    exit;
}

// Debug logging
error_log("DEBUG: Delete room units parameters:");
error_log("bhr_id: " . $bhr_id);
error_log("room_numbers: " . $room_numbers);

try {
    // Decode the JSON array of room numbers
    $numbersArray = json_decode($room_numbers, true);
    
    if (!is_array($numbersArray)) {
        echo json_encode(["success" => false, "error" => "Invalid room numbers data format"]);
        exit;
    }
    
    $conn->begin_transaction();
    
    $deletedCount = 0;
    $errors = [];
    
    // Delete each room unit
    foreach ($numbersArray as $room_number) {
        if (empty($room_number)) {
            $errors[] = "Empty room number provided";
            continue;
        }
        
        // Check if room unit is occupied before deleting
        $checkStmt = $conn->prepare("SELECT status FROM room_units WHERE bhr_id = ? AND room_number = ?");
        $checkStmt->bind_param("is", $bhr_id, $room_number);
        $checkStmt->execute();
        $result = $checkStmt->get_result();
        
        if ($row = $result->fetch_assoc()) {
            if ($row['status'] === 'Occupied') {
                $errors[] = "Cannot delete occupied room unit: $room_number";
                $checkStmt->close();
                continue;
            }
        } else {
            $errors[] = "Room unit not found: $room_number";
            $checkStmt->close();
            continue;
        }
        $checkStmt->close();
        
        // Delete the room unit
        $stmt = $conn->prepare("DELETE FROM room_units WHERE bhr_id = ? AND room_number = ?");
        $stmt->bind_param("is", $bhr_id, $room_number);
        
        if ($stmt->execute()) {
            if ($stmt->affected_rows > 0) {
                $deletedCount++;
                error_log("DEBUG: Deleted room unit: $room_number");
            } else {
                $errors[] = "No room unit found with number '$room_number' for room ID $bhr_id";
            }
        } else {
            $errors[] = "Failed to delete room $room_number: " . $stmt->error;
        }
        
        $stmt->close();
    }
    
    if (empty($errors)) {
        $conn->commit();
        echo json_encode([
            "success" => true, 
            "message" => "Successfully deleted $deletedCount room unit(s)",
            "deleted_count" => $deletedCount
        ]);
    } else {
        $conn->rollback();
        echo json_encode([
            "success" => false, 
            "error" => "Some deletions failed",
            "errors" => $errors,
            "deleted_count" => $deletedCount
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
































