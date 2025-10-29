<?php
// Debug script to check database connection and data
header('Content-Type: application/json');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get database info
    $dbInfo = $pdo->query("SELECT DATABASE() as current_db")->fetch();
    
    // Count total boarding houses
    $countStmt = $pdo->query("SELECT COUNT(*) as total FROM boarding_houses");
    $totalCount = $countStmt->fetch()['total'];
    
    // Get all boarding houses with their IDs and names
    $allStmt = $pdo->query("SELECT bh_id, bh_name, bh_rules FROM boarding_houses ORDER BY bh_id DESC LIMIT 10");
    $allBoardingHouses = $allStmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check specifically for bh_id = 87
    $specificStmt = $pdo->prepare("SELECT * FROM boarding_houses WHERE bh_id = 87");
    $specificStmt->execute();
    $specificBH = $specificStmt->fetch(PDO::FETCH_ASSOC);

    echo json_encode(array(
        'success' => true,
        'database_info' => array(
            'connected_to' => $dbInfo['current_db'],
            'host' => $host,
            'total_boarding_houses' => $totalCount
        ),
        'recent_boarding_houses' => $allBoardingHouses,
        'bh_id_87_exists' => $specificBH ? true : false,
        'bh_id_87_data' => $specificBH
    ));

} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
}
?>
