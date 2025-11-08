<?php
// Test script to check boarder_favorites table structure
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check if table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'boarder_favorites'");
    if ($stmt->rowCount() == 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Table boarder_favorites does not exist',
            'suggestion' => 'Create the table with: CREATE TABLE boarder_favorites (fav_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, bh_id INT NOT NULL, FOREIGN KEY (user_id) REFERENCES users(user_id), FOREIGN KEY (bh_id) REFERENCES boarding_houses(bh_id), UNIQUE KEY unique_favorite (user_id, bh_id))'
        ));
        exit();
    }
    
    // Get table structure
    $stmt = $pdo->query("DESCRIBE boarder_favorites");
    $structure = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Get sample data
    $stmt = $pdo->query("SELECT * FROM boarder_favorites LIMIT 5");
    $sample = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check users table relationship
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $userCount = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Check registrations table
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM registrations");
    $regCount = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode(array(
        'success' => true,
        'table_structure' => $structure,
        'sample_data' => $sample,
        'users_count' => $userCount['count'],
        'registrations_count' => $regCount['count'],
        'note' => 'Check if user_id in boarder_favorites references users.user_id or registrations.id'
    ));
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Error: ' . $e->getMessage()
    ));
}
?>

