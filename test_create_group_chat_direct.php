<?php
// Test create_group_chat.php directly
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

try {
    // Test basic functionality first
    $response = [
        'success' => true,
        'message' => 'Direct test successful',
        'data' => [
            'timestamp' => date('Y-m-d H:i:s'),
            'test' => 'working'
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




