<?php
// Simple debug test
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json');

try {
    // Test basic functionality
    $response = [
        'success' => true,
        'message' => 'Basic test successful',
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




