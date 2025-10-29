<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $bhId = 87;
    
    // First, let's check if the table exists and what columns it has
    $tableInfo = $pdo->query("DESCRIBE boarding_houses")->fetchAll(PDO::FETCH_ASSOC);
    
    // Now let's get the specific record
    $sql = "SELECT * FROM boarding_houses WHERE bh_id = ?";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$bhId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result) {
        echo json_encode(array(
            'success' => true,
            'bh_id' => $bhId,
            'table_columns' => $tableInfo,
            'boarding_house_data' => $result,
            'bh_rules_raw' => $result['bh_rules'],
            'bh_rules_is_null' => is_null($result['bh_rules']),
            'bh_rules_is_empty' => empty($result['bh_rules']),
            'bh_rules_length' => strlen($result['bh_rules'] ?? ''),
            'bh_rules_type' => gettype($result['bh_rules'])
        ));
    } else {
        echo json_encode(array('success' => false, 'error' => 'Boarding house not found.'));
    }

} catch (PDOException $e) {
    echo json_encode(array('success' => false, 'error' => 'Database error: ' . $e->getMessage()));
} catch (Exception $e) {
    echo json_encode(array('success' => false, 'error' => 'Server error: ' . $e->getMessage()));
}
?>
