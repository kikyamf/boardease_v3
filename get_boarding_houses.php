<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

include 'dbConfig.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$bh_id = isset($_POST['bh_id']) ? intval($_POST['bh_id']) : 0; // New parameter for single BH

$baseUrl = "http://192.168.101.6/BoardEase2/";

try {
    if ($bh_id > 0) {
        // Return single boarding house with complete details for editing
        $sql = "SELECT b.bh_id, b.bh_name, b.bh_address, b.user_id,
                       b.bh_description, b.bh_rules, b.number_of_bathroom, b.area, b.build_year, b.status
                FROM boarding_houses b
                WHERE b.bh_id = ?";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $bh_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($row = $result->fetch_assoc()) {
            // Get all images for this boarding house
            $imageSql = "SELECT image_path FROM boarding_house_images WHERE bh_id = ? ORDER BY image_id";
            $imageStmt = $conn->prepare($imageSql);
            $imageStmt->bind_param("i", $bh_id);
            $imageStmt->execute();
            $imageResult = $imageStmt->get_result();
            
            $images = [];
            while ($imageRow = $imageResult->fetch_assoc()) {
                $images[] = $baseUrl . $imageRow['image_path'];
            }
            
            // Add images array to the response
            $row['images'] = $images;
            
            echo json_encode($row);
        } else {
            echo json_encode([
                "success" => false,
                "error" => "Boarding house not found"
            ]);
        }
        
        $stmt->close();
        $imageStmt->close();
        
    } else if ($user_id > 0) {
        // Return list of boarding houses for manage listing
        $sql = "SELECT b.bh_id, b.bh_name, 
                       (SELECT img.image_path FROM boarding_house_images img WHERE img.bh_id = b.bh_id LIMIT 1) AS image_path
                FROM boarding_houses b
                WHERE b.user_id = ?
                ORDER BY b.bh_id DESC";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $listings = [];
        while ($row = $result->fetch_assoc()) {
            $imagePath = "";
            if ($row['image_path']) {
                $imagePath = $baseUrl . $row['image_path'];
            }
            
            $listings[] = [
                "bh_id" => $row['bh_id'],
                "bh_name" => $row['bh_name'],
                "image_path" => $imagePath
            ];
        }

        echo json_encode($listings);
        $stmt->close();
        
    } else {
        // Return all boarding houses for explore/search
        $sql = "SELECT b.bh_id, b.bh_name, b.bh_address, b.bh_description, b.bh_rules,
                       b.number_of_bathroom, b.area, b.build_year, b.user_id,
                       (SELECT img.image_path FROM boarding_house_images img WHERE img.bh_id = b.bh_id LIMIT 1) AS image_path
                FROM boarding_houses b
                ORDER BY b.bh_id DESC";

        $stmt = $conn->prepare($sql);
        $stmt->execute();
        $result = $stmt->get_result();

        $boardingHouses = [];
        while ($row = $result->fetch_assoc()) {
            $imagePath = "";
            if ($row['image_path']) {
                $imagePath = $baseUrl . $row['image_path'];
            }
            
            $boardingHouses[] = [
                "bh_id" => $row['bh_id'],
                "bh_name" => $row['bh_name'],
                "bh_address" => $row['bh_address'],
                "bh_description" => $row['bh_description'],
                "bh_rules" => $row['bh_rules'],
                "number_of_bathroom" => $row['number_of_bathroom'],
                "area" => $row['area'],
                "build_year" => $row['build_year'],
                "user_id" => $row['user_id'],
                "image_path" => $imagePath
            ];
        }

        echo json_encode($boardingHouses);
        $stmt->close();
    }
    
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "error" => "Database error: " . $e->getMessage()
    ]);
}

$conn->close();
?>