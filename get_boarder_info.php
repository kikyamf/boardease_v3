<?php
// get_boarder_info.php
// Fetches boarder profile information from the database

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning');

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Database configuration
$servername = "localhost";
$username   = "boardease";
$password   = "boardease";
$dbname     = "boardease2";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    $errorMsg = "Database connection failed: " . $conn->connect_error;
    error_log("get_boarder_info failed: " . $errorMsg);
    $response = array(
        "success" => false,
        "error" => $errorMsg
    );
    echo json_encode($response);
    exit;
}

// Get user_id from POST or GET
$user_id = isset($_POST['user_id']) ? $_POST['user_id'] : (isset($_GET['user_id']) ? $_GET['user_id'] : null);

// Validate input
if (!$user_id) {
    $response = array(
        "success" => false,
        "error" => "User ID is required"
    );
    echo json_encode($response);
    exit;
}

// Sanitize input
$user_id = intval($user_id);

// Prepare SQL statement to get boarder information
// Join users table with registrations table using user_id -> reg_id
$stmt = $conn->prepare("SELECT r.id, r.first_name, r.middle_name, r.last_name, r.suffix, 
                              r.email, r.phone, r.birth_date, r.address
                       FROM users u
                       INNER JOIN registrations r ON u.reg_id = r.id
                       WHERE u.user_id = ? AND r.role = 'Boarder'");

if (!$stmt) {
    $response = array(
        "success" => false,
        "error" => "Database error: " . $conn->error
    );
    echo json_encode($response);
    exit;
}

$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $response = array(
        "success" => false,
        "error" => "Boarder not found"
    );
    echo json_encode($response);
} else {
    $boarder = $result->fetch_assoc();
    
    // Format birthdate if it exists
    $birthdate = null;
    if ($boarder['birth_date']) {
        $birthdate = $boarder['birth_date'];
    }
    
    // Handle suffix - convert null/empty/null string to "none", or normalize "none" variants
    $suffix = $boarder['suffix'];
    if ($suffix === null || $suffix === '' || strtolower(trim($suffix)) === 'null') {
        $suffix = 'none';
    } else {
        // Normalize any "none" variant to lowercase "none"
        $suffixLower = strtolower(trim($suffix));
        if ($suffixLower === 'none') {
            $suffix = 'none';
        }
    }
    
    // Build response with all required fields
    $response = array(
        "success" => true,
        "data" => array(
            "first_name" => $boarder['first_name'] ? $boarder['first_name'] : "",
            "middle_name" => $boarder['middle_name'] ? $boarder['middle_name'] : "",
            "last_name" => $boarder['last_name'] ? $boarder['last_name'] : "",
            "suffix" => $suffix,
            "email" => $boarder['email'] ? $boarder['email'] : "",
            "contact" => $boarder['phone'] ? $boarder['phone'] : "",
            "birthdate" => $birthdate ? $birthdate : "",
            "address" => $boarder['address'] ? $boarder['address'] : ""
        )
    );
    
    echo json_encode($response);
}

$stmt->close();
$conn->close();
?>
