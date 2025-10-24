<?php
// Debug version of register_device_token.php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h3>Debug: register_device_token.php</h3>";

// Test 1: Check if dbConfig.php exists
echo "<h4>1. Checking dbConfig.php</h4>";
if (file_exists('dbConfig.php')) {
    echo "✅ dbConfig.php exists<br>";
    require_once 'dbConfig.php';
    echo "✅ dbConfig.php loaded successfully<br>";
} else {
    echo "❌ dbConfig.php not found<br>";
    exit;
}

// Test 2: Check database connection
echo "<h4>2. Testing database connection</h4>";
try {
    // Use the $conn variable from dbConfig.php
    $db = $conn;
    echo "✅ Database connection successful<br>";
} catch (Exception $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "<br>";
    exit;
}

// Test 3: Check if users table exists
echo "<h4>3. Checking users table</h4>";
try {
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
    $stmt->execute();
    $result = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    echo "✅ Users table exists, count: " . $result['count'] . "<br>";
} catch (Exception $e) {
    echo "❌ Users table error: " . $e->getMessage() . "<br>";
}

// Test 4: Check if device_tokens table exists
echo "<h4>4. Checking device_tokens table</h4>";
try {
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM device_tokens");
    $stmt->execute();
    $result = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    echo "✅ Device_tokens table exists, count: " . $result['count'] . "<br>";
} catch (Exception $e) {
    echo "❌ Device_tokens table error: " . $e->getMessage() . "<br>";
}

// Test 5: Check if user_id = 1 exists
echo "<h4>5. Checking if user_id = 1 exists</h4>";
try {
    $stmt = $db->prepare("SELECT user_id FROM users WHERE user_id = ?");
    $stmt->bind_param("i", $user_id);
    $user_id = 1;
    $stmt->execute();
    $result = $stmt->get_result();
    $user = $result->fetch_assoc();
    $stmt->close();
    if ($user) {
        echo "✅ User ID 1 exists<br>";
    } else {
        echo "❌ User ID 1 not found<br>";
    }
} catch (Exception $e) {
    echo "❌ User check error: " . $e->getMessage() . "<br>";
}

// Test 6: Simulate the actual registration
echo "<h4>6. Testing device token registration</h4>";
try {
    $user_id = 1;
    $device_token = 'doIZWxHNRkqo_lVUVcNn6a:APA91bGvBwcxisdLz9oNw6CJB1gKSaqz0HmNSLqgOfua9_R_X97IWRIas6HSV0CS4m1LoSMwI2bX959PyMn-vDmxy2K8yIkptrFx8nyzNyaWib5IYH3-0PM';
    $device_type = 'android';
    $app_version = '1.0.0';
    
    // Check if device token already exists
    $stmt = $db->prepare("
        SELECT token_id, is_active 
        FROM device_tokens 
        WHERE user_id = ? AND device_token = ?
    ");
    $stmt->bind_param("is", $user_id, $device_token);
    $stmt->execute();
    $result = $stmt->get_result();
    $existing_token = $result->fetch_assoc();
    $stmt->close();
    
    if ($existing_token) {
        echo "✅ Device token already exists, updating...<br>";
        if (!$existing_token['is_active']) {
            $stmt = $db->prepare("
                UPDATE device_tokens 
                SET is_active = 1, updated_at = NOW() 
                WHERE token_id = ?
            ");
            $stmt->bind_param("i", $existing_token['token_id']);
            $stmt->execute();
            $stmt->close();
            echo "✅ Token activated<br>";
        }
    } else {
        echo "✅ Device token not found, inserting new one...<br>";
        
        // Deactivate old tokens
        $stmt = $db->prepare("
            UPDATE device_tokens 
            SET is_active = 0, updated_at = NOW() 
            WHERE user_id = ? AND is_active = 1
        ");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $stmt->close();
        echo "✅ Old tokens deactivated<br>";
        
        // Insert new token
        $stmt = $db->prepare("
            INSERT INTO device_tokens (user_id, device_token, device_type, app_version, is_active, created_at, updated_at) 
            VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        ");
        $stmt->bind_param("isss", $user_id, $device_token, $device_type, $app_version);
        $stmt->execute();
        $stmt->close();
        echo "✅ New token inserted<br>";
    }
    
    echo "<h4 style='color: green;'>✅ SUCCESS! Device token registration test passed!</h4>";
    
} catch (Exception $e) {
    echo "<h4 style='color: red;'>❌ Registration test failed: " . $e->getMessage() . "</h4>";
}

echo "<h4>Summary</h4>";
echo "If all tests pass, the issue might be with the POST data handling or output buffering.<br>";
echo "If any test fails, that's the root cause of the 500 error.";
?>
