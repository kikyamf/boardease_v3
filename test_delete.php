<?php
header('Content-Type: application/json');

// Database connection
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "boardease2";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    echo json_encode(["error" => "Connection failed: " . $conn->connect_error]);
    exit();
}

try {
    // Test manual deletion
    $bh_id = 15;
    $image_path = "/uploads/boarding_house_images/bh_15_68da873658ba4.jpg";
    
    // First, check if the record exists
    $check_sql = "SELECT * FROM boarding_house_images WHERE bh_id = ? AND image_path = ?";
    $check_stmt = $conn->prepare($check_sql);
    $check_stmt->bind_param("is", $bh_id, $image_path);
    $check_stmt->execute();
    $result = $check_stmt->get_result();
    
    $found_records = [];
    while($row = $result->fetch_assoc()) {
        $found_records[] = $row;
    }
    $check_stmt->close();
    
    // Now try to delete
    $delete_sql = "DELETE FROM boarding_house_images WHERE bh_id = ? AND image_path = ?";
    $delete_stmt = $conn->prepare($delete_sql);
    $delete_stmt->bind_param("is", $bh_id, $image_path);
    $delete_result = $delete_stmt->execute();
    $affected_rows = $delete_stmt->affected_rows;
    $delete_stmt->close();
    
    echo json_encode([
        "success" => true,
        "bh_id" => $bh_id,
        "image_path" => $image_path,
        "records_found_before_delete" => count($found_records),
        "found_records" => $found_records,
        "delete_executed" => $delete_result,
        "affected_rows" => $affected_rows
    ]);
    
} catch (Exception $e) {
    echo json_encode(["error" => $e->getMessage()]);
}

$conn->close();
?>


































