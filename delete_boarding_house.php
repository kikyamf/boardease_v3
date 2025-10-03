<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

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

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $bh_id = $_POST['bh_id'] ?? '';
    
    if (empty($bh_id)) {
        echo json_encode(["error" => "Missing boarding house ID"]);
        exit();
    }
    
    try {
        // Start transaction
        $conn->begin_transaction();
        
        // First, get all image paths for this boarding house
        $get_images_sql = "SELECT image_path FROM boarding_house_images WHERE bh_id = ?";
        $get_images_stmt = $conn->prepare($get_images_sql);
        $get_images_stmt->bind_param("i", $bh_id);
        $get_images_stmt->execute();
        $result = $get_images_stmt->get_result();
        
        $image_paths = [];
        while($row = $result->fetch_assoc()) {
            $image_paths[] = $row['image_path'];
        }
        $get_images_stmt->close();
        
        // Delete from boarding_house_images table
        $delete_images_sql = "DELETE FROM boarding_house_images WHERE bh_id = ?";
        $delete_images_stmt = $conn->prepare($delete_images_sql);
        $delete_images_stmt->bind_param("i", $bh_id);
        $delete_images_stmt->execute();
        $delete_images_stmt->close();
        
        // First check if the boarding house exists
        $check_sql = "SELECT bh_id, bh_name FROM boarding_houses WHERE bh_id = ?";
        $check_stmt = $conn->prepare($check_sql);
        $check_stmt->bind_param("i", $bh_id);
        $check_stmt->execute();
        $check_result = $check_stmt->get_result();
        $check_stmt->close();
        
        if ($check_result->num_rows == 0) {
            $conn->rollback();
            echo json_encode(["error" => "Boarding house with ID $bh_id not found"]);
            exit();
        }
        
        // Delete from boarding_houses table
        $delete_bh_sql = "DELETE FROM boarding_houses WHERE bh_id = ?";
        $delete_bh_stmt = $conn->prepare($delete_bh_sql);
        $delete_bh_stmt->bind_param("i", $bh_id);
        $delete_bh_result = $delete_bh_stmt->execute();
        $affected_rows = $delete_bh_stmt->affected_rows;
        $delete_bh_stmt->close();
        
        echo json_encode([
            "debug" => [
                "bh_id" => $bh_id,
                "delete_result" => $delete_bh_result,
                "affected_rows" => $affected_rows,
                "images_deleted" => count($image_paths)
            ]
        ]);
        exit();
        
        if ($delete_bh_result && $affected_rows > 0) {
            // Delete physical image files
            foreach ($image_paths as $image_path) {
                $full_path = $_SERVER['DOCUMENT_ROOT'] . "/BoardEase2/" . $image_path;
                if (file_exists($full_path)) {
                    unlink($full_path);
                }
            }
            
            // Commit transaction
            $conn->commit();
            
            echo json_encode([
                "success" => true,
                "message" => "Boarding house and associated images deleted successfully",
                "deleted_images" => count($image_paths)
            ]);
        } else {
            // Rollback transaction
            $conn->rollback();
            echo json_encode(["error" => "Boarding house not found or already deleted"]);
        }
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $conn->rollback();
        echo json_encode(["error" => "Error deleting boarding house: " . $e->getMessage()]);
    }
    
} else {
    echo json_encode(["error" => "Only POST method allowed"]);
}

$conn->close();
?>
