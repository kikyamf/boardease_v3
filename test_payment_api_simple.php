<?php
// Simple test of payment API
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING PAYMENT API ===\n\n";

// Test the payment API directly
$owner_id = 1;
$status = 'all';

echo "Testing with owner_id=$owner_id, status=$status\n\n";

// Simulate the request
$_GET['owner_id'] = $owner_id;
$_GET['status'] = $status;

// Capture output
ob_start();
include 'get_payment_status.php';
$output = ob_get_clean();

echo "Raw output:\n";
echo "--- START ---\n";
echo $output;
echo "\n--- END ---\n\n";

// Check if it's valid JSON
$json = json_decode($output, true);
if ($json === null) {
    echo "❌ NOT VALID JSON\n";
    echo "JSON Error: " . json_last_error_msg() . "\n";
    echo "Output length: " . strlen($output) . " characters\n";
} else {
    echo "✅ VALID JSON\n";
    echo "Response structure:\n";
    print_r($json);
}
?>














