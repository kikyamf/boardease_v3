<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $change_request_id = isset($_POST['change_request_id']) ? intval($_POST['change_request_id']) : 0;
    $action = isset($_POST['action']) ? $_POST['action'] : ''; // 'Approve' or 'Decline'

    if ($change_request_id == 0 || empty($action)) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit;
    }

    if ($action === 'Approve') {
        $pdo->beginTransaction();

        // 1. Get request details
        $stmt = $pdo->prepare("SELECT * FROM change_room_requests WHERE change_request_id = ?");
        $stmt->execute([$change_request_id]);
        $request = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$request) {
            $pdo->rollBack();
            echo json_encode(['success' => false, 'message' => 'Request not found']);
            exit;
        }

        // 2. Create a NEW booking for the new room (as Approved/Pending Payment)
        // Note: The original requirement said "room in which he will be changing will be displayed in the pending Applications"
        // and "will be transferred once the payment is approved".
        // So we create a new booking with status 'Pending' (or a specific 'Approved_Change' if needed, but standard 'Pending' usually triggers payment flow).
        // Let's use 'Pending' to trigger the same payment flow as per client's "same process here, like if it's your first time booking".
        
        $insertBooking = $pdo->prepare("
            INSERT INTO bookings (room_id, user_id, start_date, end_date, booking_status) 
            SELECT ?, user_id, start_date, end_date, 'Pending' 
            FROM bookings WHERE booking_id = ?
        ");
        $insertBooking->execute([$request['new_room_id'], $request['booking_id']]);
        $new_booking_id = $pdo->lastInsertId();

        // 3. Update request status
        $updateRequest = $pdo->prepare("UPDATE change_room_requests SET status = 'Approved' WHERE change_request_id = ?");
        $updateRequest->execute([$change_request_id]);

        // 4. Send Notification (simplified, usually inserts into notifications table)
        $notif = $pdo->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type) VALUES (?, 'Change Room Approved', 'Your change room request has been approved. Please proceed to payment for the new room.', 'booking')");
        $notif->execute([$request['user_id']]);

        $pdo->commit();
        echo json_encode(['success' => true, 'message' => 'Request approved. New booking created for payment.']);

    } else if ($action === 'Decline') {
        $stmt = $pdo->prepare("UPDATE change_room_requests SET status = 'Declined' WHERE change_request_id = ?");
        $stmt->execute([$change_request_id]);

        // Send notification
        $stmt2 = $pdo->prepare("SELECT user_id FROM change_room_requests WHERE change_request_id = ?");
        $stmt2->execute([$change_request_id]);
        $uid = $stmt2->fetch(PDO::FETCH_ASSOC)['user_id'];
        
        $notif = $pdo->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type) VALUES (?, 'Change Room Declined', 'Your change room request has been declined.', 'booking')");
        $notif->execute([$uid]);

        echo json_encode(['success' => true, 'message' => 'Request declined.']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Invalid action']);
    }

} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) $pdo->rollBack();
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
