<?php
/**
 * Safe Configuration Test for BoardEase Messaging System
 * This test handles empty database gracefully
 */

// Set error reporting
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json');

try {
    // Test 1: Database Connection
    echo "=== BoardEase Messaging Configuration Test ===\n\n";
    
    // Test database connection
    if (!file_exists('db_connection.php')) {
        throw new Exception('db_connection.php not found');
    }
    
    require_once 'db_connection.php';
    
    $db = getDB();
    if (!$db) {
        throw new Exception('Database connection failed');
    }
    
    echo "✅ Database connection: SUCCESS\n";
    
    // Test 2: Check if tables exist
    $tables = ['users', 'messages', 'chat_groups', 'group_members', 'group_messages', 'device_tokens'];
    $existing_tables = [];
    $missing_tables = [];
    
    foreach ($tables as $table) {
        $stmt = $db->prepare("SHOW TABLES LIKE ?");
        $stmt->execute([$table]);
        if ($stmt->rowCount() > 0) {
            $existing_tables[] = $table;
            echo "✅ Table '{$table}': EXISTS\n";
        } else {
            $missing_tables[] = $table;
            echo "❌ Table '{$table}': MISSING\n";
        }
    }
    
    // Test 3: Check if we have any data
    $has_data = false;
    if (in_array('users', $existing_tables)) {
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
        $stmt->execute();
        $user_count = $stmt->fetch()['count'];
        
        if ($user_count > 0) {
            echo "✅ Users table: {$user_count} users found\n";
            $has_data = true;
        } else {
            echo "⚠️  Users table: EMPTY (no users found)\n";
        }
    }
    
    // Test 4: Test a simple endpoint (even with empty data)
    echo "\n=== Testing Endpoints ===\n";
    
    // Test get_users_for_messaging.php
    if (file_exists('get_users_for_messaging.php')) {
        echo "Testing get_users_for_messaging.php...\n";
        
        // Simulate the endpoint call
        $_GET['user_id'] = 1;
        $_GET['limit'] = 10;
        
        ob_start();
        include 'get_users_for_messaging.php';
        $output = ob_get_clean();
        
        // Check if output is valid JSON
        $json = json_decode($output, true);
        if (json_last_error() === JSON_ERROR_NONE) {
            echo "✅ get_users_for_messaging.php: Returns valid JSON\n";
            if (isset($json['success']) && $json['success']) {
                echo "✅ Response indicates success\n";
            } else {
                echo "⚠️  Response indicates no data (expected for empty database)\n";
            }
        } else {
            echo "❌ get_users_for_messaging.php: Invalid JSON response\n";
            echo "Response: " . substr($output, 0, 200) . "...\n";
        }
    } else {
        echo "❌ get_users_for_messaging.php: File not found\n";
    }
    
    // Test 5: FCM Configuration
    echo "\n=== Testing FCM Configuration ===\n";
    
    if (file_exists('fcm_config_real.php')) {
        echo "✅ fcm_config_real.php: EXISTS\n";
        
        require_once 'fcm_config_real.php';
        
        try {
            $fcm = new FCMConfig();
            echo "✅ FCM Config class: LOADED\n";
            
            // Test access token generation (this will fail if credentials are wrong)
            try {
                $token = $fcm->getAccessToken();
                if ($token) {
                    echo "✅ FCM Access Token: SUCCESS\n";
                } else {
                    echo "❌ FCM Access Token: FAILED\n";
                }
            } catch (Exception $e) {
                echo "❌ FCM Access Token: ERROR - " . $e->getMessage() . "\n";
            }
            
        } catch (Exception $e) {
            echo "❌ FCM Config class: ERROR - " . $e->getMessage() . "\n";
        }
    } else {
        echo "❌ fcm_config_real.php: NOT FOUND\n";
    }
    
    // Summary
    echo "\n=== SUMMARY ===\n";
    echo "Database Connection: " . ($db ? "✅ OK" : "❌ FAILED") . "\n";
    echo "Tables Created: " . count($existing_tables) . "/" . count($tables) . "\n";
    echo "Has Data: " . ($has_data ? "✅ YES" : "⚠️  NO (run insert_test_data.sql)") . "\n";
    
    if (count($missing_tables) > 0) {
        echo "\n❌ MISSING TABLES:\n";
        foreach ($missing_tables as $table) {
            echo "   - {$table}\n";
        }
        echo "\nRun: messaging_database_schema.sql\n";
    }
    
    if (!$has_data) {
        echo "\n⚠️  NO DATA FOUND:\n";
        echo "Run: insert_test_data.sql\n";
    }
    
    echo "\n=== NEXT STEPS ===\n";
    if (count($missing_tables) > 0) {
        echo "1. Create missing tables: messaging_database_schema.sql\n";
    }
    if (!$has_data) {
        echo "2. Insert test data: insert_test_data.sql\n";
    }
    echo "3. Test messaging endpoints\n";
    echo "4. Test FCM notifications\n";
    
    $response = [
        'success' => true,
        'database_connected' => true,
        'tables_exist' => count($existing_tables),
        'tables_total' => count($tables),
        'has_data' => $has_data,
        'missing_tables' => $missing_tables,
        'message' => 'Configuration test completed'
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage(),
        'message' => 'Configuration test failed'
    ];
    
    echo "\n❌ CONFIGURATION TEST FAILED\n";
    echo "Error: " . $e->getMessage() . "\n";
}

// Clean output buffer and return JSON
ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
?>























