<?php
// Get Reviews API - Returns reviews for boarding houses
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
    // Get parameters from request
    $boarding_house_id = isset($_GET['boarding_house_id']) ? intval($_GET['boarding_house_id']) : 0;
    $owner_id = isset($_GET['owner_id']) ? intval($_GET['owner_id']) : 0;
    $boarder_id = isset($_GET['boarder_id']) ? intval($_GET['boarder_id']) : 0;
    $rating_filter = isset($_GET['rating']) ? $_GET['rating'] : 'all'; // 'all', '5', '4', '3', '2', '1'
    $status_filter = isset($_GET['status']) ? $_GET['status'] : 'published'; // 'all', 'published', 'pending', 'rejected'
    $sort_by = isset($_GET['sort_by']) ? $_GET['sort_by'] : 'newest'; // 'newest', 'oldest', 'highest_rating', 'lowest_rating', 'most_helpful'
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 20;
    $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;
    
    if ($boarding_house_id <= 0 && $owner_id <= 0 && $boarder_id <= 0) {
        throw new Exception("At least one of boarding_house_id, owner_id, or boarder_id must be provided");
    }
    
    $db = getDB();
    
    // Build rating filter condition
    $rating_condition = "";
    if ($rating_filter !== 'all') {
        $rating_condition = "AND r.overall_rating = " . intval($rating_filter);
    }
    
    // Build status filter condition
    $status_condition = "";
    if ($status_filter !== 'all') {
        $status_condition = "AND r.status = '" . ucfirst($status_filter) . "'";
    }
    
    // Build sort order
    $order_by = "";
    switch ($sort_by) {
        case 'oldest':
            $order_by = "ORDER BY r.created_at ASC";
            break;
        case 'highest_rating':
            $order_by = "ORDER BY r.overall_rating DESC, r.created_at DESC";
            break;
        case 'lowest_rating':
            $order_by = "ORDER BY r.overall_rating ASC, r.created_at DESC";
            break;
        case 'most_helpful':
            $order_by = "ORDER BY r.helpful_count DESC, r.created_at DESC";
            break;
        case 'newest':
        default:
            $order_by = "ORDER BY r.created_at DESC";
            break;
    }
    
    // Build WHERE condition based on provided parameters
    $where_conditions = [];
    $params = [];
    
    if ($boarding_house_id > 0) {
        $where_conditions[] = "r.boarding_house_id = ?";
        $params[] = $boarding_house_id;
    }
    
    if ($owner_id > 0) {
        $where_conditions[] = "r.owner_id = ?";
        $params[] = $owner_id;
    }
    
    if ($boarder_id > 0) {
        $where_conditions[] = "r.boarder_id = ?";
        $params[] = $boarder_id;
    }
    
    $where_clause = "WHERE " . implode(" AND ", $where_conditions) . " $rating_condition $status_condition";
    
    // Get reviews
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
                reg.profile_picture as boarder_profile_picture,
                reg.university,
                reg.student_id,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                CONCAT(owner_reg.f_name, ' ', owner_reg.l_name) as owner_name
            FROM reviews r
            JOIN users u ON r.boarder_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            JOIN room_units ru ON r.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            LEFT JOIN users owner_u ON r.owner_id = owner_u.user_id
            LEFT JOIN registrations owner_reg ON owner_u.reg_id = owner_reg.reg_id
            $where_clause
            $order_by
            LIMIT ? OFFSET ?";
    
    $params[] = $limit;
    $params[] = $offset;
    
    $stmt = $db->prepare($sql);
    $stmt->execute($params);
    
    $reviews = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Parse images
        $images = [];
        if ($row['images']) {
            $images = json_decode($row['images'], true) ?: [];
        }
        
        // Format dates
        $created_at = date('Y-m-d H:i:s', strtotime($row['created_at']));
        $updated_at = date('Y-m-d H:i:s', strtotime($row['updated_at']));
        $owner_response_date = $row['owner_response_date'] ? date('Y-m-d H:i:s', strtotime($row['owner_response_date'])) : null;
        
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
        
        $reviews[] = [
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
            'boarder_profile_picture' => $row['boarder_profile_picture'] ?? '',
            'university' => $row['university'] ?? '',
            'student_id' => $row['student_id'] ?? '',
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'owner_name' => $row['owner_name'] ?? '',
            'review_info' => [
                'days_since_review' => $days_since_review,
                'time_display' => $time_display
            ]
        ];
    }
    
    // Get total count for pagination
    $count_sql = "SELECT COUNT(*) as total_count FROM reviews r $where_clause";
    $count_params = array_slice($params, 0, -2); // Remove limit and offset
    $count_stmt = $db->prepare($count_sql);
    $count_stmt->execute($count_params);
    $total_count = $count_stmt->fetch(PDO::FETCH_ASSOC)['total_count'];
    
    $response = [
        'success' => true,
        'data' => [
            'reviews' => $reviews,
            'total_count' => intval($total_count),
            'limit' => $limit,
            'offset' => $offset,
            'has_more' => ($offset + $limit) < $total_count,
            'filters' => [
                'rating' => $rating_filter,
                'status' => $status_filter,
                'sort_by' => $sort_by
            ]
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





