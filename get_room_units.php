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

// Get bhr_id from POST request
$bhr_id = $_POST["bhr_id"] ?? null;

if (!$bhr_id) {
    echo json_encode(["success" => false, "error" => "Room ID is required"]);
    exit;
}

try {
    // Query to get room units for the specific room
    $sql = "SELECT room_number, status FROM room_units WHERE bhr_id = ? ORDER BY room_number";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $bhr_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $units = [];
    while ($row = $result->fetch_assoc()) {
        $units[] = [
            "room_number" => $row["room_number"],
            "status" => $row["status"]
        ];
    }

    echo json_encode([
        "success" => true, 
        "units" => $units,
        "total_units" => count($units)
    ]);

} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
}

$conn->close();
?>

































