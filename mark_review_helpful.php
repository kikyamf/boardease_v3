<?php
// Mark Review Helpful API - Allows users to mark reviews as helpful
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';

$response = [];

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Get parameters
    $review_id = isset($input['review_id']) ? intval($input['review_id']) : 0;
    $user_id = isset($input['user_id']) ? intval($input['user_id']) : 0;
    $action = isset($input['action']) ? trim($input['action']) : ''; // 'helpful' or 'not_helpful'
    
    // Validation
    if ($review_id <= 0) {
        throw new Exception("Invalid review_id");
    }
    
    if ($user_id <= 0) {
        throw new Exception("Invalid user_id");
    }
    
    if (!in_array($action, ['helpful', 'not_helpful'])) {
        throw new Exception("Invalid action. Must be 'helpful' or 'not_helpful'");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, verify the review exists
        $stmt = $db->prepare("
            SELECT 
                r.review_id,
                r.boarder_id,
                r.helpful_count,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                bh.bh_name as boarding_house_name
            FROM reviews r
            JOIN users u ON r.boarder_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            JOIN boarding_houses bh ON r.boarding_house_id = bh.bh_id
            WHERE r.review_id = ?
        ");
        $stmt->execute([$review_id]);
        $review = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$review) {
            throw new Exception("Review not found");
        }
        
        // Check if user has already marked this review
        $stmt = $db->prepare("
            SELECT helpful_action FROM review_helpful 
            WHERE review_id = ? AND user_id = ?
        ");
        $stmt->execute([$review_id, $user_id]);
        $existing_action = $stmt->fetch();
        
        $new_helpful_count = intval($review['helpful_count']);
        
        if ($existing_action) {
            // User has already marked this review
            $current_action = $existing_action['helpful_action'];
            
            if ($current_action === $action) {
                // Same action - remove the mark
                $stmt = $db->prepare("
                    DELETE FROM review_helpful 
                    WHERE review_id = ? AND user_id = ?
                ");
                $stmt->execute([$review_id, $user_id]);
                
                if ($action === 'helpful') {
                    $new_helpful_count = max(0, $new_helpful_count - 1);
                }
                
                $message = "Helpful mark removed";
            } else {
                // Different action - update the mark
                $stmt = $db->prepare("
                    UPDATE review_helpful 
                    SET helpful_action = ?, updated_at = NOW()
                    WHERE review_id = ? AND user_id = ?
                ");
                $stmt->execute([$action, $review_id, $user_id]);
                
                if ($current_action === 'helpful' && $action === 'not_helpful') {
                    $new_helpful_count = max(0, $new_helpful_count - 1);
                } elseif ($current_action === 'not_helpful' && $action === 'helpful') {
                    $new_helpful_count = $new_helpful_count + 1;
                }
                
                $message = "Helpful mark updated";
            }
        } else {
            // User hasn't marked this review yet
            $stmt = $db->prepare("
                INSERT INTO review_helpful (review_id, user_id, helpful_action, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
            ");
            $stmt->execute([$review_id, $user_id, $action]);
            
            if ($action === 'helpful') {
                $new_helpful_count = $new_helpful_count + 1;
            }
            
            $message = "Review marked as " . ($action === 'helpful' ? 'helpful' : 'not helpful');
        }
        
        // Update the helpful count in the reviews table
        $stmt = $db->prepare("
            UPDATE reviews 
            SET helpful_count = ?, updated_at = NOW()
            WHERE review_id = ?
        ");
        $stmt->execute([$new_helpful_count, $review_id]);
        
        // Commit transaction
        $db->commit();
        
        $response = [
            'success' => true,
            'message' => $message,
            'data' => [
                'review_id' => $review_id,
                'boarder_name' => $review['boarder_name'],
                'boarding_house_name' => $review['boarding_house_name'],
                'action' => $action,
                'new_helpful_count' => $new_helpful_count,
                'user_action' => $existing_action ? ($existing_action['helpful_action'] === $action ? 'removed' : 'updated') : 'added'
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





