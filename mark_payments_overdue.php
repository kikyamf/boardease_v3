<?php
// Mark Payments Overdue API - Automatically marks overdue payments
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';

$response = [];

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Get parameters
    $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
    
    if ($owner_id <= 0) {
        throw new Exception("Invalid owner_id");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // Find bills that are past due date and still unpaid
        $overdueQuery = "
            UPDATE bills b
            INNER JOIN active_boarders ab ON b.active_id = ab.active_id
            INNER JOIN users u ON ab.user_id = u.user_id
            LEFT JOIN boarding_houses bh ON u.user_id = bh.user_id
            SET b.status = 'Overdue'
            WHERE bh.user_id = ? 
            AND b.status = 'Unpaid' 
            AND b.due_date < CURDATE()
        ";
        
        $stmt = $db->prepare($overdueQuery);
        $stmt->execute([$owner_id]);
        $updated_count = $stmt->rowCount();
        
        // Get details of overdue bills for notifications
        $overdueDetailsQuery = "
            SELECT 
                b.bill_id,
                b.amount_due,
                b.due_date,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ab.user_id as boarder_user_id
            FROM bills b
            INNER JOIN active_boarders ab ON b.active_id = ab.active_id
            INNER JOIN users u ON ab.user_id = u.user_id
            INNER JOIN registrations r ON u.reg_id = r.reg_id
            LEFT JOIN boarding_houses bh ON u.user_id = bh.user_id
            WHERE bh.user_id = ? 
            AND b.status = 'Overdue' 
            AND b.due_date < CURDATE()
        ";
        
        $stmt = $db->prepare($overdueDetailsQuery);
        $stmt->execute([$owner_id]);
        $overdue_bills = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Send notifications to boarders about overdue payments
        $notifications_sent = 0;
        foreach ($overdue_bills as $bill) {
            $notificationTitle = "Payment Overdue";
            $notificationMessage = "Your payment of P" . number_format($bill['amount_due'], 2) . 
                                  " was due on " . date('M j, Y', strtotime($bill['due_date'])) . 
                                  " and is now overdue. Please make payment as soon as possible.";
            
            $stmt = $db->prepare("
                INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status)
                VALUES (?, ?, ?, 'payment', 'unread')
            ");
            $stmt->execute([
                $bill['boarder_user_id'],
                $notificationTitle,
                $notificationMessage
            ]);
            $notifications_sent++;
        }
        
        // Commit transaction
        $db->commit();
        
        $response = [
            'success' => true,
            'message' => "Marked $updated_count payments as overdue and sent $notifications_sent notifications",
            'data' => [
                'overdue_count' => $updated_count,
                'notifications_sent' => $notifications_sent,
                'overdue_bills' => $overdue_bills
            ]
        ];
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $db->rollback();
        throw $e;
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>