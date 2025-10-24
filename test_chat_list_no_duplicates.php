<?php
// Test the chat list to ensure no duplicate conversations
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING CHAT LIST NO DUPLICATES ===\n\n";

// Test with user 29 (owner)
$user_id = 29;
echo "Testing with user_id: $user_id (BH Owner)\n\n";

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
        
        echo "Individual chats (should show each person only once):\n";
        $individual_chats = [];
        foreach ($data['data']['chats'] as $chat) {
            if ($chat['chat_type'] === 'individual') {
                $chat_name = $chat['chat_name'];
                if (in_array($chat_name, $individual_chats)) {
                    echo "❌ DUPLICATE FOUND: $chat_name\n";
                } else {
                    echo "✅ $chat_name (ID: " . $chat['other_user_id'] . ")\n";
                    echo "  Type: " . $chat['user_type'] . "\n";
                    echo "  Last message: " . $chat['last_message'] . "\n";
                    echo "  Unread count: " . $chat['unread_count'] . "\n";
                    echo "  Last message time: " . $chat['last_message_time'] . "\n";
                    echo "---\n";
                    $individual_chats[] = $chat_name;
                }
            }
        }
        
        echo "\nGroup chats:\n";
        foreach ($data['data']['chats'] as $chat) {
            if ($chat['chat_type'] === 'group') {
                echo "✅ " . $chat['group_name'] . " (ID: " . $chat['group_id'] . ")\n";
                echo "  Last message: " . $chat['last_message'] . "\n";
                echo "  Unread count: " . $chat['unread_count'] . "\n";
                echo "  Last message time: " . $chat['last_message_time'] . "\n";
                echo "---\n";
            }
        }
        
        echo "\nSummary:\n";
        echo "Individual chats count: " . count($individual_chats) . "\n";
        echo "Total unique individual chats: " . count(array_unique($individual_chats)) . "\n";
        if (count($individual_chats) === count(array_unique($individual_chats))) {
            echo "✅ No duplicates found!\n";
        } else {
            echo "❌ Duplicates found!\n";
        }
    } else {
        echo "No chats data found\n";
    }
} else {
    echo "Failed to parse JSON response\n";
    echo "Raw response: $response\n";
}

echo "\n=== TEST COMPLETE ===\n";
?>




