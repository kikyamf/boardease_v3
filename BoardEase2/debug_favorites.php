<?php
// Debug script for favorites functionality
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

$debug = array();

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $debug['database_connection'] = 'Success';
    
    // Check if boarder_favorites table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'boarder_favorites'");
    $tableExists = $stmt->rowCount() > 0;
    $debug['table_exists'] = $tableExists;
    
    if ($tableExists) {
        // Get table structure
        $stmt = $pdo->query("DESCRIBE boarder_favorites");
        $structure = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $debug['table_structure'] = $structure;
        
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
        $debug['foreign_keys'] = $foreignKeys;
        
        // Get sample data
        $stmt = $pdo->query("SELECT * FROM boarder_favorites LIMIT 10");
        $sample = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $debug['sample_data'] = $sample;
        $debug['total_favorites'] = $pdo->query("SELECT COUNT(*) as count FROM boarder_favorites")->fetch(PDO::FETCH_ASSOC)['count'];
    } else {
        $debug['error'] = 'Table boarder_favorites does not exist';
        $debug['create_table_sql'] = "
CREATE TABLE IF NOT EXISTS `boarder_favorites` (
  `fav_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  PRIMARY KEY (`fav_id`),
  UNIQUE KEY `unique_favorite` (`user_id`,`bh_id`),
  KEY `fk_user` (`user_id`),
  KEY `fk_bh` (`bh_id`),
  CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bh` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        ";
    }
    
    // Check users table
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $userCount = $stmt->fetch(PDO::FETCH_ASSOC);
    $debug['users_count'] = $userCount['count'];
    
    // Check registrations table
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM registrations");
    $regCount = $stmt->fetch(PDO::FETCH_ASSOC);
    $debug['registrations_count'] = $regCount['count'];
    
    // Check relationship between users and registrations
    $stmt = $pdo->query("SELECT u.user_id, u.reg_id, r.id as reg_id_from_registrations 
                        FROM users u 
                        INNER JOIN registrations r ON u.reg_id = r.id 
                        LIMIT 5");
    $relationships = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $debug['user_registration_relationships'] = $relationships;
    
    // Test: Get a sample user_id from registrations and check if they have a users entry
    $stmt = $pdo->query("SELECT r.id as reg_id, u.user_id 
                        FROM registrations r 
                        LEFT JOIN users u ON r.id = u.reg_id 
                        LIMIT 5");
    $userMapping = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $debug['registration_to_user_mapping'] = $userMapping;
    
    echo json_encode(array(
        'success' => true,
        'debug' => $debug
    ), JSON_PRETTY_PRINT);
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage(),
        'debug' => $debug
    ), JSON_PRETTY_PRINT);
} catch (Exception $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Error: ' . $e->getMessage(),
        'debug' => $debug
    ), JSON_PRETTY_PRINT);
}
?>

