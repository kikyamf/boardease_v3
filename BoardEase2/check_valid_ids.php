<?php
// Script to check valid user_id and bh_id values
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
    
    // Get valid user_id values from users table
    $stmt = $pdo->query("SELECT user_id, reg_id FROM users LIMIT 10");
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $result['valid_user_ids'] = $users;
    
    // Get valid bh_id values from boarding_houses table
    $stmt = $pdo->query("SELECT bh_id, bh_name, status FROM boarding_houses WHERE status = 'Active' LIMIT 20");
    $boardingHouses = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $result['valid_bh_ids'] = $boardingHouses;
    
    // Get total counts
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $userCount = $stmt->fetch(PDO::FETCH_ASSOC);
    $result['total_users'] = $userCount['count'];
    
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM boarding_houses WHERE status = 'Active'");
    $bhCount = $stmt->fetch(PDO::FETCH_ASSOC);
    $result['total_active_boarding_houses'] = $bhCount['count'];
    
    // Get registrations that have corresponding users
    $stmt = $pdo->query("SELECT r.id as reg_id, u.user_id 
                        FROM registrations r 
                        INNER JOIN users u ON r.id = u.reg_id 
                        LIMIT 10");
    $regUsers = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $result['registration_to_user_mapping'] = $regUsers;
    
    // Get current favorites count
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM boarder_favorites");
    $favCount = $stmt->fetch(PDO::FETCH_ASSOC);
    $result['current_favorites_count'] = $favCount['count'];
    
    echo json_encode(array(
        'success' => true,
        'data' => $result
    ), JSON_PRETTY_PRINT);
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ), JSON_PRETTY_PRINT);
} catch (Exception $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Error: ' . $e->getMessage()
    ), JSON_PRETTY_PRINT);
}
?>

