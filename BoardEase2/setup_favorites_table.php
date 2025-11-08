<?php
// Script to check and setup boarder_favorites table
// This will check the current structure and suggest changes

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $result = array();
    $result['steps'] = array();
    
    // Check if table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'boarder_favorites'");
    $tableExists = $stmt->rowCount() > 0;
    $result['table_exists'] = $tableExists;
    
    if ($tableExists) {
        // Get current table structure
        $stmt = $pdo->query("DESCRIBE boarder_favorites");
        $structure = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $result['current_structure'] = $structure;
        
        // Check foreign keys
        $stmt = $pdo->query("SELECT 
            CONSTRAINT_NAME, 
            TABLE_NAME, 
            COLUMN_NAME, 
            REFERENCED_TABLE_NAME, 
            REFERENCED_COLUMN_NAME 
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = '$dbname' 
            AND TABLE_NAME = 'boarder_favorites' 
            AND REFERENCED_TABLE_NAME IS NOT NULL");
        $foreignKeys = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $result['current_foreign_keys'] = $foreignKeys;
        
        // Check if user_id references registrations.id
        $referencesRegistrations = false;
        foreach ($foreignKeys as $fk) {
            if ($fk['REFERENCED_TABLE_NAME'] === 'registrations' && $fk['REFERENCED_COLUMN_NAME'] === 'id') {
                $referencesRegistrations = true;
                break;
            }
        }
        
        $result['references_registrations'] = $referencesRegistrations;
        
        if (!$referencesRegistrations) {
            $result['needs_update'] = true;
            $result['update_sql'] = "
-- Step 1: Drop existing foreign key constraints
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS boarder_favorites_ibfk_1;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS boarder_favorites_ibfk_2;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS fk_user;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS fk_user_favorites;

-- Step 2: Add new foreign key to reference registrations.id
ALTER TABLE boarder_favorites 
ADD CONSTRAINT fk_user_reg_favorites 
FOREIGN KEY (user_id) REFERENCES registrations(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Step 3: Ensure foreign key for bh_id exists
ALTER TABLE boarder_favorites 
ADD CONSTRAINT fk_bh_favorites 
FOREIGN KEY (bh_id) REFERENCES boarding_houses(bh_id) 
ON DELETE CASCADE ON UPDATE CASCADE;
            ";
        } else {
            $result['needs_update'] = false;
            $result['message'] = 'Table structure is correct. It already references registrations.id';
        }
    } else {
        // Table doesn't exist, provide creation script
        $result['needs_update'] = true;
        $result['create_sql'] = "
CREATE TABLE `boarder_favorites` (
  `fav_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT 'References registrations.id',
  `bh_id` int(11) NOT NULL COMMENT 'References boarding_houses.bh_id',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`fav_id`),
  UNIQUE KEY `unique_favorite` (`user_id`, `bh_id`),
  KEY `fk_user_reg` (`user_id`),
  KEY `fk_bh` (`bh_id`),
  CONSTRAINT `fk_bh_favorites` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_reg_favorites` FOREIGN KEY (`user_id`) REFERENCES `registrations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        ";
    }
    
    echo json_encode($result, JSON_PRETTY_PRINT);
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ), JSON_PRETTY_PRINT);
}
?>

