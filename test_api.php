<?php
// Test the get_unread_notif_count.php API
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    // Test the same query that the API uses
    $stmt = $conn->prepare("
        SELECT 
            COUNT(*) as total_unread,
            SUM(CASE WHEN notif_type = 'booking' THEN 1 ELSE 0 END) as booking_count,
            SUM(CASE WHEN notif_type = 'payment' THEN 1 ELSE 0 END) as payment_count,
            SUM(CASE WHEN notif_type = 'maintenance' THEN 1 ELSE 0 END) as maintenance_count,
            SUM(CASE WHEN notif_type = 'announcement' THEN 1 ELSE 0 END) as announcement_count,
            SUM(CASE WHEN notif_type = 'general' THEN 1 ELSE 0 END) as general_count
        FROM notifications 
        WHERE user_id = ? AND notif_status = 'unread'
    ");
    
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $data = $result->fetch_assoc();
    
    $response = [
        'success' => true,
        'data' => [
            'total_unread' => (int)$data['total_unread'],
            'by_type' => [
                'booking' => (int)$data['booking_count'],
                'payment' => (int)$data['payment_count'],
                'maintenance' => (int)$data['maintenance_count'],
                'announcement' => (int)$data['announcement_count'],
                'general' => (int)$data['general_count']
            ]
        ]
    ];
    
    echo "API Test Result:\n";
    echo json_encode($response, JSON_PRETTY_PRINT) . "\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















