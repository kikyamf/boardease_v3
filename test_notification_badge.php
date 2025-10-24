<?php
// Test notification badge functionality
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'notification_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 1; // Default to user ID 1 for testing
    
    // Create a test notification
    $result = NotificationHelper::createNotification(
        $user_id,
        "🔔 Badge Test Notification",
        "This notification should show a badge count on your app! Check the notification icon.",
        "general",
        true
    );
    
    $response = [
        'success' => true,
        'message' => 'Test notification created for badge testing',
        'data' => $result,
        'instructions' => [
            '1. Check your Android app',
            '2. Look at the notification icon in the home screen',
            '3. You should see a red badge with a number',
            '4. Tap the notification icon to open notifications',
            '5. The badge should disappear after opening'
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





















