<?php
// test_login.php - Simple test to verify the login endpoint

echo "Testing login endpoint...\n";

// Test data - use actual credentials from your database
$testData = array(
    'email' => 'test@example.com',  // Replace with actual email from your database
    'password' => 'testpassword'    // Replace with actual password from your database
);

// Initialize cURL
$ch = curl_init();

// Set cURL options
curl_setopt($ch, CURLOPT_URL, 'http://192.168.1.3/boardease2/login.php');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $testData);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

// Execute the request
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);

curl_close($ch);

// Display results
echo "HTTP Code: " . $httpCode . "\n";
echo "Response: " . $response . "\n";

if ($error) {
    echo "cURL Error: " . $error . "\n";
}

echo "Test completed.\n";
?>
