<?php
// Debug chat list duplicates
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== DEBUGGING CHAT LIST DUPLICATES ===\n\n";

require_once 'db_helper.php';
$db = getDB();

$user_id = 1; // Test with user 1

echo "1. CHECKING INDIVIDUAL CHAT DUPLICATES FOR USER $user_id:\n";

// First, let's see all messages for this user
$stmt = $db->prepare("
    SELECT 
        m.message_id,
        m.sender_id,
        m.receiver_id,
        m.msg_text,
        m.msg_timestamp,
        CASE 
            WHEN m.sender_id = ? THEN m.receiver_id
            ELSE m.sender_id
        END as other_user_id,
        r.f_name,
        r.l_name
    FROM messages m
    JOIN users u ON (
        CASE 
            WHEN m.sender_id = ? THEN m.receiver_id
            ELSE m.sender_id
        END = u.user_id
    )
    JOIN registrations r ON u.reg_id = r.reg_id
    WHERE (m.sender_id = ? OR m.receiver_id = ?) 
    AND m.msg_status != 'Deleted'
    ORDER BY m.msg_timestamp DESC
    LIMIT 20
");

$stmt->execute([$user_id, $user_id, $user_id, $user_id]);
$all_messages = $stmt->fetchAll();

echo "Recent messages for user $user_id:\n";
foreach ($all_messages as $msg) {
    $direction = $msg['sender_id'] == $user_id ? "→" : "←";
    echo "- $direction {$msg['f_name']} {$msg['l_name']} (ID: {$msg['other_user_id']}): '{$msg['msg_text']}' at {$msg['msg_timestamp']}\n";
}

echo "\n2. CHECKING FOR DUPLICATE USER NAMES:\n";

// Check if there are multiple users with the same name
$stmt = $db->prepare("
    SELECT 
        r.f_name,
        r.l_name,
        COUNT(*) as count,
        GROUP_CONCAT(u.user_id) as user_ids
    FROM registrations r
    JOIN users u ON r.reg_id = u.reg_id
    WHERE u.status = 'Active' AND r.status = 'Approved'
    GROUP BY r.f_name, r.l_name
    HAVING COUNT(*) > 1
");

$stmt->execute();
$duplicate_names = $stmt->fetchAll();

if ($duplicate_names) {
    echo "❌ FOUND DUPLICATE NAMES:\n";
    foreach ($duplicate_names as $dup) {
        echo "- {$dup['f_name']} {$dup['l_name']}: User IDs {$dup['user_ids']} (Count: {$dup['count']})\n";
    }
} else {
    echo "✅ No duplicate names found\n";
}

echo "\n3. TESTING CHAT LIST QUERY:\n";

// Test the actual chat list query
$stmt = $db->prepare("
    SELECT 
        other_user_id,
        first_name,
        last_name,
        user_type,
        user_status,
        last_message,
        last_message_time,
        last_message_status,
        last_sender_id,
        unread_count
    FROM (
        SELECT 
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END as other_user_id,
            r.f_name as first_name,
            r.l_name as last_name,
            r.role as user_type,
            u.status as user_status,
            m.msg_text as last_message,
            m.msg_timestamp as last_message_time,
            m.msg_status as last_message_status,
            m.sender_id as last_sender_id,
            (SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count,
            ROW_NUMBER() OVER (PARTITION BY other_user_id ORDER BY m.msg_timestamp DESC) as rn
        FROM messages m
        JOIN users u ON (
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END = u.user_id
        )
        JOIN registrations r ON u.reg_id = r.reg_id
        WHERE (m.sender_id = ? OR m.receiver_id = ?) 
        AND m.msg_status != 'Deleted'
    ) as chat_messages
    WHERE rn = 1
    ORDER BY last_message_time DESC
");

$stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id]);
$individual_chats = $stmt->fetchAll();

echo "Chat list results:\n";
foreach ($individual_chats as $chat) {
    echo "- {$chat['first_name']} {$chat['last_name']} (ID: {$chat['other_user_id']}): '{$chat['last_message']}' at {$chat['last_message_time']}\n";
}

echo "\n4. CHECKING FOR DUPLICATE USER IDs IN CHAT LIST:\n";

$user_ids = array_column($individual_chats, 'other_user_id');
$duplicate_user_ids = array_diff_assoc($user_ids, array_unique($user_ids));

if ($duplicate_user_ids) {
    echo "❌ FOUND DUPLICATE USER IDs IN CHAT LIST:\n";
    foreach ($duplicate_user_ids as $index => $user_id_dup) {
        echo "- User ID $user_id_dup appears multiple times\n";
    }
} else {
    echo "✅ No duplicate user IDs in chat list\n";
}

echo "\n5. CHECKING FOR DUPLICATE NAMES IN CHAT LIST:\n";

$names = [];
foreach ($individual_chats as $chat) {
    $full_name = $chat['first_name'] . ' ' . $chat['last_name'];
    if (isset($names[$full_name])) {
        echo "❌ DUPLICATE NAME FOUND: $full_name\n";
        echo "   - User ID {$names[$full_name]} and User ID {$chat['other_user_id']}\n";
    } else {
        $names[$full_name] = $chat['other_user_id'];
    }
}

if (count($names) === count($individual_chats)) {
    echo "✅ No duplicate names in chat list\n";
}

echo "\n=== DEBUG COMPLETE ===\n";
?>








