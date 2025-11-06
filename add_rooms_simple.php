<?php
// Simple script to add room data
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get first 3 boarding houses
    $sql = "SELECT bh_id, bh_name FROM boarding_houses WHERE status = 'active' LIMIT 3";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $boardingHouses = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $added = 0;
    foreach ($boardingHouses as $bh) {
        // Add rooms for this boarding house
        $rooms = [
            ['Single Room', 2500, 'Private Room', 1, 'Cozy single room with basic amenities'],
            ['Double Room', 3500, 'Private Room', 2, 'Spacious double room perfect for two people'],
            ['Bed Spacer', 1800, 'Bed Spacer', 1, 'Shared room with individual bed space']
        ];
        
        foreach ($rooms as $room) {
            $insertSql = "INSERT INTO boarding_house_rooms (bh_id, room_name, price, room_category, capacity, room_description, total_rooms) VALUES (?, ?, ?, ?, ?, ?, ?)";
            $insertStmt = $pdo->prepare($insertSql);
            $insertStmt->execute([$bh['bh_id'], $room[0], $room[1], $room[2], $room[3], $room[4], 1]);
            $added++;
        }
    }
    
    echo "Successfully added $added rooms to the database!";
    
} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
?>
