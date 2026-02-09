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

    $owner_id = isset($_GET['owner_id']) ? intval($_GET['owner_id']) : 0;

    if ($owner_id == 0) {
        echo json_encode(['success' => false, 'message' => 'Owner ID is required']);
        exit;
    }

    $sql = "SELECT crr.*, r.f_name, r.l_name, bh.bh_name, bhr.room_name as old_room_name, bhr_new.room_name as new_room_name, bhr_new.room_category as new_room_category
            FROM change_room_requests crr
            JOIN bookings b ON crr.booking_id = b.booking_id
            JOIN registrations r ON crr.user_id = r.id
            JOIN boarding_house_rooms bhr ON b.room_id = bhr.bhr_id
            JOIN boarding_house_rooms bhr_new ON crr.new_room_id = bhr_new.bhr_id
            JOIN boarding_houses bh ON bhr_new.bh_id = bh.bh_id
            WHERE bh.user_id = ?
            ORDER BY crr.created_at DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$owner_id]);
    $requests = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode(['success' => true, 'data' => ['change_room_requests' => $requests]]);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
