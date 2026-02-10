<?php
// check_change_room_requests_status.php
// Diagnostic script to check the current state of change_room_requests table

header('Content-Type: application/json');

define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $info = [];

    // Check if table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'change_room_requests'");
    $info['table_exists'] = $stmt->rowCount() > 0;

    if ($info['table_exists']) {
        // Get table structure
        $stmt = $pdo->query("DESCRIBE change_room_requests");
        $info['columns'] = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Get foreign keys
        $stmt = $pdo->query("
            SELECT 
                CONSTRAINT_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = '" . DB_NAME . "'
            AND TABLE_NAME = 'change_room_requests'
            AND REFERENCED_TABLE_NAME IS NOT NULL
        ");
        $info['foreign_keys'] = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Check if users table exists and has user_id column
        $stmt = $pdo->query("SELECT user_id FROM users LIMIT 1");
        $info['users_table_accessible'] = true;

        // Check if registrations table exists
        try {
            $stmt = $pdo->query("SELECT id FROM registrations LIMIT 1");
            $info['registrations_table_exists'] = true;
        } catch (Exception $e) {
            $info['registrations_table_exists'] = false;
        }

        // Get sample user IDs from both tables
        $stmt = $pdo->query("SELECT user_id FROM users LIMIT 5");
        $info['sample_user_ids'] = $stmt->fetchAll(PDO::FETCH_COLUMN);

        if ($info['registrations_table_exists']) {
            $stmt = $pdo->query("SELECT id FROM registrations LIMIT 5");
            $info['sample_registration_ids'] = $stmt->fetchAll(PDO::FETCH_COLUMN);
        }
    }

    echo json_encode([
        'success' => true,
        'info' => $info
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ], JSON_PRETTY_PRINT);
}
?>
