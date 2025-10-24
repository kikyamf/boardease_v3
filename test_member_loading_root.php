<?php
// Test member loading endpoint at root level
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing Root-Level Member Loading Endpoint...\n\n";

// Test the root-level endpoint
$url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=1";
echo "URL: " . $url . "\n";

$context = stream_context_create([
    'http' => [
        'timeout' => 10
    ]
]);

$response = file_get_contents($url, false, $context);

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
                    echo "- " . $user['full_name'] . " (" . $user['user_type'] . ")\n";
                }
            }
        }
    } else {
        echo "❌ Failed to parse JSON\n";
    }
}
?>




