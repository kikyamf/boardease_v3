<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Database configuration
$host = 'localhost';
$dbname = 'boardease_testing';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get the email from URL parameter
    $email = $_GET['email'] ?? null;
    
    if (!$email) {
        echo json_encode([
            'success' => false,
            'message' => 'Please provide email parameter: ?email=your@email.com'
        ]);
        exit();
    }
    
    // Update user status to approved
    $sql = "UPDATE registration SET status = 'approved' WHERE email = :email";
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':email', $email, PDO::PARAM_STR);
    $result = $stmt->execute();
    
    if ($result && $stmt->rowCount() > 0) {
        echo json_encode([
            'success' => true,
            'message' => "User with email '$email' has been approved and can now login"
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => "No user found with email '$email' or user is already approved"
        ]);
    }
    
} catch(PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
