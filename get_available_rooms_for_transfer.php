<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $booking_id = isset($_GET['booking_id']) ? intval($_GET['booking_id']) : 0;
    $bh_id = isset($_GET['bh_id']) ? intval($_GET['bh_id']) : 0;

    if ($booking_id == 0 || $bh_id == 0) {
        echo json_encode(['success' => false, 'message' => 'Booking ID and BH ID are required']);
        exit;
    }

    // Get the owner of the current BH
    $ownerStmt = $pdo->prepare("SELECT user_id FROM boarding_houses WHERE bh_id = ?");
    $ownerStmt->execute([$bh_id]);
    $owner = $ownerStmt->fetch(PDO::FETCH_ASSOC);
    $owner_id = $owner ? $owner['user_id'] : 0;

    if ($owner_id == 0) {
        echo json_encode(['success' => false, 'message' => 'Owner not found']);
        exit;
    }

    // 1. Available rooms in current BH (excluding current room)
    $stmt1 = $pdo->prepare("
        SELECT bhr.* 
        FROM boarding_house_rooms bhr
        WHERE bhr.bh_id = ? 
        AND bhr.bhr_id NOT IN (SELECT room_id FROM bookings WHERE booking_id = ?)
    ");
    $stmt1->execute([$bh_id, $booking_id]);
    $currentBhRooms = $stmt1->fetchAll(PDO::FETCH_ASSOC);

    // 2. Other BHs of the same owner and their available rooms
    $stmt2 = $pdo->prepare("
        SELECT bh.bh_id, bh.bh_name, bhr.bhr_id, bhr.room_name, bhr.room_category, bhr.price, bhr.capacity
        FROM boarding_houses bh
        JOIN boarding_house_rooms bhr ON bh.bh_id = bhr.bh_id
        WHERE bh.user_id = ? AND bh.bh_id != ?
    ");
    $stmt2->execute([$owner_id, $bh_id]);
    $otherBhRooms = $stmt2->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        'success' => true, 
        'data' => [
            'current_bh_name' => '', // Optionally fetch current bh name
            'current_bh_rooms' => $currentBhRooms,
            'other_bh_rooms' => $otherBhRooms
        ]
    ]);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
