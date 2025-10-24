<?php
// Get Review Details API - Returns detailed information for a specific review
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';

$response = [];

try {
    // Get review_id from request
    $review_id = isset($_GET['review_id']) ? intval($_GET['review_id']) : 0;
    
    if ($review_id <= 0) {
        throw new Exception("Invalid review_id");
    }
    
    $db = getDB();
    
    // Get detailed review information
    $sql = "SELECT 
                r.review_id,
                r.boarder_id,
                r.boarding_house_id,
                r.booking_id,
                r.owner_id,
                r.room_id,
                r.overall_rating,
                r.cleanliness_rating,
                r.location_rating,
                r.value_rating,
                r.amenities_rating,
                r.safety_rating,
                r.management_rating,
                r.title,
                r.review_text,
                r.images,
                r.would_recommend,
                r.stay_duration,
                r.visit_type,
                r.status,
                r.helpful_count,
                r.report_count,
                r.owner_response,
                r.owner_response_date,
                r.created_at,
                r.updated_at,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                reg.email as boarder_email,
                reg.phone_number as boarder_phone,
                reg.profile_picture as boarder_profile_picture,
                reg.university,
                reg.student_id,
                reg.birth_date,
                reg.gender,
                ru.room_number as room_name,
                ru.room_description,
                ru.room_capacity,
                ru.room_amenities,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                bh.bh_contact as boarding_house_contact,
                bh.bh_description as boarding_house_description,
                CONCAT(owner_reg.f_name, ' ', owner_reg.l_name) as owner_name,
                owner_reg.phone_number as owner_phone,
                owner_reg.email as owner_email
            FROM reviews r
            JOIN users u ON r.boarder_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            JOIN room_units ru ON r.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            LEFT JOIN users owner_u ON r.owner_id = owner_u.user_id
            LEFT JOIN registrations owner_reg ON owner_u.reg_id = owner_reg.reg_id
            WHERE r.review_id = ?";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([$review_id]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        throw new Exception("Review not found");
    }
    
    // Parse images
    $images = [];
    if ($row['images']) {
        $images = json_decode($row['images'], true) ?: [];
    }
    
    // Format dates
    $created_at = date('Y-m-d H:i:s', strtotime($row['created_at']));
    $updated_at = date('Y-m-d H:i:s', strtotime($row['updated_at']));
    $owner_response_date = $row['owner_response_date'] ? date('Y-m-d H:i:s', strtotime($row['owner_response_date'])) : null;
    $birth_date = $row['birth_date'] ? date('Y-m-d', strtotime($row['birth_date'])) : null;
    
    // Calculate time since review
    $created = new DateTime($row['created_at']);
    $today = new DateTime();
    $days_since_review = $created->diff($today)->days;
    
    // Format time display
    $time_display = '';
    if ($days_since_review == 0) {
        $time_display = 'Today';
    } elseif ($days_since_review == 1) {
        $time_display = 'Yesterday';
    } elseif ($days_since_review < 7) {
        $time_display = $days_since_review . ' days ago';
    } elseif ($days_since_review < 30) {
        $weeks = floor($days_since_review / 7);
        $time_display = $weeks . ' week' . ($weeks > 1 ? 's' : '') . ' ago';
    } elseif ($days_since_review < 365) {
        $months = floor($days_since_review / 30);
        $time_display = $months . ' month' . ($months > 1 ? 's' : '') . ' ago';
    } else {
        $years = floor($days_since_review / 365);
        $time_display = $years . ' year' . ($years > 1 ? 's' : '') . ' ago';
    }
    
    // Calculate age
    $age = null;
    if ($birth_date) {
        $birth = new DateTime($birth_date);
        $age = $birth->diff($today)->y;
    }
    
    // Calculate average rating
    $ratings = [
        $row['cleanliness_rating'],
        $row['location_rating'],
        $row['value_rating'],
        $row['amenities_rating'],
        $row['safety_rating'],
        $row['management_rating']
    ];
    $average_rating = array_sum($ratings) / count($ratings);
    
    // Determine review sentiment
    $sentiment = 'neutral';
    if ($row['overall_rating'] >= 4) {
        $sentiment = 'positive';
    } elseif ($row['overall_rating'] <= 2) {
        $sentiment = 'negative';
    }
    
    $review_details = [
        'review_id' => intval($row['review_id']),
        'boarder_id' => intval($row['boarder_id']),
        'boarding_house_id' => intval($row['boarding_house_id']),
        'booking_id' => intval($row['booking_id']),
        'owner_id' => intval($row['owner_id']),
        'room_id' => intval($row['room_id']),
        'overall_rating' => intval($row['overall_rating']),
        'cleanliness_rating' => intval($row['cleanliness_rating']),
        'location_rating' => intval($row['location_rating']),
        'value_rating' => intval($row['value_rating']),
        'amenities_rating' => intval($row['amenities_rating']),
        'safety_rating' => intval($row['safety_rating']),
        'management_rating' => intval($row['management_rating']),
        'average_rating' => round($average_rating, 1),
        'title' => $row['title'],
        'review_text' => $row['review_text'],
        'images' => $images,
        'would_recommend' => $row['would_recommend'] ? true : false,
        'stay_duration' => $row['stay_duration'] ?? '',
        'visit_type' => $row['visit_type'] ?? 'Student',
        'status' => $row['status'],
        'helpful_count' => intval($row['helpful_count']),
        'report_count' => intval($row['report_count']),
        'owner_response' => $row['owner_response'] ?? '',
        'owner_response_date' => $owner_response_date,
        'created_at' => $created_at,
        'updated_at' => $updated_at,
        'boarder_name' => $row['boarder_name'],
        'boarder_email' => $row['boarder_email'] ?? '',
        'boarder_phone' => $row['boarder_phone'] ?? '',
        'boarder_profile_picture' => $row['boarder_profile_picture'] ?? '',
        'university' => $row['university'] ?? '',
        'student_id' => $row['student_id'] ?? '',
        'birth_date' => $birth_date,
        'age' => $age,
        'gender' => $row['gender'] ?? '',
        'room_name' => $row['room_name'],
        'room_description' => $row['room_description'] ?? '',
        'room_capacity' => intval($row['room_capacity']),
        'room_amenities' => $row['room_amenities'] ?? '',
        'boarding_house_name' => $row['boarding_house_name'],
        'boarding_house_address' => $row['boarding_house_address'],
        'boarding_house_contact' => $row['boarding_house_contact'] ?? '',
        'boarding_house_description' => $row['boarding_house_description'] ?? '',
        'owner_name' => $row['owner_name'] ?? '',
        'owner_phone' => $row['owner_phone'] ?? '',
        'owner_email' => $row['owner_email'] ?? '',
        'review_info' => [
            'days_since_review' => $days_since_review,
            'time_display' => $time_display,
            'sentiment' => $sentiment
        ]
    ];
    
    $response = [
        'success' => true,
        'data' => [
            'review_details' => $review_details
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>





