<?php
// Submit Review API - Allows boarders to submit reviews for boarding houses
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';
require_once 'auto_notify_announcement.php';

$response = [];

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Get parameters
    $boarder_id = isset($input['boarder_id']) ? intval($input['boarder_id']) : 0;
    $boarding_house_id = isset($input['boarding_house_id']) ? intval($input['boarding_house_id']) : 0;
    $booking_id = isset($input['booking_id']) ? intval($input['booking_id']) : 0;
    $overall_rating = isset($input['overall_rating']) ? intval($input['overall_rating']) : 0;
    $cleanliness_rating = isset($input['cleanliness_rating']) ? intval($input['cleanliness_rating']) : 0;
    $location_rating = isset($input['location_rating']) ? intval($input['location_rating']) : 0;
    $value_rating = isset($input['value_rating']) ? intval($input['value_rating']) : 0;
    $amenities_rating = isset($input['amenities_rating']) ? intval($input['amenities_rating']) : 0;
    $safety_rating = isset($input['safety_rating']) ? intval($input['safety_rating']) : 0;
    $management_rating = isset($input['management_rating']) ? intval($input['management_rating']) : 0;
    $title = isset($input['title']) ? trim($input['title']) : '';
    $review_text = isset($input['review_text']) ? trim($input['review_text']) : '';
    $images = isset($input['images']) ? $input['images'] : []; // Array of image URLs
    $would_recommend = isset($input['would_recommend']) ? boolval($input['would_recommend']) : null;
    $stay_duration = isset($input['stay_duration']) ? trim($input['stay_duration']) : '';
    $visit_type = isset($input['visit_type']) ? trim($input['visit_type']) : 'Business'; // Business, Leisure, Student
    
    // Validation
    if ($boarder_id <= 0) {
        throw new Exception("Invalid boarder_id");
    }
    
    if ($boarding_house_id <= 0) {
        throw new Exception("Invalid boarding_house_id");
    }
    
    if ($booking_id <= 0) {
        throw new Exception("Invalid booking_id");
    }
    
    if ($overall_rating < 1 || $overall_rating > 5) {
        throw new Exception("Overall rating must be between 1 and 5");
    }
    
    if (empty($title)) {
        throw new Exception("Review title is required");
    }
    
    if (empty($review_text)) {
        throw new Exception("Review text is required");
    }
    
    if (!in_array($visit_type, ['Business', 'Leisure', 'Student'])) {
        $visit_type = 'Student';
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, verify the boarder has completed a stay at this boarding house
        $stmt = $db->prepare("
            SELECT 
                b.booking_id,
                b.user_id,
                b.room_id,
                b.booking_status,
                b.check_out_date,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name,
                bh.user_id as owner_id,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                r.phone_number as boarder_phone
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE b.booking_id = ? AND b.user_id = ? AND bhr.bh_id = ? 
            AND b.booking_status = 'Completed' AND b.check_out_date IS NOT NULL
        ");
        $stmt->execute([$booking_id, $boarder_id, $boarding_house_id]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$booking) {
            throw new Exception("You can only review boarding houses where you have completed a stay");
        }
        
        // Check if review already exists for this booking
        $stmt = $db->prepare("
            SELECT review_id FROM reviews 
            WHERE booking_id = ? AND boarder_id = ?
        ");
        $stmt->execute([$booking_id, $boarder_id]);
        $existing_review = $stmt->fetch();
        
        if ($existing_review) {
            throw new Exception("You have already submitted a review for this stay");
        }
        
        // Insert review
        $stmt = $db->prepare("
            INSERT INTO reviews (
                boarder_id, boarding_house_id, booking_id, owner_id, room_id,
                overall_rating, cleanliness_rating, location_rating, value_rating,
                amenities_rating, safety_rating, management_rating,
                title, review_text, images, would_recommend, stay_duration,
                visit_type, status, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Published', NOW(), NOW())
        ");
        
        $images_json = !empty($images) ? json_encode($images) : null;
        
        $stmt->execute([
            $boarder_id,
            $boarding_house_id,
            $booking_id,
            $booking['owner_id'],
            $booking['room_id'],
            $overall_rating,
            $cleanliness_rating,
            $location_rating,
            $value_rating,
            $amenities_rating,
            $safety_rating,
            $management_rating,
            $title,
            $review_text,
            $images_json,
            $would_recommend,
            $stay_duration
        ]);
        
        $review_id = $db->lastInsertId();
        
        // Commit transaction
        $db->commit();
        
        // Send notification to owner
        $notification_result = AutoNotifyAnnouncement::sendAnnouncement($booking['owner_id'], [
            'title' => 'New Review Received',
            'message' => "You received a new {$overall_rating}-star review from {$booking['boarder_name']}",
            'type' => 'review_received',
            'review_id' => $review_id,
            'boarder_name' => $booking['boarder_name'],
            'rating' => $overall_rating,
            'boarding_house_name' => $booking['boarding_house_name']
        ]);
        
        $response = [
            'success' => true,
            'message' => 'Review submitted successfully',
            'data' => [
                'review_id' => $review_id,
                'boarder_name' => $booking['boarder_name'],
                'room_name' => $booking['room_name'],
                'boarding_house_name' => $booking['boarding_house_name'],
                'overall_rating' => $overall_rating,
                'title' => $title,
                'status' => 'Published',
                'created_at' => date('Y-m-d H:i:s'),
                'notification_sent' => $notification_result ? $notification_result['success'] : false
            ]
        ];
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $db->rollback();
        throw $e;
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>





