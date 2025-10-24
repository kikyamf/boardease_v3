<?php
// Test create_group_chat.php with clean output
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Test Create Group Chat - Clean Output</h2>";

// Test data (same as Android app sends)
$json_data = json_encode([
    'group_name' => 'Test Group 1',
    'created_by' => 29,
    'member_ids' => [28, 1]
]);

echo "<p>Testing with JSON data:</p>";
echo "<pre>" . htmlspecialchars($json_data) . "</pre>";

// Test the actual create_group_chat.php using cURL
echo "<h3>Testing create_group_chat.php:</h3>";

$url = 'http://192.168.101.6/BoardEase2/create_group_chat.php';

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $json_data);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Content-Length: ' . strlen($json_data)
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

echo "<p>HTTP Code: " . $http_code . "</p>";
if ($error) {
    echo "<p>cURL Error: " . $error . "</p>";
}

echo "<p>Raw Response:</p>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

// Try to parse the response as JSON
echo "<h3>JSON Parsing Test:</h3>";
$json_response = json_decode($response, true);
if ($json_response === null) {
    echo "<p>❌ Failed to parse JSON response</p>";
    echo "<p>JSON Error: " . json_last_error_msg() . "</p>";
} else {
    echo "<p>✅ Successfully parsed JSON response</p>";
    echo "<pre>" . print_r($json_response, true) . "</pre>";
}
?>




