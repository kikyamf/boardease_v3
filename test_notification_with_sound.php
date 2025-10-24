<?php
// Test notification with sound and FCM push notification
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'notification_helper.php';
require_once 'fcm_config.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 1; // Default to user ID 1 for testing
    
    // Create notification in database
    $result = NotificationHelper::createNotification(
        $user_id,
        "🔔 NOTIFICATION WITH SOUND",
        "This notification includes sound and FCM push notification! Check your device.",
        "general",
        true // Send FCM notification
    );
    
    // Also create additional test notifications
    $test_notifications = [
        [
            "title" => "💰 Payment Alert with Sound",
            "message" => "Payment received! This notification has sound enabled.",
            "type" => "payment"
        ],
        [
            "title" => "📅 Booking Request with Sound", 
            "message" => "New booking request received with sound notification.",
            "type" => "booking"
        ],
        [
            "title" => "🔧 Maintenance Alert with Sound",
            "message" => "Maintenance completed with sound notification.",
            "type" => "maintenance"
        ],
        [
            "title" => "📢 Announcement with Sound",
            "message" => "Important announcement with sound notification.",
            "type" => "announcement"
        ]
    ];
    
    $created_notifications = [];
    foreach ($test_notifications as $notif) {
        $notif_result = NotificationHelper::createNotification(
            $user_id,
            $notif["title"],
            $notif["message"],
            $notif["type"],
            true // Send FCM with sound
        );
        $created_notifications[] = $notif_result;
    }
    
    $response = [
        'success' => true,
        'message' => 'Notifications with sound created successfully!',
        'data' => [
            'main_notification' => $result,
            'additional_notifications' => $created_notifications,
            'total_created' => count($created_notifications) + 1
        ],
        'instructions' => [
            '1. Check your Android device for push notifications with sound',
            '2. Open the app and check the notification badge count',
            '3. Click the notification icon to open notifications',
            '4. All notifications should be marked as read automatically',
            '5. Badge should disappear after opening notifications'
        ],
        'sound_info' => [
            'FCM notifications include sound by default',
            'Sound plays when notification arrives',
            'Check device volume and notification settings',
            'Notifications should appear in notification panel'
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





















