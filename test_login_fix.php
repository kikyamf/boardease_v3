<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Test the corrected database configuration
$host = 'localhost';
$dbname = 'boardease_testing';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo json_encode([
        'success' => true,
        'message' => 'Database connection successful with fixed credentials',
        'database' => $dbname,
        'username' => $username
    ]);
    
    // Check if registration table exists and get user count
    $sql = "SELECT COUNT(*) as user_count FROM registration";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo "\n\nUser count in registration table: " . $result['user_count'];
    
    // Check for approved users
    $sql = "SELECT COUNT(*) as approved_count FROM registration WHERE status = 'approved'";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo "\nApproved users count: " . $result['approved_count'];
    
    // Show sample users (first 3)
    $sql = "SELECT email, status, created_at FROM registration LIMIT 3";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "\n\nSample users:\n";
    foreach($users as $user) {
        echo "Email: " . $user['email'] . ", Status: " . $user['status'] . ", Created: " . $user['created_at'] . "\n";
    }
    
} catch(PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
}
?>
