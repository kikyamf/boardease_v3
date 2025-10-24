<?php
// Debug send_message.php from message folder
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');

try {
    // Test the require_once statements
    $db_helper_exists = file_exists('../db_helper.php');
    $fcm_config_exists = file_exists('../fcm_config.php');
    $service_account_exists = file_exists('../firebase-service-account.json');
    
    if (!$db_helper_exists) {
        throw new Exception('db_helper.php not found in parent directory');
    }
    
    if (!$fcm_config_exists) {
        throw new Exception('fcm_config.php not found in parent directory');
    }
    
    if (!$service_account_exists) {
        throw new Exception('firebase-service-account.json not found in parent directory');
    }
    
    // Try to include the files
    require_once '../db_helper.php';
    require_once '../fcm_config.php';
    
    // Test database connection
    $db = getDB();
    if (!$db) {
        throw new Exception('Database connection failed');
    }
    
    // Test FCM
    $testToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';
    
    $fcm_result = FCMConfig::sendToDevice(
        $testToken,
        'Debug Test',
        'Testing from message folder',
        ['type' => 'debug']
    );
    
    $response = [
        'success' => true,
        'message' => 'All files found and working!',
        'data' => [
            'files_found' => [
                'db_helper' => $db_helper_exists,
                'fcm_config' => $fcm_config_exists,
                'service_account' => $service_account_exists
            ],
            'fcm_test' => $fcm_result
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => [
            'current_directory' => getcwd(),
            'files_checked' => [
                '../db_helper.php' => file_exists('../db_helper.php'),
                '../fcm_config.php' => file_exists('../fcm_config.php'),
                '../firebase-service-account.json' => file_exists('../firebase-service-account.json')
            ]
        ]
    ];
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>























