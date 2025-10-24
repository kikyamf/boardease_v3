<?php
// Simple notification test
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'notification_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 1;
    
    // Test creating a simple notification
    $result = NotificationHelper::createNotification(
        $user_id,
        "Test Notification",
        "This is a test notification to verify the system is working.",
        "general",
        true
    );
    
    $response = [
        'success' => true,
        'message' => 'Simple notification test completed',
        'data' => [
            'user_id' => $user_id,
            'result' => $result
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
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>







