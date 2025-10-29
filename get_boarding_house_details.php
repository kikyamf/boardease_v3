<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get boarding house ID from request
    $bhId = isset($_GET['bh_id']) ? (int)$_GET['bh_id'] : 0;

    if ($bhId === 0) {
        echo json_encode(array('success' => false, 'error' => 'Boarding house ID is required.'));
        exit();
    }

    // SQL query to get boarding house details with owner info
    $sql = "
        SELECT
            bh.*,
            r.first_name,
            r.middle_name,
            r.last_name,
            r.phone,
            r.email,
            r.role
        FROM boarding_houses AS bh
        LEFT JOIN registrations AS r ON bh.user_id = r.id
        WHERE bh.bh_id = ?
    ";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$bhId]);
    $boardingHouse = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$boardingHouse) {
        echo json_encode(array('success' => false, 'error' => 'Boarding house not found.'));
        exit();
    }

    // Debug: Log database and table info
    error_log("DEBUG: Connected to database: " . $dbname);
    error_log("DEBUG: Querying boarding_houses table for bh_id: " . $bhId);
    error_log("DEBUG: Found boarding house: " . $boardingHouse['bh_name']);
    error_log("DEBUG: bh_rules value: '" . $boardingHouse['bh_rules'] . "'");
    error_log("DEBUG: bh_rules is null: " . (is_null($boardingHouse['bh_rules']) ? 'true' : 'false'));
    error_log("DEBUG: bh_rules is empty: " . (empty($boardingHouse['bh_rules']) ? 'true' : 'false'));
    
    // Debug: Show all fields from the query
    error_log("DEBUG: All boarding house fields: " . json_encode($boardingHouse));
    
    // Debug: Check bh_rules specifically
    $bhRulesValue = $boardingHouse['bh_rules'];
    $finalBhRules = isset($bhRulesValue) && !empty($bhRulesValue) ? $bhRulesValue : 'No specific rules';
    error_log("DEBUG: Final bh_rules value being sent: '" . $finalBhRules . "'");

    // Fetch images for this boarding house
    $imagesSql = "
        SELECT image_path
        FROM boarding_house_images
        WHERE bh_id = ?
        ORDER BY image_id ASC
    ";
    $imagesStmt = $pdo->prepare($imagesSql);
    $imagesStmt->execute([$bhId]);
    $images = $imagesStmt->fetchAll(PDO::FETCH_COLUMN);

    // Format image URLs
    $formattedImages = array();
    foreach ($images as $imagePath) {
        if (!empty($imagePath)) {
            $formattedImages[] = 'https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/' . $imagePath;
        }
    }

    // If no images, add placeholder
    if (empty($formattedImages)) {
        $formattedImages[] = 'https://via.placeholder.com/400x300?text=No+Image+Available';
    }

    // Fetch room categories for this boarding house
    $roomsSql = "
        SELECT DISTINCT room_category
        FROM boarding_house_rooms
        WHERE bh_id = ?
        ORDER BY room_category ASC
    ";
    $roomsStmt = $pdo->prepare($roomsSql);
    $roomsStmt->execute([$bhId]);
    $roomCategories = $roomsStmt->fetchAll(PDO::FETCH_COLUMN);

    // Fetch detailed room information
    $roomDetailsSql = "
        SELECT
            bhr_id,
            room_category,
            room_name,
            price,
            capacity,
            room_description,
            total_rooms,
            created_at
        FROM boarding_house_rooms
        WHERE bh_id = ?
        ORDER BY room_category, price ASC
    ";
    $roomDetailsStmt = $pdo->prepare($roomDetailsSql);
    $roomDetailsStmt->execute([$bhId]);
    $roomDetails = $roomDetailsStmt->fetchAll(PDO::FETCH_ASSOC);

    // Calculate price range
    $priceRangeSql = "
        SELECT
            MIN(price) as min_price,
            MAX(price) as max_price
        FROM boarding_house_rooms
        WHERE bh_id = ?
    ";
    $priceRangeStmt = $pdo->prepare($priceRangeSql);
    $priceRangeStmt->execute([$bhId]);
    $priceRange = $priceRangeStmt->fetch(PDO::FETCH_ASSOC);

    // Format the response
    $response = array(
        'success' => true,
        'data' => array(
            'boarding_house' => array(
                'bh_id' => (int)$boardingHouse['bh_id'],
                'bh_name' => $boardingHouse['bh_name'],
                'bh_address' => $boardingHouse['bh_address'],
                'bh_description' => $boardingHouse['bh_description'],
                'bh_rules' => isset($boardingHouse['bh_rules']) && !empty($boardingHouse['bh_rules']) ? $boardingHouse['bh_rules'] : 'No specific rules',
                'number_of_bathroom' => (int)$boardingHouse['number_of_bathroom'],
                'area' => (float)$boardingHouse['area'],
                'build_year' => (int)$boardingHouse['build_year'],
                'status' => $boardingHouse['status'],
                'bh_created_at' => $boardingHouse['bh_created_at'],
                'images' => $formattedImages,
                'room_categories' => $roomCategories,
                'room_details' => $roomDetails,
                'min_price' => $priceRange['min_price'] ? (int)$priceRange['min_price'] : null,
                'max_price' => $priceRange['max_price'] ? (int)$priceRange['max_price'] : null,
                'owner' => array(
                    'first_name' => $boardingHouse['first_name'],
                    'middle_name' => $boardingHouse['middle_name'],
                    'last_name' => $boardingHouse['last_name'],
                    'phone' => $boardingHouse['phone'],
                    'email' => $boardingHouse['email'],
                    'role' => $boardingHouse['role']
                )
            )
        )
    );

    echo json_encode($response);

} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>
