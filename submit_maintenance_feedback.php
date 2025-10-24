<?php
// Submit Maintenance Feedback API - Allows boarders to submit feedback for completed maintenance
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';
require_once 'auto_notify_maintenance.php';

$response = [];

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Get parameters
    $maintenance_id = isset($input['maintenance_id']) ? intval($input['maintenance_id']) : 0;
    $boarder_id = isset($input['boarder_id']) ? intval($input['boarder_id']) : 0;
    $rating = isset($input['rating']) ? intval($input['rating']) : 0;
    $comment = isset($input['comment']) ? trim($input['comment']) : '';
    $work_quality = isset($input['work_quality']) ? intval($input['work_quality']) : null;
    $timeliness = isset($input['timeliness']) ? intval($input['timeliness']) : null;
    $communication = isset($input['communication']) ? intval($input['communication']) : null;
    $cleanliness = isset($input['cleanliness']) ? intval($input['cleanliness']) : null;
    $would_recommend = isset($input['would_recommend']) ? boolval($input['would_recommend']) : null;
    
    // Validation
    if ($maintenance_id <= 0) {
        throw new Exception("Invalid maintenance_id");
    }
    
    if ($boarder_id <= 0) {
        throw new Exception("Invalid boarder_id");
    }
    
    if ($rating < 1 || $rating > 5) {
        throw new Exception("Rating must be between 1 and 5");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, verify the maintenance request exists and belongs to this boarder
        $stmt = $db->prepare("
            SELECT 
                mr.maintenance_id,
                mr.boarder_id,
                mr.owner_id,
                mr.status,
                mr.title,
                mr.maintenance_type,
                mr.room_id,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name
            FROM maintenance_requests mr
            JOIN users u ON mr.boarder_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON mr.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE mr.maintenance_id = ? AND mr.boarder_id = ?
        ");
        $stmt->execute([$maintenance_id, $boarder_id]);
        $maintenance = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$maintenance) {
            throw new Exception("Maintenance request not found or you don't have permission to provide feedback");
        }
        
        if ($maintenance['status'] !== 'Completed') {
            throw new Exception("Feedback can only be submitted for completed maintenance requests");
        }
        
        // Check if feedback already exists
        $stmt = $db->prepare("
            SELECT feedback_rating FROM maintenance_requests 
            WHERE maintenance_id = ? AND feedback_rating IS NOT NULL
        ");
        $stmt->execute([$maintenance_id]);
        $existing_feedback = $stmt->fetch();
        
        if ($existing_feedback) {
            throw new Exception("Feedback has already been submitted for this maintenance request");
        }
        
        // Update maintenance request with feedback
        $stmt = $db->prepare("
            UPDATE maintenance_requests 
            SET feedback_rating = ?,
                feedback_comment = ?,
                work_quality_rating = ?,
                timeliness_rating = ?,
                communication_rating = ?,
                cleanliness_rating = ?,
                would_recommend = ?,
                feedback_submitted_at = NOW(),
                updated_at = NOW()
            WHERE maintenance_id = ?
        ");
        $stmt->execute([
            $rating,
            $comment,
            $work_quality,
            $timeliness,
            $communication,
            $cleanliness,
            $would_recommend,
            $maintenance_id
        ]);
        
        // Commit transaction
        $db->commit();
        
        // Send notification to owner about feedback
        $notification_result = AutoNotifyMaintenance::maintenanceFeedbackReceived($maintenance['owner_id'], [
            'maintenance_id' => $maintenance_id,
            'boarder_name' => $maintenance['boarder_name'],
            'room_name' => $maintenance['room_name'],
            'title' => $maintenance['title'],
            'rating' => $rating,
            'comment' => $comment
        ]);
        
        $response = [
            'success' => true,
            'message' => 'Feedback submitted successfully',
            'data' => [
                'maintenance_id' => $maintenance_id,
                'boarder_name' => $maintenance['boarder_name'],
                'room_name' => $maintenance['room_name'],
                'boarding_house_name' => $maintenance['boarding_house_name'],
                'rating' => $rating,
                'comment' => $comment,
                'work_quality' => $work_quality,
                'timeliness' => $timeliness,
                'communication' => $communication,
                'cleanliness' => $cleanliness,
                'would_recommend' => $would_recommend,
                'feedback_submitted_at' => date('Y-m-d H:i:s'),
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





