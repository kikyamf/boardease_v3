<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
$host = 'localhost';
$dbname = 'boardease_testing'; // Your actual database name
$username = 'root'; // Your actual database username
$password = ''; // Your actual database password

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
    exit();
}

// Check if request method is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        'success' => false,
        'message' => 'Only POST method is allowed'
    ]);
    exit();
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// If JSON input is not available, try form data
if (!$input) {
    $input = $_POST;
}

// Validate required fields
if (!isset($input['email']) || !isset($input['password'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Email and password are required'
    ]);
    exit();
}

$email = trim($input['email']);
$password = trim($input['password']);

// Validate email format
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email format'
    ]);
    exit();
}

try {
    // Debug: Log the received email
    error_log("Login attempt for email: " . $email);
    
    // First, check if user exists (without status filter for debugging)
    $sql_check = "SELECT id, email, status FROM registration WHERE email = :email";
    $stmt_check = $pdo->prepare($sql_check);
    $stmt_check->bindParam(':email', $email, PDO::PARAM_STR);
    $stmt_check->execute();
    $user_check = $stmt_check->fetch(PDO::FETCH_ASSOC);
    
    if (!$user_check) {
        error_log("User not found in database for email: " . $email);
        echo json_encode([
            'success' => false,
            'message' => 'Email not found in database'
        ]);
        exit();
    }
    
    error_log("User found - ID: " . $user_check['id'] . ", Status: " . $user_check['status']);
    
    // Now check with status filter
    $sql = "SELECT id, first_name, last_name, middle_name, email, password, role, 
                   phone, address, birth_date, gcash_num, gcash_qr, valid_id_type, 
                   id_number, idFrontFile, idBackFile, status
            FROM registration 
            WHERE email = :email AND status = 'approved'";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':email', $email, PDO::PARAM_STR);
    $stmt->execute();
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        error_log("User found but status is not 'approved'. Current status: " . $user_check['status']);
        echo json_encode([
            'success' => false,
            'message' => 'Account not approved. Current status: ' . $user_check['status']
        ]);
        exit();
    }
    
    // Verify password - plain text comparison (since passwords are not hashed yet)
    error_log("Comparing passwords - Input: '" . $password . "' vs Database: '" . $user['password'] . "'");
    $passwordMatch = ($password === $user['password']);
    
    if (!$passwordMatch) {
        error_log("Password mismatch for user: " . $email);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid password'
        ]);
        exit();
    }
    
    // Prepare user data for response (exclude password)
    $userData = [
        'id' => $user['id'],
        'firstName' => $user['first_name'],
        'lastName' => $user['last_name'],
        'middleName' => $user['middle_name'],
        'email' => $user['email'],
        'role' => $user['role'],
        'phone' => $user['phone'],
        'address' => $user['address'],
        'birthDate' => $user['birth_date'],
        'gcashNumber' => $user['gcash_num'],
        'qrCodePath' => $user['gcash_qr'],
        'validIdType' => $user['valid_id_type'],
        'idNumber' => $user['id_number'],
        'idFrontFile' => $user['idFrontFile'],
        'idBackFile' => $user['idBackFile'],
        'status' => $user['status']
    ];
    
    // Return success response with user data
    echo json_encode([
        'success' => true,
        'message' => 'Login successful',
        'user' => $userData
    ]);
    
} catch(PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch(Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}
?>