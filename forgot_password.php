<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'boardease_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
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
if (!isset($input['email']) || empty(trim($input['email']))) {
    echo json_encode([
        'success' => false,
        'message' => 'Email is required'
    ]);
    exit();
}

$email = trim($input['email']);

// Validate email format
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email format'
    ]);
    exit();
}

try {
    // Check if user exists
    $sql_check = "SELECT id, first_name, last_name, email, status FROM registration WHERE email = :email";
    $stmt_check = $pdo->prepare($sql_check);
    $stmt_check->bindParam(':email', $email, PDO::PARAM_STR);
    $stmt_check->execute();
    $user = $stmt_check->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        echo json_encode([
            'success' => false,
            'message' => 'Email not found in our system'
        ]);
        exit();
    }
    
    // Check if user is approved
    if ($user['status'] !== 'approved') {
        echo json_encode([
            'success' => false,
            'message' => 'Account not approved. Please contact support.'
        ]);
        exit();
    }
    
    // Generate a simple reset token (in production, use a more secure method)
    $reset_token = bin2hex(random_bytes(32));
    $reset_expires = date('Y-m-d H:i:s', strtotime('+1 hour')); // Token expires in 1 hour
    
    // Store reset token in database (you might want to create a separate table for this)
    // For now, we'll use a simple approach and store it in a session or temporary table
    // In a real application, you would:
    // 1. Create a password_reset_tokens table
    // 2. Store the token with expiration time
    // 3. Send an email with the reset link
    
    // For this demo, we'll just simulate sending an email
    $user_name = $user['first_name'] . ' ' . $user['last_name'];
    
    // In a real application, you would send an actual email here
    // For now, we'll just log the reset request
    error_log("Password reset requested for: " . $email);
    error_log("Reset token: " . $reset_token);
    error_log("Token expires: " . $reset_expires);
    
    // Simulate email sending (in production, use PHPMailer or similar)
    $reset_link = "http://192.168.1.3/boardease_v3/reset_password.php?token=" . $reset_token;
    
    // Log the reset link for testing purposes
    error_log("Reset link: " . $reset_link);
    
    echo json_encode([
        'success' => true,
        'message' => 'Password reset instructions have been sent to your email address. Please check your inbox and follow the instructions to reset your password.',
        'reset_token' => $reset_token, // Only for testing - remove in production
        'reset_link' => $reset_link   // Only for testing - remove in production
    ]);
    
} catch (PDOException $e) {
    error_log("Database error in forgot_password.php: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Database error occurred. Please try again later.'
    ]);
} catch (Exception $e) {
    error_log("General error in forgot_password.php: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'An error occurred. Please try again later.'
    ]);
}
?>
