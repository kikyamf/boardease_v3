<?php
// Delete notification
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $notif_id = $_POST['notif_id'] ?? null;
    $user_id = $_POST['user_id'] ?? null;
    
    if (!$notif_id || !$user_id) {
        throw new Exception('Missing required parameters: notif_id, user_id');
    }
    
    $db = getDB();
    
    // Delete notification
    $stmt = $db->prepare("
        DELETE FROM notifications 
        WHERE notif_id = ? AND user_id = ?
    ");
    $stmt->execute([$notif_id, $user_id]);
    
    if ($stmt->rowCount() > 0) {
        $response = [
            'success' => true,
            'message' => 'Notification deleted successfully',
            'data' => [
                'notif_id' => $notif_id,
                'user_id' => $user_id
            ]
        ];
    } else {
        throw new Exception('Notification not found');
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

ob_clean();
echo json_encode($response);
exit;
?>
