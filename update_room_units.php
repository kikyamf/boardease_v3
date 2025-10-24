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
$room_units = $_POST["room_units"] ?? null; // JSON array of room units with their new status

if (!$bhr_id || !$room_units) {
    echo json_encode(["success" => false, "error" => "Room ID and room units data are required"]);
    exit;
}

// Debug logging
error_log("DEBUG: Update room units parameters:");
error_log("bhr_id: " . $bhr_id);
error_log("room_units: " . $room_units);

try {
    // Decode the JSON array of room units
    $unitsArray = json_decode($room_units, true);
    
    if (!is_array($unitsArray)) {
        echo json_encode(["success" => false, "error" => "Invalid room units data format"]);
        exit;
    }
    
    $conn->begin_transaction();
    
    $updatedCount = 0;
    $errors = [];
    
    // Update each room unit
    foreach ($unitsArray as $unit) {
        $room_number = $unit['room_number'] ?? null;
        $new_status = $unit['status'] ?? null;
        
        if (!$room_number || !$new_status) {
            $errors[] = "Missing room_number or status for unit: " . json_encode($unit);
            continue;
        }
        
        // Validate status
        $validStatuses = ['Available', 'Occupied', 'Maintenance', 'Reserved'];
        if (!in_array($new_status, $validStatuses)) {
            $errors[] = "Invalid status '$new_status' for room $room_number. Valid statuses: " . implode(', ', $validStatuses);
            continue;
        }
        
        // Update the room unit
        $stmt = $conn->prepare("UPDATE room_units SET status = ? WHERE bhr_id = ? AND room_number = ?");
        $stmt->bind_param("sis", $new_status, $bhr_id, $room_number);
        
        if ($stmt->execute()) {
            if ($stmt->affected_rows > 0) {
                $updatedCount++;
                error_log("DEBUG: Updated room $room_number to status: $new_status");
            } else {
                $errors[] = "No room unit found with number '$room_number' for room ID $bhr_id";
            }
        } else {
            $errors[] = "Failed to update room $room_number: " . $stmt->error;
        }
        
        $stmt->close();
    }
    
    if (empty($errors)) {
        $conn->commit();
        echo json_encode([
            "success" => true, 
            "message" => "Successfully updated $updatedCount room unit(s)",
            "updated_count" => $updatedCount
        ]);
    } else {
        $conn->rollback();
        echo json_encode([
            "success" => false, 
            "error" => "Some updates failed",
            "errors" => $errors,
            "updated_count" => $updatedCount
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

































