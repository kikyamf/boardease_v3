<?php
// Test messaging endpoints
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h1>Testing Messaging Endpoints</h1>";

// Test send_message.php
echo "<h2>Testing send_message.php</h2>";
$url = "http://192.168.101.6/BoardEase2/send_message.php";
$data = [
    'sender_id' => 1,
    'receiver_id' => 2,
    'message' => 'Test message from PHP'
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

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

// Test send_group_message.php
echo "<h2>Testing send_group_message.php</h2>";
$url = "http://192.168.101.6/BoardEase2/send_group_message.php";
$data = [
    'sender_id' => 1,
    'group_id' => 1,
    'message' => 'Test group message from PHP'
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

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

// Test create_group_chat.php
echo "<h2>Testing create_group_chat.php</h2>";
$url = "http://192.168.101.6/BoardEase2/create_group_chat.php";
$data = [
    'group_name' => 'Test Group',
    'created_by' => 1,
    'member_ids' => json_encode([2, 3])
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

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

// Test get_messages.php
echo "<h2>Testing get_messages.php</h2>";
$url = "http://192.168.101.6/BoardEase2/get_messages.php?user1_id=1&user2_id=2";
$result = file_get_contents($url);

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

// Test get_group_messages.php
echo "<h2>Testing get_group_messages.php</h2>";
$url = "http://192.168.101.6/BoardEase2/get_group_messages.php?group_id=1";
$result = file_get_contents($url);

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

// Test get_group_members.php
echo "<h2>Testing get_group_members.php</h2>";
$url = "http://192.168.101.6/BoardEase2/get_group_members.php?group_id=1";
$result = file_get_contents($url);

echo "<h3>Response:</h3>";
echo "<pre>" . htmlspecialchars($result) . "</pre>";

echo "<h2>Test Complete!</h2>";
?>




















