<?php
// Test the chat list API to see what unread counts are being returned
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING CHAT LIST UNREAD COUNT ===\n\n";

// Test with user 29 (owner)
$user_id = 29;
echo "Testing with user_id: $user_id\n\n";

$url = "http://192.168.101.6/BoardEase2/message/get_chat_list.php?user_id=$user_id";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "HTTP Code: $http_code\n";
echo "Response:\n";
$data = json_decode($response, true);
if ($data) {
    echo "Success: " . ($data['success'] ? 'true' : 'false') . "\n";
    if (isset($data['data']['chats'])) {
        echo "Total chats: " . count($data['data']['chats']) . "\n\n";
        
        foreach ($data['data']['chats'] as $chat) {
            echo "Chat: " . $chat['chat_name'] . "\n";
            echo "  Type: " . $chat['chat_type'] . "\n";
            echo "  Last message: " . $chat['last_message'] . "\n";
            echo "  Unread count: " . $chat['unread_count'] . "\n";
            echo "  Last message time: " . $chat['last_message_time'] . "\n";
            echo "  Last sender: " . $chat['last_sender_name'] . "\n";
            echo "---\n";
        }
    } else {
        echo "No chats data found\n";
    }
} else {
    echo "Failed to parse JSON response\n";
    echo "Raw response: $response\n";
}

echo "\n=== TESTING UNREAD COUNT API ===\n";

// Also test the unread count API
$unread_url = "http://192.168.101.6/BoardEase2/get_unread_count.php?user_id=$user_id";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $unread_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

$unread_response = curl_exec($ch);
$unread_http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "Unread Count API HTTP Code: $unread_http_code\n";
echo "Unread Count API Response:\n";
$unread_data = json_decode($unread_response, true);
if ($unread_data) {
    echo "Success: " . ($unread_data['success'] ? 'true' : 'false') . "\n";
    if (isset($unread_data['data'])) {
        echo "Individual unread: " . $unread_data['data']['individual_unread'] . "\n";
        echo "Group unread: " . $unread_data['data']['group_unread'] . "\n";
        echo "Total unread: " . $unread_data['data']['total_unread'] . "\n";
    }
} else {
    echo "Failed to parse unread count JSON\n";
    echo "Raw response: $unread_response\n";
}

echo "\n=== TEST COMPLETE ===\n";
?>




