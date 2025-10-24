<?php
// Test member loading API directly
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing Member Loading API Directly...\n\n";

// Test the endpoint that CreateGroupChat is calling
$url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=1";
echo "URL: " . $url . "\n";

// Use curl for better error handling
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

echo "HTTP Code: " . $httpCode . "\n";
if ($error) {
    echo "CURL Error: " . $error . "\n";
}

if ($response === false) {
    echo "❌ Failed to get response from server\n";
} else {
    echo "✅ Got response:\n";
    echo $response . "\n\n";
    
    $data = json_decode($response, true);
    if ($data) {
        echo "✅ JSON parsed successfully\n";
        if (isset($data['success'])) {
            echo "Success: " . ($data['success'] ? 'true' : 'false') . "\n";
            if (isset($data['message'])) {
                echo "Message: " . $data['message'] . "\n";
            }
            if (isset($data['data']['users'])) {
                echo "Users found: " . count($data['data']['users']) . "\n";
                foreach ($data['data']['users'] as $user) {
                    echo "- " . $user['full_name'] . " (" . $user['user_type'] . ") - " . $user['boarding_house_name'] . "\n";
                }
            }
        }
    } else {
        echo "❌ Failed to parse JSON\n";
    }
}
?>




