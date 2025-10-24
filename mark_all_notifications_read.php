<?php
// Mark all notifications as read for a user
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    // Handle JSON POST request from Android
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);
    
    $user_id = null;
    if ($data && isset($data['user_id'])) {
        $user_id = $data['user_id'];
    } else {
        // Fallback to regular POST data
        $user_id = $_POST['user_id'] ?? null;
    }
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Mark all notifications as read
    $stmt = $db->prepare("
        UPDATE notifications 
        SET notif_status = 'read' 
        WHERE user_id = ? AND notif_status = 'unread'
    ");
    $stmt->execute([$user_id]);
    $updated_count = $stmt->rowCount();
    
    $response = [
        'success' => true,
        'message' => "Marked $updated_count notifications as read",
        'data' => [
            'user_id' => $user_id,
            'updated_count' => $updated_count
        ]
    ];
    
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
