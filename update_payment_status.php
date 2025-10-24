<?php
// Update Payment Status API - Updates payment status for bills
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
    $bill_id = isset($input['payment_id']) ? intval($input['payment_id']) : 0;
    $new_status = isset($input['status']) ? $input['status'] : '';
    $notes = isset($input['notes']) ? $input['notes'] : '';
    
    if ($bill_id <= 0 || empty($new_status)) {
        throw new Exception("Invalid payment_id or status");
    }
    
    // Validate status
    $valid_statuses = ['Unpaid', 'Paid', 'Overdue'];
    if (!in_array(ucfirst($new_status), $valid_statuses)) {
        throw new Exception("Invalid status. Must be: Unpaid, Paid, or Overdue");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, get bill details for notification
        $stmt = $db->prepare("
            SELECT 
                b.*,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ab.user_id as boarder_user_id,
                bh.bh_name
            FROM bills b
            INNER JOIN active_boarders ab ON b.active_id = ab.active_id
            INNER JOIN users u ON ab.user_id = u.user_id
            INNER JOIN registrations r ON u.reg_id = r.reg_id
            LEFT JOIN boarding_houses bh ON ab.user_id = bh.user_id
            WHERE b.bill_id = ?
        ");
        $stmt->execute([$bill_id]);
        $bill = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$bill) {
            throw new Exception("Bill not found");
        }
        
        // Update the bill status
        $stmt = $db->prepare("UPDATE bills SET status = ? WHERE bill_id = ?");
        $stmt->execute([ucfirst($new_status), $bill_id]);
        
        if ($stmt->rowCount() === 0) {
            throw new Exception('No changes made to bill status');
        }
        
        // Create notification for the boarder
        $notificationTitle = "Payment Status Updated";
        $notificationMessage = "Your payment of P" . number_format($bill['amount_due'], 2) . 
                              " has been marked as " . ucfirst($new_status) . 
                              ($notes ? ". Note: " . $notes : "");
        
        $stmt = $db->prepare("
            INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status)
            VALUES (?, ?, ?, 'payment', 'unread')
        ");
        $stmt->execute([
            $bill['boarder_user_id'],
            $notificationTitle,
            $notificationMessage
        ]);
        
        // Commit transaction
        $db->commit();
        
        $response = [
            'success' => true,
            'message' => 'Payment status updated successfully',
            'data' => [
                'bill_id' => $bill_id,
                'new_status' => ucfirst($new_status),
                'amount' => $bill['amount_due'],
                'boarder_name' => $bill['boarder_name'],
                'notification_sent' => true
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