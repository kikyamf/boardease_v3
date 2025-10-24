<?php
// Test Android request simulation using cURL
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Android Request Simulation</h2>";

// Test data (same as Android app sends)
$json_data = json_encode([
    'group_name' => 'Group 1',
    'created_by' => 29,
    'member_ids' => [28, 1]
]);

echo "<p>Simulating Android request with JSON:</p>";
echo "<pre>" . htmlspecialchars($json_data) . "</pre>";

// Test the actual create_group_chat.php using cURL
echo "<h3>Testing create_group_chat.php with cURL:</h3>";

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
echo "<p>Response:</p>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

// Also test the debug version
echo "<h3>Testing debug_create_group_chat_web.php:</h3>";

$debug_url = 'http://192.168.101.6/BoardEase2/debug_create_group_chat_web.php';

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $debug_url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $json_data);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Content-Length: ' . strlen($json_data)
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

$debug_response = curl_exec($ch);
$debug_http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$debug_error = curl_error($ch);
curl_close($ch);

echo "<p>Debug HTTP Code: " . $debug_http_code . "</p>";
if ($debug_error) {
    echo "<p>Debug cURL Error: " . $debug_error . "</p>";
}
echo "<p>Debug Response:</p>";
echo "<pre>" . htmlspecialchars($debug_response) . "</pre>";
?>




