<?php
// Test search users with role-based filtering
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

echo "<h2>Testing Search Users with Role-Based Filtering</h2>";

try {
    $db = getDB();
    
    // Test 1: Owner searching for boarders
    echo "<h3>Test 1: Owner (user_id=1) searching for 'John'</h3>";
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=1&search_term=John";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data['success']) {
        echo "✅ Owner search successful<br>";
        echo "Found " . count($data['data']['users']) . " users<br>";
        foreach ($data['data']['users'] as $user) {
            echo "- " . $user['full_name'] . " (" . $user['user_type'] . ") - " . $user['boarding_house_name'] . "<br>";
        }
    } else {
        echo "❌ Owner search failed: " . $data['message'] . "<br>";
    }
    
    echo "<br>";
    
    // Test 2: Boarder searching for owner and other boarders
    echo "<h3>Test 2: Boarder (user_id=2) searching for 'Mari'</h3>";
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=2&search_term=Mari";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data['success']) {
        echo "✅ Boarder search successful<br>";
        echo "Found " . count($data['data']['users']) . " users<br>";
        foreach ($data['data']['users'] as $user) {
            echo "- " . $user['full_name'] . " (" . $user['user_type'] . ") - " . $user['boarding_house_name'] . "<br>";
        }
    } else {
        echo "❌ Boarder search failed: " . $data['message'] . "<br>";
    }
    
    echo "<br>";
    
    // Test 3: Search with no results
    echo "<h3>Test 3: Owner searching for 'NonExistent'</h3>";
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=1&search_term=NonExistent";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data['success']) {
        echo "✅ No results search successful<br>";
        echo "Found " . count($data['data']['users']) . " users (expected: 0)<br>";
    } else {
        echo "❌ No results search failed: " . $data['message'] . "<br>";
    }
    
    echo "<br>";
    
    // Test 4: Search with empty term (should fail)
    echo "<h3>Test 4: Empty search term (should fail)</h3>";
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=1&search_term=";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if (!$data['success']) {
        echo "✅ Empty search term correctly failed: " . $data['message'] . "<br>";
    } else {
        echo "❌ Empty search term should have failed but didn't<br>";
    }
    
    echo "<br>";
    
    // Test 5: Search with invalid user_id
    echo "<h3>Test 5: Invalid user_id (should fail)</h3>";
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=999&search_term=John";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if (!$data['success']) {
        echo "✅ Invalid user_id correctly failed: " . $data['message'] . "<br>";
    } else {
        echo "❌ Invalid user_id should have failed but didn't<br>";
    }
    
} catch (Exception $e) {
    echo "❌ Test failed: " . $e->getMessage() . "<br>";
}

ob_clean();
?>




