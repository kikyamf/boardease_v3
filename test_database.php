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
    // Get all images for bh_id = 15
    $sql = "SELECT * FROM boarding_house_images WHERE bh_id = 15";
    $result = $conn->query($sql);
    
    $images = [];
    if ($result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $images[] = $row;
        }
    }
    
    // Also check the table structure
    $structure_sql = "DESCRIBE boarding_house_images";
    $structure_result = $conn->query($structure_sql);
    
    $structure = [];
    if ($structure_result->num_rows > 0) {
        while($row = $structure_result->fetch_assoc()) {
            $structure[] = $row;
        }
    }
    
    echo json_encode([
        "success" => true,
        "bh_id" => 15,
        "images_found" => count($images),
        "images" => $images,
        "table_structure" => $structure
    ]);
    
} catch (Exception $e) {
    echo json_encode(["error" => $e->getMessage()]);
}

$conn->close();
?>


































