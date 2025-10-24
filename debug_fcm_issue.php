<?php
// Debug FCM notification issues
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';
require_once 'fcm_config.php';

echo "<h2>FCM Debug Report</h2>";

// 1. Check Firebase service account file
echo "<h3>1. Firebase Service Account File</h3>";
$serviceAccountPath = 'fcm_service_account_key.json';
if (file_exists($serviceAccountPath)) {
    echo "✅ Service account file exists: $serviceAccountPath<br>";
    $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
    if ($serviceAccount) {
        echo "✅ Service account JSON is valid<br>";
        echo "Project ID: " . ($serviceAccount['project_id'] ?? 'NOT FOUND') . "<br>";
        echo "Client Email: " . ($serviceAccount['client_email'] ?? 'NOT FOUND') . "<br>";
    } else {
        echo "❌ Service account JSON is invalid<br>";
    }
} else {
    echo "❌ Service account file NOT found: $serviceAccountPath<br>";
}

// 2. Check device tokens in database
echo "<h3>2. Device Tokens in Database</h3>";
$db = getDB();
$stmt = $db->prepare("
    SELECT u.user_id, r.f_name, r.l_name, dt.device_token, dt.is_active, dt.created_at
    FROM users u 
    JOIN registration r ON u.reg_id = r.reg_id
    LEFT JOIN device_tokens dt ON u.user_id = dt.user_id
    ORDER BY u.user_id
");
$stmt->execute();
$users = $stmt->fetchAll();

echo "<table border='1' style='border-collapse: collapse;'>";
echo "<tr><th>User ID</th><th>Name</th><th>Device Token</th><th>Active</th><th>Created</th></tr>";

foreach ($users as $user) {
    $tokenStatus = $user['device_token'] ? 
        (strlen($user['device_token']) > 50 ? "✅ Valid" : "❌ Too short") : 
        "❌ No token";
    $activeStatus = $user['is_active'] ? "✅ Active" : "❌ Inactive";
    
    echo "<tr>";
    echo "<td>" . $user['user_id'] . "</td>";
    echo "<td>" . $user['f_name'] . " " . $user['l_name'] . "</td>";
    echo "<td>" . ($user['device_token'] ? substr($user['device_token'], 0, 50) . "..." : "None") . "</td>";
    echo "<td>$activeStatus</td>";
    echo "<td>" . ($user['created_at'] ?? 'N/A') . "</td>";
    echo "</tr>";
}
echo "</table>";

// 3. Test FCM access token generation
echo "<h3>3. FCM Access Token Test</h3>";
try {
    $reflection = new ReflectionClass('FCMConfig');
    $method = $reflection->getMethod('getAccessToken');
    $method->setAccessible(true);
    $accessToken = $method->invoke(null);
    
    if ($accessToken) {
        echo "✅ Access token generated successfully<br>";
        echo "Token length: " . strlen($accessToken) . " characters<br>";
        echo "Token preview: " . substr($accessToken, 0, 50) . "...<br>";
    } else {
        echo "❌ Failed to generate access token<br>";
    }
} catch (Exception $e) {
    echo "❌ Error generating access token: " . $e->getMessage() . "<br>";
}

// 4. Test FCM notification send
echo "<h3>4. FCM Notification Test</h3>";
$testToken = null;
foreach ($users as $user) {
    if ($user['device_token'] && $user['is_active']) {
        $testToken = $user['device_token'];
        break;
    }
}

if ($testToken) {
    echo "Testing with token: " . substr($testToken, 0, 50) . "...<br>";
    try {
        $result = FCMConfig::sendToDevice(
            $testToken,
            "Test Notification",
            "This is a test notification from debug script",
            ['type' => 'test', 'timestamp' => date('Y-m-d H:i:s')]
        );
        
        echo "FCM Response:<br>";
        echo "<pre>" . json_encode($result, JSON_PRETTY_PRINT) . "</pre>";
        
        if ($result['success']) {
            echo "✅ Test notification sent successfully!<br>";
        } else {
            echo "❌ Test notification failed<br>";
            echo "HTTP Code: " . $result['http_code'] . "<br>";
            if (isset($result['response']['error'])) {
                echo "Error: " . $result['response']['error']['message'] . "<br>";
            }
        }
    } catch (Exception $e) {
        echo "❌ Exception during test: " . $e->getMessage() . "<br>";
    }
} else {
    echo "❌ No valid device tokens found for testing<br>";
}

// 5. Check recent message logs
echo "<h3>5. Recent Message Logs</h3>";
if (file_exists('messages.log')) {
    $logs = file('messages.log', FILE_IGNORE_NEW_LINES);
    $recentLogs = array_slice($logs, -5); // Last 5 messages
    
    echo "<table border='1' style='border-collapse: collapse;'>";
    echo "<tr><th>Message ID</th><th>Sender</th><th>Receiver</th><th>Notification Sent</th><th>Timestamp</th></tr>";
    
    foreach ($recentLogs as $log) {
        $data = json_decode($log, true);
        if ($data) {
            echo "<tr>";
            echo "<td>" . $data['message_id'] . "</td>";
            echo "<td>" . $data['sender_id'] . "</td>";
            echo "<td>" . $data['receiver_id'] . "</td>";
            echo "<td>" . ($data['notification_sent'] ? "✅ Yes" : "❌ No") . "</td>";
            echo "<td>" . $data['timestamp'] . "</td>";
            echo "</tr>";
        }
    }
    echo "</table>";
} else {
    echo "❌ No message log file found<br>";
}

echo "<h3>Summary</h3>";
echo "If notifications are failing, check:<br>";
echo "1. Firebase service account file exists and is valid<br>";
echo "2. Users have device tokens registered<br>";
echo "3. Device tokens are active in database<br>";
echo "4. FCM access token can be generated<br>";
echo "5. Test notification can be sent<br>";
?>








