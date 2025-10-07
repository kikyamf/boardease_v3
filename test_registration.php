<?php
// test_registration.php - Simple test to verify the registration endpoint

echo "Testing registration endpoint...\n";

// Test data
$testData = array(
    'role' => 'Boarder',
    'firstName' => 'Test',
    'middleName' => 'User',
    'lastName' => 'Name',
    'birthDate' => '01/01/1990',
    'phone' => '1234567890',
    'address' => 'Test Address',
    'email' => 'test@example.com',
    'password' => 'testpassword',
    'gcashNum' => '09123456789',
    'idType' => 'Driver\'s License',
    'idNumber' => 'DL123456789',
    'isAgreed' => 'true'
);

// Initialize cURL
$ch = curl_init();

// Set cURL options
curl_setopt($ch, CURLOPT_URL, 'http://192.168.1.3/boardease2/insert_registration.php');
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
