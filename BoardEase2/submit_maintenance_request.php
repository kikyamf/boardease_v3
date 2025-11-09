<?php
// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept");

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

// Get JSON input
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);

// If JSON decode failed, try to get from POST
if ($input === null && !empty($_POST)) {
    $input = $_POST;
}

// Get parameters from JSON or POST request
$user_id = $input["user_id"] ?? $_POST["user_id"] ?? null;
$room_id = isset($input["room_id"]) ? ($input["room_id"] === null ? null : intval($input["room_id"])) : (isset($_POST["room_id"]) ? intval($_POST["room_id"]) : null);
$subject = $input["subject"] ?? $_POST["subject"] ?? null;
$area_for_maintenance = $input["area_for_maintenance"] ?? $_POST["area_for_maintenance"] ?? null;
$description = $input["description"] ?? $_POST["description"] ?? $_POST["mr_description"] ?? null;

// Validate required fields
if (!$user_id) {
    echo json_encode(["success" => false, "error" => "User ID is required"]);
    exit;
}

if (!$subject || trim($subject) === '') {
    echo json_encode(["success" => false, "error" => "Subject is required"]);
    exit;
}

if (!$area_for_maintenance || trim($area_for_maintenance) === '') {
    echo json_encode(["success" => false, "error" => "Area for maintenance is required"]);
    exit;
}

if (!$description || trim($description) === '') {
    echo json_encode(["success" => false, "error" => "Description is required"]);
    exit;
}

// Validate area_for_maintenance value
$valid_areas = ['BH Room', 'Bathroom', 'Kitchen', 'Others'];
if (!in_array($area_for_maintenance, $valid_areas)) {
    echo json_encode(["success" => false, "error" => "Invalid area for maintenance. Must be one of: " . implode(', ', $valid_areas)]);
    exit;
}

try {
    // Prepare SQL statement
    // room_id can be NULL, so we handle it conditionally
    if ($room_id && $room_id > 0) {
        $stmt = $conn->prepare("INSERT INTO maintenance_requests (user_id, room_id, subject, area_for_maintenance, mr_description, mr_status) VALUES (?, ?, ?, ?, ?, 'Pending')");
        if (!$stmt) {
            throw new Exception("Prepare failed: " . $conn->error);
        }
        $stmt->bind_param("iisss", $user_id, $room_id, $subject, $area_for_maintenance, $description);
    } else {
        // If room_id is not provided or is 0, set it to NULL
        $stmt = $conn->prepare("INSERT INTO maintenance_requests (user_id, room_id, subject, area_for_maintenance, mr_description, mr_status) VALUES (?, NULL, ?, ?, ?, 'Pending')");
        if (!$stmt) {
            throw new Exception("Prepare failed: " . $conn->error);
        }
        $stmt->bind_param("isss", $user_id, $subject, $area_for_maintenance, $description);
    }
    
    // Execute statement
    if ($stmt->execute()) {
        $request_id = $conn->insert_id;
        
        echo json_encode([
            "success" => true,
            "message" => "Maintenance request submitted successfully",
            "request_id" => $request_id
        ]);
    } else {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    $stmt->close();
    
} catch (Exception $e) {
    error_log("Error submitting maintenance request: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "error" => "Failed to submit maintenance request: " . $e->getMessage()
    ]);
} finally {
    $conn->close();
}

?>

