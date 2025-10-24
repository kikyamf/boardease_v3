<?php
// Simple test of the API logic
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    $db = $conn;
    
    // Get total unread count
    $total_stmt = $db->prepare("
        SELECT COUNT(*) as total_unread 
        FROM notifications 
        WHERE user_id = ? AND notif_status = 'unread'
    ");
    $total_stmt->execute([$user_id]);
    $total_unread = $total_stmt->fetch()['total_unread'];
    
    $response = [
        'success' => true,
        'data' => [
            'total_unread' => (int)$total_unread
        ]
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
    echo json_encode($response, JSON_PRETTY_PRINT);
}

$conn->close();
?>








