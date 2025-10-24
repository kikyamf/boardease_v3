<?php
header('Content-Type: application/json');

try {
    // Database connection
    $servername = "localhost";
    $username = "root";
    $password = "";
    $dbname = "boardease2";
    
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    if ($conn->connect_error) {
        throw new Exception("Connection failed: " . $conn->connect_error);
    }
    
    // Get parameters
    $bh_id = $_POST['bh_id'] ?? '';
    
    // Test if table exists
    $result = $conn->query("SHOW TABLES LIKE 'boarding_houses'");
    if ($result->num_rows == 0) {
        throw new Exception("Table 'boarding_houses' does not exist");
    }
    
    // Check if boarding house exists
    $check_sql = "SELECT * FROM boarding_houses WHERE bh_id = ?";
    $check_stmt = $conn->prepare($check_sql);
    $check_stmt->bind_param("i", $bh_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();
    
    $found_records = [];
    while($row = $result->fetch_assoc()) {
        $found_records[] = $row;
    }
    $check_stmt->close();
    
    echo json_encode([
        "success" => true,
        "message" => "PHP is working",
        "bh_id" => $bh_id,
        "table_exists" => true,
        "records_found" => count($found_records),
        "found_records" => $found_records
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        "error" => $e->getMessage()
    ]);
}

if (isset($conn)) {
    $conn->close();
}
?>


































