<?php
// BoardEase2/submit_review.php
// Updated to use Live DB credentials and mysqli for consistency.

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }

    // Get input
    $json = file_get_contents('php://input');
    $input = json_decode($json, true) ?? $_POST;

    $user_id = isset($input["user_id"]) ? intval($input["user_id"]) : 0;
    $bh_id = isset($input["bh_id"]) ? intval($input["bh_id"]) : 0;
    $rating = isset($input["rating"]) ? intval($input["rating"]) : 0;
    $comment = trim($input["comment"] ?? '');

    if ($user_id <= 0 || $bh_id <= 0 || $rating < 1 || $rating > 5 || empty($comment)) {
        throw new Exception("Missing or invalid required fields (User, BH, Rating, or Comment)", 400);
    }

    // Insert Review
    $sql = "INSERT INTO reviews (user_id, bh_id, rating, comment, review_created_at) VALUES (?, ?, ?, ?, NOW())";
    $stmt = $conn->prepare($sql);
    
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param("iiis", $user_id, $bh_id, $rating, $comment);
    
    if ($stmt->execute()) {
        echo json_encode([
            "success" => true,
            "message" => "Review submitted successfully",
            "review_id" => $conn->insert_id
        ]);
    } else {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    $stmt->close();
    $conn->close();

} catch (Exception $e) {
    if (isset($conn) && $conn->connect_errno == 0 && $conn->ping()) {
        $conn->close();
    }
    http_response_code($e->getCode() >= 400 ? $e->getCode() : 500);
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>
