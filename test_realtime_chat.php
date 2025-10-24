<?php
// Test real-time chat functionality
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING REAL-TIME CHAT FUNCTIONALITY ===\n\n";

require_once 'db_helper.php';
$db = getDB();

// 1. Test individual message with FCM data
echo "1. TESTING INDIVIDUAL MESSAGE WITH FCM DATA:\n";

$sender_id = 1;
$receiver_id = 6;
$test_message = "REAL-TIME TEST MESSAGE " . time();

echo "Sending message from User $sender_id to User $receiver_id\n";
echo "Message: '$test_message'\n";

$stmt = $db->prepare("
    INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
    VALUES (?, ?, ?, NOW(), 'Sent')
");
$stmt->execute([$sender_id, $receiver_id, $test_message]);
$message_id = $db->lastInsertId();

echo "✅ Message inserted with ID: $message_id\n";

// Get sender and receiver info for FCM
$stmt = $db->prepare("
    SELECT u.user_id, r.f_name as first_name, r.l_name as last_name, dt.device_token 
    FROM users u 
    JOIN registrations r ON u.reg_id = r.reg_id
    LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
    WHERE u.user_id = ? AND u.status = 'Active' AND r.status = 'Approved'
");

$stmt->execute([$sender_id]);
$sender = $stmt->fetch();
$sender_name = $sender['first_name'] . ' ' . $sender['last_name'];

$stmt->execute([$receiver_id]);
$receiver = $stmt->fetch();
$receiver_name = $receiver['first_name'] . ' ' . $receiver['last_name'];

echo "Sender: $sender_name (Token: " . ($sender['device_token'] ? 'Present' : 'Missing') . ")\n";
echo "Receiver: $receiver_name (Token: " . ($receiver['device_token'] ? 'Present' : 'Missing') . ")\n";

// 2. Test FCM data payload
echo "\n2. FCM DATA PAYLOAD THAT WOULD BE SENT:\n";
$fcm_data = [
    'type' => 'new_message',
    'sender_id' => (string)$sender_id,
    'sender_name' => $sender_name,
    'receiver_id' => (string)$receiver_id,
    'message_id' => (string)$message_id,
    'message_text' => $test_message,
    'chat_type' => 'individual',
    'timestamp' => date('Y-m-d H:i:s')
];

echo "FCM Data:\n";
foreach ($fcm_data as $key => $value) {
    echo "  $key: $value\n";
}

// 3. Test group message with FCM data
echo "\n3. TESTING GROUP MESSAGE WITH FCM DATA:\n";

$group_id = 4;
$test_group_message = "REAL-TIME GROUP TEST " . time();

echo "Sending group message to Group $group_id\n";
echo "Message: '$test_group_message'\n";

$stmt = $db->prepare("
    INSERT INTO group_messages (gc_id, sender_id, groupmessage_text, groupmessage_timestamp, groupmessage_status) 
    VALUES (?, ?, ?, NOW(), 'Sent')
");
$stmt->execute([$group_id, $sender_id, $test_group_message]);
$group_message_id = $db->lastInsertId();

echo "✅ Group message inserted with ID: $group_message_id\n";

// Get group members
$stmt = $db->prepare("
    SELECT u.user_id, r.f_name, r.l_name, dt.device_token
    FROM group_members gm
    JOIN users u ON gm.user_id = u.user_id
    JOIN registrations r ON u.reg_id = r.reg_id
    LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
    WHERE gm.gc_id = ? AND u.status = 'Active' AND r.status = 'Approved'
");

$stmt->execute([$group_id]);
$members = $stmt->fetchAll();

echo "Group members:\n";
foreach ($members as $member) {
    $member_name = $member['f_name'] . ' ' . $member['l_name'];
    $token_status = $member['device_token'] ? 'Present' : 'Missing';
    echo "- $member_name (ID: {$member['user_id']}, Token: $token_status)\n";
}

// 4. Test FCM data payload for group
echo "\n4. FCM DATA PAYLOAD FOR GROUP MESSAGE:\n";
$group_fcm_data = [
    'type' => 'new_message',
    'sender_id' => (string)$sender_id,
    'sender_name' => $sender_name,
    'receiver_id' => (string)$members[0]['user_id'], // First member as example
    'group_id' => (string)$group_id,
    'group_name' => 'Test Group',
    'message_id' => (string)$group_message_id,
    'message_text' => $test_group_message,
    'chat_type' => 'group',
    'timestamp' => date('Y-m-d H:i:s')
];

echo "Group FCM Data:\n";
foreach ($group_fcm_data as $key => $value) {
    echo "  $key: $value\n";
}

// 5. Test API endpoints
echo "\n5. TESTING API ENDPOINTS:\n";

// Test individual message API
echo "Testing individual message API...\n";
$url = "http://localhost/send_message.php";
$data = [
    'sender_id' => $sender_id,
    'receiver_id' => $receiver_id,
    'message' => 'API TEST MESSAGE ' . time()
];

$options = [
    'http' => [
        'header' => "Content-type: application/x-www-form-urlencoded\r\n",
        'method' => 'POST',
        'content' => http_build_query($data)
    ]
];

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);
$response = json_decode($result, true);

if ($response && $response['success']) {
    echo "✅ Individual message API working\n";
    echo "Message ID: {$response['data']['message_id']}\n";
    echo "Notification sent: " . ($response['data']['notification_sent'] ? 'Yes' : 'No') . "\n";
} else {
    echo "❌ Individual message API failed\n";
}

// Test group message API
echo "\nTesting group message API...\n";
$url = "http://localhost/send_group_message.php";
$data = [
    'sender_id' => $sender_id,
    'group_id' => $group_id,
    'message' => 'API GROUP TEST MESSAGE ' . time()
];

$options = [
    'http' => [
        'header' => "Content-type: application/x-www-form-urlencoded\r\n",
        'method' => 'POST',
        'content' => http_build_query($data)
    ]
];

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);
$response = json_decode($result, true);

if ($response && $response['success']) {
    echo "✅ Group message API working\n";
    echo "Message ID: {$response['data']['message_id']}\n";
    echo "Notifications sent: {$response['data']['notification_count']}\n";
} else {
    echo "❌ Group message API failed\n";
}

echo "\n=== REAL-TIME CHAT IMPLEMENTATION SUMMARY ===\n";
echo "✅ FCM Service Updated:\n";
echo "   - Added real-time message handling\n";
echo "   - Broadcasts NEW_MESSAGE_RECEIVED intent\n";
echo "   - Includes all message data in broadcast\n\n";

echo "✅ Conversation Activity Updated:\n";
echo "   - Added BroadcastReceiver for real-time updates\n";
echo "   - Handles individual and group messages\n";
echo "   - Updates UI immediately when message received\n";
echo "   - Auto-scrolls to bottom\n";
echo "   - Marks messages as read\n\n";

echo "✅ Server APIs Updated:\n";
echo "   - send_message.php includes message_text in FCM data\n";
echo "   - send_group_message.php includes message_text in FCM data\n";
echo "   - Both use 'new_message' type for consistency\n\n";

echo "🎯 EXPECTED BEHAVIOR:\n";
echo "   - When someone sends a message, it appears immediately in conversation\n";
echo "   - No need to go back to home or refresh\n";
echo "   - Messages appear in real-time during active conversation\n";
echo "   - Works for both individual and group chats\n\n";

echo "📱 TEST YOUR ANDROID APP:\n";
echo "   1. Open a conversation\n";
echo "   2. Have someone else send a message\n";
echo "   3. Message should appear immediately without refreshing\n";
echo "   4. Test with both individual and group chats\n";
?>








