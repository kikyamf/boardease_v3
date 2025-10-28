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
    
    // Get the first few boarding houses
    $sql = "SELECT bh_id, bh_name FROM boarding_houses WHERE status = 'active' LIMIT 5";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $boardingHouses = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $addedRooms = [];
    
    foreach ($boardingHouses as $bh) {
        // Add 2-3 sample rooms for each boarding house
        $rooms = [
            ['Single Room', 2000, 'Private Room'],
            ['Double Room', 2500, 'Private Room'],
            ['Bed Spacer', 1500, 'Bed Spacer']
        ];
        
        foreach ($rooms as $room) {
            $insertSql = "INSERT INTO boarding_house_rooms (bh_id, room_name, price, room_category, capacity, room_description, total_rooms) VALUES (?, ?, ?, ?, ?, ?, ?)";
            $insertStmt = $pdo->prepare($insertSql);
            $capacity = $room[1] == 1500 ? 1 : 2; // Bed spacer = 1, others = 2
            $description = "Comfortable " . strtolower($room[0]) . " with basic amenities";
            $totalRooms = 1;
            $insertStmt->execute([$bh['bh_id'], $room[0], $room[1], $room[2], $capacity, $description, $totalRooms]);
            $addedRooms[] = $bh['bh_name'] . ' - ' . $room[0] . ' (₱' . $room[1] . ')';
        }
    }
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Sample rooms added successfully',
        'added_rooms' => $addedRooms
    ));
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
}
?>
