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
$user_id = isset($input["user_id"]) ? intval($input["user_id"]) : (isset($_POST["user_id"]) ? intval($_POST["user_id"]) : null);
$bh_id = isset($input["bh_id"]) ? intval($input["bh_id"]) : (isset($_POST["bh_id"]) ? intval($_POST["bh_id"]) : null);
$rating = isset($input["rating"]) ? intval($input["rating"]) : (isset($_POST["rating"]) ? intval($_POST["rating"]) : null);
$comment = $input["comment"] ?? $_POST["comment"] ?? null;

// Validate required fields
if (!$user_id || $user_id <= 0) {
    echo json_encode(["success" => false, "error" => "User ID is required and must be a positive integer"]);
    exit;
}

if (!$bh_id || $bh_id <= 0) {
    echo json_encode(["success" => false, "error" => "Boarding House ID is required and must be a positive integer"]);
    exit;
}

if (!$rating || $rating < 1 || $rating > 5) {
    echo json_encode(["success" => false, "error" => "Rating is required and must be between 1 and 5"]);
    exit;
}

if (!$comment || trim($comment) === '') {
    echo json_encode(["success" => false, "error" => "Comment is required"]);
    exit;
}

try {
    // Prepare SQL statement
    // review_created_at will be set automatically if it's a TIMESTAMP with DEFAULT CURRENT_TIMESTAMP
    $stmt = $conn->prepare("INSERT INTO reviews (user_id, bh_id, rating, comment) VALUES (?, ?, ?, ?)");
    
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    // Bind parameters: user_id (int), bh_id (int), rating (int), comment (string)
    $stmt->bind_param("iiis", $user_id, $bh_id, $rating, $comment);
    
    // Execute statement
    if ($stmt->execute()) {
        $review_id = $conn->insert_id;
        
        echo json_encode([
            "success" => true,
            "message" => "Review submitted successfully",
            "review_id" => $review_id
        ]);
    } else {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    $stmt->close();
    
} catch (Exception $e) {
    error_log("Error submitting review: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "error" => "Failed to submit review: " . $e->getMessage()
    ]);
} finally {
    $conn->close();
}

?>

