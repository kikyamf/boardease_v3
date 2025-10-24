<?php
header("Content-Type: application/json");
include 'dbConfig.php';

$response = [];

// --- Validate user_id ---
$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

// Debug info
$debug_info = [
    "raw_user_id" => $_POST['user_id'] ?? "not_set",
    "parsed_user_id" => $user_id,
    "post_data" => $_POST,
    "request_method" => $_SERVER['REQUEST_METHOD'] ?? "unknown",
    "content_type" => $_SERVER['CONTENT_TYPE'] ?? "unknown",
    "raw_input" => file_get_contents('php://input')
];

if ($user_id <= 0) {
    echo json_encode([
        "success" => false, 
        "error" => "Invalid user_id", 
        "debug_info" => $debug_info
    ], JSON_UNESCAPED_SLASHES);
    exit;
}

try {
    // Get current active boarders from active_boarders table
    // These are boarders who are currently renting
    $sql = "SELECT 
                ab.active_boarder_id as boarder_id,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                reg.email as boarder_email,
                reg.phone_number as boarder_phone,
                bh.bh_name as boarding_house_name,
                ru.room_number,
                bhr.room_category as rent_type,
                ab.start_date,
                ab.end_date,
                ab.status,
                u.profile_picture
            FROM active_boarders ab
            JOIN room_units ru ON ab.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            JOIN users u ON ab.user_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            WHERE bh.user_id = ? 
            AND ab.status = 'Active'
            ORDER BY ab.start_date DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $boarders = [];
    while ($row = $result->fetch_assoc()) {
        $boarders[] = [
            "boarder_id" => $row["boarder_id"],
            "boarder_name" => $row["boarder_name"],
            "boarder_email" => $row["boarder_email"],
            "boarder_phone" => $row["boarder_phone"],
            "boarding_house_name" => $row["boarding_house_name"],
            "room_number" => $row["room_number"],
            "rent_type" => $row["rent_type"],
            "start_date" => $row["start_date"],
            "end_date" => $row["end_date"],
            "status" => $row["status"],
            "profile_picture" => $row["profile_picture"]
        ];
    }

    echo json_encode([
        "success" => true,
        "boarders" => $boarders,
        "total_count" => count($boarders)
    ], JSON_UNESCAPED_SLASHES);

} catch (Exception $e) {
    echo json_encode([
        "success" => false, 
        "error" => "Database error: " . $e->getMessage()
    ], JSON_UNESCAPED_SLASHES);
}
?>