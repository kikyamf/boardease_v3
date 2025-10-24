<?php
// Get Reviews Summary API - Returns review statistics and summary for boarding houses
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
    $period = isset($_GET['period']) ? $_GET['period'] : 'all'; // 'all', 'month', 'year'
    
    if ($boarding_house_id <= 0 && $owner_id <= 0) {
        throw new Exception("Either boarding_house_id or owner_id must be provided");
    }
    
    $db = getDB();
    
    // Build date filter condition
    $date_condition = "";
    switch ($period) {
        case 'month':
            $date_condition = "AND r.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            break;
        case 'year':
            $date_condition = "AND r.created_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)";
            break;
    }
    
    // Build WHERE condition
    $where_condition = "";
    $params = [];
    
    if ($boarding_house_id > 0) {
        $where_condition = "WHERE r.boarding_house_id = ? $date_condition";
        $params[] = $boarding_house_id;
    } else {
        $where_condition = "WHERE r.owner_id = ? $date_condition";
        $params[] = $owner_id;
    }
    
    // Get reviews summary
    $sql = "SELECT 
                COUNT(*) as total_reviews,
                SUM(CASE WHEN r.status = 'Published' THEN 1 ELSE 0 END) as published_reviews,
                SUM(CASE WHEN r.status = 'Pending' THEN 1 ELSE 0 END) as pending_reviews,
                SUM(CASE WHEN r.status = 'Rejected' THEN 1 ELSE 0 END) as rejected_reviews,
                SUM(CASE WHEN r.overall_rating = 5 THEN 1 ELSE 0 END) as five_star_count,
                SUM(CASE WHEN r.overall_rating = 4 THEN 1 ELSE 0 END) as four_star_count,
                SUM(CASE WHEN r.overall_rating = 3 THEN 1 ELSE 0 END) as three_star_count,
                SUM(CASE WHEN r.overall_rating = 2 THEN 1 ELSE 0 END) as two_star_count,
                SUM(CASE WHEN r.overall_rating = 1 THEN 1 ELSE 0 END) as one_star_count,
                AVG(r.overall_rating) as average_overall_rating,
                AVG(r.cleanliness_rating) as average_cleanliness_rating,
                AVG(r.location_rating) as average_location_rating,
                AVG(r.value_rating) as average_value_rating,
                AVG(r.amenities_rating) as average_amenities_rating,
                AVG(r.safety_rating) as average_safety_rating,
                AVG(r.management_rating) as average_management_rating,
                SUM(CASE WHEN r.would_recommend = 1 THEN 1 ELSE 0 END) as recommend_count,
                SUM(r.helpful_count) as total_helpful_votes,
                SUM(r.report_count) as total_reports,
                SUM(CASE WHEN r.owner_response IS NOT NULL AND r.owner_response != '' THEN 1 ELSE 0 END) as responses_count,
                MIN(r.created_at) as first_review_date,
                MAX(r.created_at) as last_review_date
            FROM reviews r
            $where_condition";
    
    $stmt = $db->prepare($sql);
    $stmt->execute($params);
    $summary = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Calculate additional metrics
    $total_reviews = intval($summary['total_reviews']);
    $published_reviews = intval($summary['published_reviews']);
    $pending_reviews = intval($summary['pending_reviews']);
    $rejected_reviews = intval($summary['rejected_reviews']);
    
    $five_star_count = intval($summary['five_star_count']);
    $four_star_count = intval($summary['four_star_count']);
    $three_star_count = intval($summary['three_star_count']);
    $two_star_count = intval($summary['two_star_count']);
    $one_star_count = intval($summary['one_star_count']);
    
    $average_overall_rating = floatval($summary['average_overall_rating']);
    $average_cleanliness_rating = floatval($summary['average_cleanliness_rating']);
    $average_location_rating = floatval($summary['average_location_rating']);
    $average_value_rating = floatval($summary['average_value_rating']);
    $average_amenities_rating = floatval($summary['average_amenities_rating']);
    $average_safety_rating = floatval($summary['average_safety_rating']);
    $average_management_rating = floatval($summary['average_management_rating']);
    
    $recommend_count = intval($summary['recommend_count']);
    $total_helpful_votes = intval($summary['total_helpful_votes']);
    $total_reports = intval($summary['total_reports']);
    $responses_count = intval($summary['responses_count']);
    
    // Calculate percentages
    $recommendation_rate = $total_reviews > 0 ? ($recommend_count / $total_reviews) * 100 : 0;
    $response_rate = $total_reviews > 0 ? ($responses_count / $total_reviews) * 100 : 0;
    
    // Calculate rating distribution percentages
    $rating_distribution = [
        '5_star' => $total_reviews > 0 ? ($five_star_count / $total_reviews) * 100 : 0,
        '4_star' => $total_reviews > 0 ? ($four_star_count / $total_reviews) * 100 : 0,
        '3_star' => $total_reviews > 0 ? ($three_star_count / $total_reviews) * 100 : 0,
        '2_star' => $total_reviews > 0 ? ($two_star_count / $total_reviews) * 100 : 0,
        '1_star' => $total_reviews > 0 ? ($one_star_count / $total_reviews) * 100 : 0
    ];
    
    // Get recent reviews (last 5)
    $recent_sql = "SELECT 
                    r.review_id,
                    r.overall_rating,
                    r.title,
                    r.created_at,
                    CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                    reg.profile_picture as boarder_profile_picture,
                    bh.bh_name as boarding_house_name
                FROM reviews r
                JOIN users u ON r.boarder_id = u.user_id
                JOIN registrations reg ON u.reg_id = reg.reg_id
                JOIN boarding_houses bh ON r.boarding_house_id = bh.bh_id
                $where_condition
                ORDER BY r.created_at DESC
                LIMIT 5";
    
    $recent_stmt = $db->prepare($recent_sql);
    $recent_stmt->execute($params);
    
    $recent_reviews = [];
    while ($row = $recent_stmt->fetch(PDO::FETCH_ASSOC)) {
        $recent_reviews[] = [
            'review_id' => intval($row['review_id']),
            'overall_rating' => intval($row['overall_rating']),
            'title' => $row['title'],
            'created_at' => date('Y-m-d H:i:s', strtotime($row['created_at'])),
            'boarder_name' => $row['boarder_name'],
            'boarder_profile_picture' => $row['boarder_profile_picture'] ?? '',
            'boarding_house_name' => $row['boarding_house_name']
        ];
    }
    
    // Get boarding house information
    $bh_sql = "SELECT 
                    bh.bh_id,
                    bh.bh_name,
                    bh.bh_address,
                    bh.bh_description
                FROM boarding_houses bh
                WHERE bh.bh_id = ? OR bh.user_id = ?";
    
    $bh_stmt = $db->prepare($bh_sql);
    $bh_stmt->execute([$boarding_house_id, $owner_id]);
    $boarding_house = $bh_stmt->fetch(PDO::FETCH_ASSOC);
    
    $response = [
        'success' => true,
        'data' => [
            'summary' => [
                'total_reviews' => $total_reviews,
                'published_reviews' => $published_reviews,
                'pending_reviews' => $pending_reviews,
                'rejected_reviews' => $rejected_reviews,
                'five_star_count' => $five_star_count,
                'four_star_count' => $four_star_count,
                'three_star_count' => $three_star_count,
                'two_star_count' => $two_star_count,
                'one_star_count' => $one_star_count,
                'average_overall_rating' => round($average_overall_rating, 2),
                'average_cleanliness_rating' => round($average_cleanliness_rating, 2),
                'average_location_rating' => round($average_location_rating, 2),
                'average_value_rating' => round($average_value_rating, 2),
                'average_amenities_rating' => round($average_amenities_rating, 2),
                'average_safety_rating' => round($average_safety_rating, 2),
                'average_management_rating' => round($average_management_rating, 2),
                'recommend_count' => $recommend_count,
                'recommendation_rate' => round($recommendation_rate, 2),
                'total_helpful_votes' => $total_helpful_votes,
                'total_reports' => $total_reports,
                'responses_count' => $responses_count,
                'response_rate' => round($response_rate, 2),
                'first_review_date' => $summary['first_review_date'] ? date('Y-m-d', strtotime($summary['first_review_date'])) : null,
                'last_review_date' => $summary['last_review_date'] ? date('Y-m-d', strtotime($summary['last_review_date'])) : null
            ],
            'rating_distribution' => [
                '5_star' => round($rating_distribution['5_star'], 2),
                '4_star' => round($rating_distribution['4_star'], 2),
                '3_star' => round($rating_distribution['3_star'], 2),
                '2_star' => round($rating_distribution['2_star'], 2),
                '1_star' => round($rating_distribution['1_star'], 2)
            ],
            'recent_reviews' => $recent_reviews,
            'boarding_house' => $boarding_house ? [
                'bh_id' => intval($boarding_house['bh_id']),
                'bh_name' => $boarding_house['bh_name'],
                'bh_address' => $boarding_house['bh_address'],
                'bh_description' => $boarding_house['bh_description'] ?? ''
            ] : null,
            'period' => $period
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





