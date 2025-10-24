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
$bhr_id = $_POST["bhr_id"] ?? null;
$category = $_POST["category"] ?? null;
$title = $_POST["title"] ?? null;
$room_description = $_POST["room_description"] ?? null;
$price = $_POST["price"] ?? null;
$capacity = $_POST["capacity"] ?? null;
$total_rooms = $_POST["total_rooms"] ?? null;
$room_units = $_POST["room_units"] ?? null; // JSON array of room units to update

// Debug logging
error_log("DEBUG: Update room parameters:");
error_log("bhr_id: " . $bhr_id);
error_log("category: " . $category);
error_log("title: " . $title);
error_log("room_description: '" . $room_description . "'");
error_log("price: " . $price);
error_log("capacity: " . $capacity);
error_log("total_rooms: " . $total_rooms);
error_log("room_units: " . $room_units);

// Validate required fields
if (!$bhr_id || !$category || !$title || !$price || !$capacity || !$total_rooms) {
    echo json_encode(["success" => false, "error" => "Required fields missing"]);
    exit;
}

try {
    $conn->begin_transaction();
    
    // Update room in database
    $sql = "UPDATE boarding_house_rooms 
            SET room_category = ?, room_name = ?, price = ?, capacity = ?, room_description = ?, total_rooms = ? 
            WHERE bhr_id = ?";
    
    $stmt = $conn->prepare($sql);
    // Fixed parameter binding: ssdisii (s=string, s=string, d=double, i=int, s=string, i=int, i=int)
    $stmt->bind_param("ssdisii", 
        $category, 
        $title, 
        $price, 
        $capacity, 
        $room_description, 
        $total_rooms, 
        $bhr_id
    );
    
    if (!$stmt->execute()) {
        throw new Exception("Failed to update room: " . $stmt->error);
    }
    $stmt->close();
    
    // Handle room units based on total_rooms change
    $unitsUpdated = 0;
    $unitsCreated = 0;
    $unitsDeleted = 0;
    
    // Get current room units count
    $countStmt = $conn->prepare("SELECT COUNT(*) as current_count FROM room_units WHERE bhr_id = ?");
    $countStmt->bind_param("i", $bhr_id);
    $countStmt->execute();
    $countResult = $countStmt->get_result();
    $currentCount = $countResult->fetch_assoc()['current_count'];
    $countStmt->close();
    
    $newTotalRooms = intval($total_rooms);
    
    if ($newTotalRooms > $currentCount) {
        // Need to create more room units
        $unitsToCreate = $newTotalRooms - $currentCount;
        
        // Get the highest existing unit number
        $maxStmt = $conn->prepare("SELECT room_number FROM room_units WHERE bhr_id = ? ORDER BY room_number DESC LIMIT 1");
        $maxStmt->bind_param("i", $bhr_id);
        $maxStmt->execute();
        $maxResult = $maxStmt->get_result();
        
        $lastUnitNumber = 0;
        if ($maxRow = $maxResult->fetch_assoc()) {
            $lastRoomNumber = $maxRow['room_number'];
            // Extract number from room number (e.g., "PR-5" -> 5)
            if (preg_match('/-(\d+)$/', $lastRoomNumber, $matches)) {
                $lastUnitNumber = intval($matches[1]);
            }
        }
        $maxStmt->close();
        
        // Generate prefix from room name
        $words = explode(' ', $title);
        $prefix = '';
        foreach ($words as $w) {
            if (!empty($w)) $prefix .= strtoupper($w[0]);
        }
        
        // Create new room units
        $createStmt = $conn->prepare("INSERT INTO room_units (bhr_id, room_number, status) VALUES (?, ?, 'Available')");
        for ($i = 1; $i <= $unitsToCreate; $i++) {
            $newUnitNumber = $lastUnitNumber + $i;
            $room_number = $prefix . '-' . $newUnitNumber;
            
            $createStmt->bind_param("is", $bhr_id, $room_number);
            if ($createStmt->execute()) {
                $unitsCreated++;
                error_log("DEBUG: Created new room unit: $room_number");
            }
        }
        $createStmt->close();
        
    } elseif ($newTotalRooms < $currentCount) {
        // Need to delete excess room units (only if they're not occupied)
        $unitsToDelete = $currentCount - $newTotalRooms;
        
        $deleteStmt = $conn->prepare("DELETE FROM room_units WHERE bhr_id = ? AND status != 'Occupied' ORDER BY room_number DESC LIMIT ?");
        $deleteStmt->bind_param("ii", $bhr_id, $unitsToDelete);
        if ($deleteStmt->execute()) {
            $unitsDeleted = $deleteStmt->affected_rows;
            error_log("DEBUG: Deleted $unitsDeleted room units");
        }
        $deleteStmt->close();
    }
    
    // Update room units status if provided
    if ($room_units) {
        $unitsArray = json_decode($room_units, true);
        
        if (is_array($unitsArray)) {
            foreach ($unitsArray as $unit) {
                $room_number = $unit['room_number'] ?? null;
                $new_status = $unit['status'] ?? null;
                
                if ($room_number && $new_status) {
                    // Validate status
                    $validStatuses = ['Available', 'Occupied', 'Maintenance', 'Reserved'];
                    if (in_array($new_status, $validStatuses)) {
                        $unitStmt = $conn->prepare("UPDATE room_units SET status = ? WHERE bhr_id = ? AND room_number = ?");
                        $unitStmt->bind_param("sis", $new_status, $bhr_id, $room_number);
                        
                        if ($unitStmt->execute() && $unitStmt->affected_rows > 0) {
                            $unitsUpdated++;
                            error_log("DEBUG: Updated room unit $room_number to status: $new_status");
                        }
                        $unitStmt->close();
                    }
                }
            }
        }
    }
    
    $conn->commit();
    
    $message = "Room updated successfully";
    $changes = [];
    
    if ($unitsCreated > 0) {
        $changes[] = "$unitsCreated unit(s) created";
    }
    if ($unitsDeleted > 0) {
        $changes[] = "$unitsDeleted unit(s) deleted";
    }
    if ($unitsUpdated > 0) {
        $changes[] = "$unitsUpdated unit(s) status updated";
    }
    
    if (!empty($changes)) {
        $message .= " - " . implode(", ", $changes);
    }
    
    echo json_encode([
        "success" => true, 
        "message" => $message,
        "units_created" => $unitsCreated,
        "units_deleted" => $unitsDeleted,
        "units_updated" => $unitsUpdated
    ]);
    
} catch (Exception $e) {
    $conn->rollback();
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
}

$conn->close();
?>








