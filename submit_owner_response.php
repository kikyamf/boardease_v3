<?php
// Submit Owner Response API - Allows owners to respond to reviews
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
    $review_id = isset($input['review_id']) ? intval($input['review_id']) : 0;
    $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
    $response_text = isset($input['response_text']) ? trim($input['response_text']) : '';
    
    // Validation
    if ($review_id <= 0) {
        throw new Exception("Invalid review_id");
    }
    
    if ($owner_id <= 0) {
        throw new Exception("Invalid owner_id");
    }
    
    if (empty($response_text)) {
        throw new Exception("Response text is required");
    }
    
    if (strlen($response_text) > 1000) {
        throw new Exception("Response text must be 1000 characters or less");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, verify the review exists and belongs to this owner
        $stmt = $db->prepare("
            SELECT 
                r.review_id,
                r.boarder_id,
                r.owner_id,
                r.title,
                r.overall_rating,
                r.owner_response,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                bh.bh_name as boarding_house_name
            FROM reviews r
            JOIN users u ON r.boarder_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            JOIN boarding_houses bh ON r.boarding_house_id = bh.bh_id
            WHERE r.review_id = ? AND r.owner_id = ?
        ");
        $stmt->execute([$review_id, $owner_id]);
        $review = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$review) {
            throw new Exception("Review not found or you don't have permission to respond to this review");
        }
        
        if (!empty($review['owner_response'])) {
            throw new Exception("You have already responded to this review");
        }
        
        // Update review with owner response
        $stmt = $db->prepare("
            UPDATE reviews 
            SET owner_response = ?,
                owner_response_date = NOW(),
                updated_at = NOW()
            WHERE review_id = ?
        ");
        $stmt->execute([$response_text, $review_id]);
        
        // Commit transaction
        $db->commit();
        
        // Send notification to boarder
        $notification_result = AutoNotifyAnnouncement::sendAnnouncement($review['boarder_id'], [
            'title' => 'Owner Responded to Your Review',
            'message' => "The owner of {$review['boarding_house_name']} has responded to your review",
            'type' => 'owner_response',
            'review_id' => $review_id,
            'boarding_house_name' => $review['boarding_house_name'],
            'response_text' => $response_text
        ]);
        
        $response = [
            'success' => true,
            'message' => 'Response submitted successfully',
            'data' => [
                'review_id' => $review_id,
                'boarder_name' => $review['boarder_name'],
                'boarding_house_name' => $review['boarding_house_name'],
                'response_text' => $response_text,
                'response_date' => date('Y-m-d H:i:s'),
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





