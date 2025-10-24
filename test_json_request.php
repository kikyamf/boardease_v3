<?php
// Test JSON request format
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

try {
    // Get raw input
    $input = file_get_contents('php://input');
    
    // Get POST data
    $post_data = $_POST;
    
    // Get GET data
    $get_data = $_GET;
    
    $response = [
        'success' => true,
        'data' => [
            'raw_input' => $input,
            'raw_input_length' => strlen($input),
            'post_data' => $post_data,
            'get_data' => $get_data,
            'content_type' => $_SERVER['CONTENT_TYPE'] ?? 'not set',
            'request_method' => $_SERVER['REQUEST_METHOD'] ?? 'not set',
            'json_decode_result' => json_decode($input, true),
            'json_last_error' => json_last_error_msg()
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




