<?php
// Debug Payment API Response
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Capture any output that might interfere with JSON
ob_start();

// Simulate the exact request that Android makes
$_SERVER['REQUEST_METHOD'] = 'POST';
$_SERVER['CONTENT_TYPE'] = 'application/json';

// Simulate JSON input
$json_input = '{"owner_id": 1, "status": "all"}';
file_put_contents('php://input', $json_input);

echo "=== DEBUGGING PAYMENT API RESPONSE ===\n\n";

// Include the payment API
include 'get_payment_status.php';

$output = ob_get_clean();

echo "📋 Complete Output:\n";
echo "--- START ---\n";
echo $output;
echo "\n--- END ---\n\n";

// Check if it's valid JSON
$json_data = json_decode($output, true);
if (json_last_error() === JSON_ERROR_NONE) {
    echo "✅ Valid JSON Response\n";
    echo "📊 Structure:\n";
    print_r($json_data);
} else {
    echo "❌ Invalid JSON\n";
    echo "JSON Error: " . json_last_error_msg() . "\n";
    echo "Raw output length: " . strlen($output) . " characters\n";
    
    // Show first 200 characters
    echo "First 200 characters:\n";
    echo substr($output, 0, 200) . "...\n";
}
?>














