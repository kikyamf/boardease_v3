<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check if there are any rooms
    $sql = "SELECT COUNT(*) as room_count FROM boarding_house_rooms";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $roomCount = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Get sample rooms with prices
    $sql2 = "SELECT bhr.bh_id, bhr.room_name, bhr.price, bh.bh_name 
             FROM boarding_house_rooms bhr 
             JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id 
             LIMIT 10";
    $stmt2 = $pdo->prepare($sql2);
    $stmt2->execute();
    $rooms = $stmt2->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode(array(
        'success' => true,
        'room_count' => $roomCount['room_count'],
        'sample_rooms' => $rooms
    ));
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
}
?>
