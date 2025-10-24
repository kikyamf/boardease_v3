<?php
// Test the register_device_token.php endpoint
echo "<h3>Testing register_device_token.php</h3>";

// Test data
$testData = [
    'user_id' => '1',
    'device_token' => 'doIZWxHNRkqo_lVUVcNn6a:APA91bGvBwcxisdLz9oNw6CJB1gKSaqz0HmNSLqgOfua9_R_X97IWRIas6HSV0CS4m1LoSMwI2bX959PyMn-vDmxy2K8yIkptrFx8nyzNyaWib5IYH3-0PM',
    'device_type' => 'android',
    'app_version' => '1.0.0'
];

// Make POST request to register_device_token.php
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, 'http://192.168.101.6/BoardEase2/register_device_token.php');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($testData));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/x-www-form-urlencoded'
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "<h4>Response:</h4>";
echo "<p><strong>HTTP Code:</strong> $httpCode</p>";
echo "<p><strong>Response:</strong></p>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

if ($httpCode == 200) {
    $data = json_decode($response, true);
    if ($data && $data['success']) {
        echo "<h4 style='color: green;'>✅ SUCCESS! Device token registered successfully!</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ FAILED: " . ($data['message'] ?? 'Unknown error') . "</h4>";
    }
} else {
    echo "<h4 style='color: red;'>❌ HTTP Error: $httpCode</h4>";
}
?>








