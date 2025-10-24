<?php
// Test group chat creation functionality
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing Group Chat Creation...\n\n";

// Test 1: Create a group chat with valid data
echo "=== Test 1: Creating Group Chat ===\n";

$url = "http://192.168.101.6/BoardEase2/create_group_chat.php";
$data = [
    'group_name' => 'Test Group Chat',
    'created_by' => 1,
    'member_ids' => [2, 3, 4]
];

$options = [
    'http' => [
        'header' => "Content-Type: application/json\r\n",
        'method' => 'POST',
        'content' => json_encode($data)
    ]
];

$context = stream_context_create($options);
$response = file_get_contents($url, false, $context);

if ($response === false) {
    echo "❌ Failed to get response from server\n";
} else {
    echo "✅ Got response:\n";
    echo $response . "\n\n";
    
    $result = json_decode($response, true);
    if ($result && isset($result['success'])) {
        if ($result['success']) {
            echo "✅ Group chat created successfully!\n";
            echo "Group ID: " . $result['data']['group_id'] . "\n";
            echo "Group Name: " . $result['data']['group_name'] . "\n";
            echo "Member Count: " . $result['data']['member_count'] . "\n";
        } else {
            echo "❌ Group creation failed: " . $result['message'] . "\n";
        }
    } else {
        echo "❌ Failed to parse response\n";
    }
}

echo "\n=== Test 2: Creating Group Chat with Missing Data ===\n";

$data = [
    'group_name' => 'Test Group Chat 2',
    'created_by' => 1,
    'member_ids' => [] // Empty members
];

$options = [
    'http' => [
        'header' => "Content-Type: application/json\r\n",
        'method' => 'POST',
        'content' => json_encode($data)
    ]
];

$context = stream_context_create($options);
$response = file_get_contents($url, false, $context);

if ($response === false) {
    echo "❌ Failed to get response from server\n";
} else {
    echo "✅ Got response:\n";
    echo $response . "\n\n";
    
    $result = json_decode($response, true);
    if ($result && isset($result['success'])) {
        if (!$result['success']) {
            echo "✅ Correctly failed with empty members: " . $result['message'] . "\n";
        } else {
            echo "❌ Should have failed but didn't\n";
        }
    } else {
        echo "❌ Failed to parse response\n";
    }
}

echo "\n=== Test 3: Creating Group Chat with Invalid JSON ===\n";

$options = [
    'http' => [
        'header' => "Content-Type: application/json\r\n",
        'method' => 'POST',
        'content' => 'invalid json'
    ]
];

$context = stream_context_create($options);
$response = file_get_contents($url, false, $context);

if ($response === false) {
    echo "❌ Failed to get response from server\n";
} else {
    echo "✅ Got response:\n";
    echo $response . "\n\n";
    
    $result = json_decode($response, true);
    if ($result && isset($result['success'])) {
        if (!$result['success']) {
            echo "✅ Correctly failed with invalid JSON: " . $result['message'] . "\n";
        } else {
            echo "❌ Should have failed but didn't\n";
        }
    } else {
        echo "❌ Failed to parse response\n";
    }
}

echo "\n=== Test 4: Check if Group Chat was Created in Database ===\n";

require_once 'db_helper.php';
$db = getDB();

// Check chat_groups table
$stmt = $db->prepare("SELECT * FROM chat_groups ORDER BY gc_id DESC LIMIT 5");
$stmt->execute();
$groups = $stmt->fetchAll();

echo "Recent group chats:\n";
foreach ($groups as $group) {
    echo "- ID: " . $group['gc_id'] . ", Name: " . $group['gc_name'] . ", Created by: " . $group['gc_created_by'] . "\n";
}

// Check group_members table
$stmt = $db->prepare("SELECT * FROM group_members ORDER BY gm_joined_at DESC LIMIT 10");
$stmt->execute();
$members = $stmt->fetchAll();

echo "\nRecent group members:\n";
foreach ($members as $member) {
    echo "- Group ID: " . $member['gc_id'] . ", User ID: " . $member['user_id'] . ", Role: " . $member['gm_role'] . "\n";
}

echo "\n=== Test Complete ===\n";
?>




