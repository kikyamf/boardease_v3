<?php
// Test Payment API Direct Response
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING PAYMENT API DIRECT RESPONSE ===\n\n";

// Simulate the exact API call that Android makes
$owner_id = 1;
$status = 'all';

echo "📱 Testing API call that Android makes:\n";
echo "   URL: get_payment_status.php?owner_id=$owner_id&status=$status\n\n";

// Include the payment API
ob_start();
include 'get_payment_status.php';
$response = ob_get_clean();

echo "📋 Raw API Response:\n";
echo "--- START RESPONSE ---\n";
echo $response;
echo "\n--- END RESPONSE ---\n\n";

// Check if response is valid JSON
$json_data = json_decode($response, true);
if (json_last_error() === JSON_ERROR_NONE) {
    echo "✅ Response is valid JSON\n";
    echo "📊 Response structure:\n";
    if (isset($json_data['success'])) {
        echo "   - success: " . ($json_data['success'] ? 'true' : 'false') . "\n";
        if (isset($json_data['data'])) {
            echo "   - data: " . (is_array($json_data['data']) ? 'array' : gettype($json_data['data'])) . "\n";
            if (is_array($json_data['data']) && isset($json_data['data']['payments'])) {
                echo "   - payments count: " . count($json_data['data']['payments']) . "\n";
            }
        }
        if (isset($json_data['error'])) {
            echo "   - error: " . $json_data['error'] . "\n";
        }
    }
} else {
    echo "❌ Response is NOT valid JSON\n";
    echo "   JSON Error: " . json_last_error_msg() . "\n";
    echo "   This is why Android gets 'error parsing response'\n";
}

echo "\n=== TEST COMPLETE ===\n";
?>














