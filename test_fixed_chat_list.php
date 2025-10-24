<?php
// Test the fixed get_chat_list.php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING FIXED GET_CHAT_LIST.PHP ===\n\n";

// 1. Test the API
echo "1. TESTING API:\n";
$url = "http://localhost/get_chat_list.php?user_id=1";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ API working!\n";
    echo "Found " . count($data['data']['chats']) . " chats:\n\n";
    
    foreach ($data['data']['chats'] as $chat) {
        echo "Chat: {$chat['other_user_name']}\n";
        echo "- Last message: '{$chat['last_message']}'\n";
        echo "- Time: {$chat['last_message_time']}\n";
        echo "- Unread: {$chat['unread_count']}\n\n";
    }
} else {
    echo "❌ API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
}

// 2. Send a new message to test if it appears at the top
echo "2. SENDING NEW MESSAGE TO TEST SORTING:\n";

require_once 'db_helper.php';
$db = getDB();

$stmt = $db->prepare("
    INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
    VALUES (1, 6, 'TEST MESSAGE - This should be at the top!', NOW(), 'Sent')
");

if ($stmt->execute()) {
    $message_id = $db->lastInsertId();
    echo "✅ Sent new message ID: $message_id\n";
    echo "Message: 'TEST MESSAGE - This should be at the top!'\n";
    echo "Time: " . date('Y-m-d H:i:s') . "\n\n";
    
    // 3. Test API again
    echo "3. TESTING API AFTER NEW MESSAGE:\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "Updated chat list:\n";
        foreach ($data['data']['chats'] as $chat) {
            echo "- {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']}\n";
        }
        
        // Check if the new message is at the top
        $first_chat = $data['data']['chats'][0];
        if (strpos($first_chat['last_message'], 'TEST MESSAGE') !== false) {
            echo "\n🎯 SUCCESS! New message appears at the top!\n";
        } else {
            echo "\n❌ New message is not at the top. Sorting issue.\n";
        }
    }
} else {
    echo "❌ Failed to send new message\n";
}

echo "\n=== TEST COMPLETE ===\n";
?>


















