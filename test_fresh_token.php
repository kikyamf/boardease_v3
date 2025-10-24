<?php
// Test FCM with a fresh token
require_once 'fcm_config.php';

// This is a sample fresh token format - you'll need to get a real one from the app
$freshToken = "YOUR_FRESH_TOKEN_HERE"; // Replace with actual fresh token

if ($freshToken === "YOUR_FRESH_TOKEN_HERE") {
    echo "Please replace the token with a fresh one from your app.<br>";
    echo "To get a fresh token:<br>";
    echo "1. Open your Android app<br>";
    echo "2. Check the logs for 'FCM Registration Token: ...'<br>";
    echo "3. Copy that token and replace it in this script<br>";
    exit;
}

echo "<h3>Testing with Fresh Token</h3>";
echo "Token: " . substr($freshToken, 0, 50) . "...<br><br>";

try {
    $result = FCMConfig::sendToDevice(
        $freshToken,
        "Test Notification",
        "This is a test with a fresh token",
        ['type' => 'test', 'timestamp' => date('Y-m-d H:i:s')]
    );
    
    echo "<h4>FCM Response:</h4>";
    echo "<pre>" . json_encode($result, JSON_PRETTY_PRINT) . "</pre>";
    
    if ($result['success']) {
        echo "<h4 style='color: green;'>✅ SUCCESS! Fresh token works!</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ Still failing</h4>";
        if (isset($result['response']['error'])) {
            echo "Error: " . $result['response']['error']['message'] . "<br>";
        }
    }
    
} catch (Exception $e) {
    echo "<h4 style='color: red;'>❌ Exception: " . $e->getMessage() . "</h4>";
}
?>








