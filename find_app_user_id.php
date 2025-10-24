<?php
// Quick script to find which user ID matches your app
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== FINDING YOUR APP'S USER ID ===\n\n";
    
    // Test the most active users from your previous output
    $active_users = [1, 2, 6, 11, 4, 3, 8, 9]; // Based on your previous output
    
    echo "Testing the most active users to find which one matches your app:\n\n";
    
    foreach ($active_users as $user_id) {
        echo "=== TESTING USER ID $user_id ===\n";
        
        $url = "http://localhost/get_chat_list_simple.php?user_id=$user_id";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            $chats = $data['data']['chats'];
            echo "Found " . count($chats) . " chats:\n";
            
            $found_names = [];
            foreach ($chats as $chat) {
                $found_names[] = $chat['other_user_name'];
                $time = $chat['last_message_time'];
                $message = substr($chat['last_message'], 0, 40);
                echo "  - {$chat['other_user_name']} | Last: '$message...' | Time: $time\n";
            }
            
            // Check if this matches your app
            $app_names = ['David Brown', 'Namz Baer', 'John Doe'];
            $matches = array_intersect($found_names, $app_names);
            
            if (count($matches) > 0) {
                echo "🎯 POTENTIAL MATCH! User ID $user_id has conversations with: " . implode(', ', $matches) . "\n";
                
                // Check if the times match your app
                echo "Times in your app: David (10:13 PM), Namz (8:27 PM), John (6:57 PM)\n";
                echo "Check if the times above match your app!\n";
            }
            
            echo "\n";
        } else {
            echo "❌ Error or no chats for user $user_id\n\n";
        }
    }
    
    echo "=== SUMMARY ===\n";
    echo "Your app shows:\n";
    echo "- David Brown (10:13 PM)\n";
    echo "- Namz Baer (8:27 PM)\n";
    echo "- John Doe (6:57 PM)\n";
    echo "- BH CUAS Chat (6:47 PM)\n\n";
    echo "Look for the user ID above that shows similar names and times!\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















