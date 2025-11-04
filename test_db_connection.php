<?php
// test_db_connection.php - Diagnostic endpoint to test database connectivity
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

$result = array(
    'timestamp' => date('Y-m-d H:i:s'),
    'checks' => array()
);

// Check 1: Test MySQL connection
try {
    $dsn = "mysql:host=$host;charset=utf8";
    $pdo = new PDO($dsn, $username, $password, array(
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5
    ));
    $result['checks']['mysql_connection'] = array(
        'status' => 'success',
        'message' => 'MySQL server is running and accessible'
    );
} catch (PDOException $e) {
    $result['checks']['mysql_connection'] = array(
        'status' => 'failed',
        'message' => 'Cannot connect to MySQL: ' . $e->getMessage(),
        'error_code' => $e->getCode(),
        'suggestion' => $e->getCode() == 2002 ? 'Start XAMPP MySQL service' : 'Check database configuration'
    );
    echo json_encode($result);
    exit();
}

// Check 2: Test database exists and is accessible
try {
    $dsn = "mysql:host=$host;dbname=$dbname;charset=utf8";
    $pdo = new PDO($dsn, $username, $password, array(
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5
    ));
    $result['checks']['database_access'] = array(
        'status' => 'success',
        'message' => "Database '$dbname' is accessible"
    );
} catch (PDOException $e) {
    $result['checks']['database_access'] = array(
        'status' => 'failed',
        'message' => "Cannot access database '$dbname': " . $e->getMessage(),
        'error_code' => $e->getCode()
    );
    echo json_encode($result);
    exit();
}

// Check 3: Test table exists
try {
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM boarding_houses");
    $count = $stmt->fetch(PDO::FETCH_ASSOC);
    $result['checks']['table_access'] = array(
        'status' => 'success',
        'message' => "Table 'boarding_houses' exists",
        'record_count' => (int)$count['count']
    );
} catch (PDOException $e) {
    $result['checks']['table_access'] = array(
        'status' => 'failed',
        'message' => "Cannot access table 'boarding_houses': " . $e->getMessage()
    );
}

// Check 4: Test query execution
try {
    $sql = "SELECT bh_id, bh_name FROM boarding_houses WHERE status = 'active' LIMIT 5";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $result['checks']['query_execution'] = array(
        'status' => 'success',
        'message' => 'Query executed successfully',
        'sample_records' => $results
    );
} catch (PDOException $e) {
    $result['checks']['query_execution'] = array(
        'status' => 'failed',
        'message' => 'Query execution failed: ' . $e->getMessage()
    );
}

// Overall status
$allPassed = true;
foreach ($result['checks'] as $check) {
    if ($check['status'] !== 'success') {
        $allPassed = false;
        break;
    }
}

$result['overall_status'] = $allPassed ? 'success' : 'failed';
$result['message'] = $allPassed ? 
    'All database checks passed. Database is ready.' : 
    'Some database checks failed. See details above.';

echo json_encode($result, JSON_PRETTY_PRINT);
?>
