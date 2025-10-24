<?php
// Test FCM notifications
require_once 'fcm_config.php';
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>FCM Notification Test</h2>";

try {
    // Get the latest device token for user_id = 1
    $stmt = $conn->prepare("
        SELECT device_token, user_id 
        FROM device_tokens 
        WHERE user_id = 1 AND is_active = 1 
        ORDER BY created_at DESC 
        LIMIT 1
    ");
    $stmt->execute();
    $result = $stmt->get_result();
    $token_data = $result->fetch_assoc();
    $stmt->close();
    
    if (!$token_data) {
        echo "<h3 style='color: red;'>❌ No active device token found for user_id = 1</h3>";
        echo "<p>Make sure the Android app has been opened to register the device token.</p>";
        exit;
    }
    
    $device_token = $token_data['device_token'];
    echo "<h3>✅ Found device token for user_id = 1</h3>";
    echo "<p>Token: " . substr($device_token, 0, 50) . "...</p>";
    
    // Test 1: Simple notification
    echo "<h3>Test 1: Simple Notification</h3>";
    $result1 = FCMConfig::sendToDevice(
        $device_token,
        "Test Notification",
        "This is a test notification from the server!",
        [
            'type' => 'test',
            'timestamp' => date('Y-m-d H:i:s'),
            'test_id' => '1'
        ]
    );
    
    echo "<h4>Result:</h4>";
    echo "<pre>" . json_encode($result1, JSON_PRETTY_PRINT) . "</pre>";
    
    if ($result1['success']) {
        echo "<h4 style='color: green;'>✅ Test 1 SUCCESS!</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ Test 1 FAILED</h4>";
    }
    
    // Wait 2 seconds
    sleep(2);
    
    // Test 2: Message notification (like the real messaging system)
    echo "<h3>Test 2: Message Notification</h3>";
    $result2 = FCMConfig::sendToDevice(
        $device_token,
        "John Doe",
        "Hello! This is a test message notification.",
        [
            'type' => 'new_message',
            'sender_id' => '2',
            'sender_name' => 'John Doe',
            'receiver_id' => '1',
            'message_id' => '999',
            'chat_type' => 'individual',
            'timestamp' => date('Y-m-d H:i:s')
        ]
    );
    
    echo "<h4>Result:</h4>";
    echo "<pre>" . json_encode($result2, JSON_PRETTY_PRINT) . "</pre>";
    
    if ($result2['success']) {
        echo "<h4 style='color: green;'>✅ Test 2 SUCCESS!</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ Test 2 FAILED</h4>";
    }
    
    // Wait 2 seconds
    sleep(2);
    
    // Test 3: Booking notification
    echo "<h3>Test 3: Booking Notification</h3>";
    $result3 = FCMConfig::sendToDevice(
        $device_token,
        "New Booking Request",
        "You have a new booking request for your boarding house.",
        [
            'type' => 'booking',
            'booking_id' => '123',
            'boarder_name' => 'Mike Johnson',
            'property_name' => 'Sample Boarding House',
            'timestamp' => date('Y-m-d H:i:s')
        ]
    );
    
    echo "<h4>Result:</h4>";
    echo "<pre>" . json_encode($result3, JSON_PRETTY_PRINT) . "</pre>";
    
    if ($result3['success']) {
        echo "<h4 style='color: green;'>✅ Test 3 SUCCESS!</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ Test 3 FAILED</h4>";
    }
    
    // Summary
    echo "<h3>Summary</h3>";
    $success_count = 0;
    if ($result1['success']) $success_count++;
    if ($result2['success']) $success_count++;
    if ($result3['success']) $success_count++;
    
    echo "<p><strong>Tests passed:</strong> $success_count/3</p>";
    
    if ($success_count == 3) {
        echo "<h3 style='color: green;'>🎉 ALL TESTS PASSED! FCM notifications are working perfectly!</h3>";
        echo "<p>Your messaging system should now be able to send notifications successfully.</p>";
    } else {
        echo "<h3 style='color: orange;'>⚠️ Some tests failed. Check the error messages above.</h3>";
    }
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>








