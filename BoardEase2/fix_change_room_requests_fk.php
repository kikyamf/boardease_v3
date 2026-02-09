<?php
// fix_change_room_requests_fk.php
// This script fixes the foreign key constraint on change_room_requests.user_id
// to reference users(user_id) instead of registrations(id)

header('Content-Type: application/json');

define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $steps = [];

    // Step 1: Check current foreign keys
    $stmt = $pdo->query("
        SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = '" . DB_NAME . "'
        AND TABLE_NAME = 'change_room_requests'
        AND COLUMN_NAME = 'user_id'
        AND REFERENCED_TABLE_NAME IS NOT NULL
    ");
    $fks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $steps[] = ['step' => 'Check existing FKs', 'result' => $fks];

    // Step 2: Drop incorrect foreign key if it exists
    foreach ($fks as $fk) {
        if ($fk['REFERENCED_TABLE_NAME'] === 'registrations') {
            $constraintName = $fk['CONSTRAINT_NAME'];
            $pdo->exec("ALTER TABLE change_room_requests DROP FOREIGN KEY `$constraintName`");
            $steps[] = ['step' => 'Dropped FK', 'constraint' => $constraintName];
        }
    }

    // Step 3: Add correct foreign key to users(user_id)
    try {
        $pdo->exec("
            ALTER TABLE change_room_requests 
            ADD CONSTRAINT fk_change_room_user 
            FOREIGN KEY (user_id) REFERENCES users(user_id) 
            ON DELETE CASCADE
        ");
        $steps[] = ['step' => 'Added correct FK', 'result' => 'Success'];
    } catch (Exception $e) {
        // Check if constraint already exists
        if (strpos($e->getMessage(), 'Duplicate key') !== false || strpos($e->getMessage(), 'already exists') !== false) {
            $steps[] = ['step' => 'FK already exists', 'result' => 'Skipped'];
        } else {
            throw $e;
        }
    }

    // Step 4: Verify the fix
    $stmt = $pdo->query("
        SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = '" . DB_NAME . "'
        AND TABLE_NAME = 'change_room_requests'
        AND COLUMN_NAME = 'user_id'
        AND REFERENCED_TABLE_NAME IS NOT NULL
    ");
    $newFks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $steps[] = ['step' => 'Verify new FKs', 'result' => $newFks];

    echo json_encode([
        'success' => true,
        'message' => 'Foreign key constraint fixed successfully',
        'steps' => $steps
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage(),
        'steps' => $steps ?? []
    ], JSON_PRETTY_PRINT);
}
?>
