<?php
// Submit Maintenance Request API - Allows boarders to submit maintenance requests
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
    $boarder_id = isset($input['boarder_id']) ? intval($input['boarder_id']) : 0;
    $room_id = isset($input['room_id']) ? intval($input['room_id']) : 0;
    $maintenance_type = isset($input['maintenance_type']) ? trim($input['maintenance_type']) : '';
    $priority = isset($input['priority']) ? trim($input['priority']) : 'Medium';
    $title = isset($input['title']) ? trim($input['title']) : '';
    $description = isset($input['description']) ? trim($input['description']) : '';
    $location = isset($input['location']) ? trim($input['location']) : '';
    $images = isset($input['images']) ? $input['images'] : []; // Array of image URLs
    $preferred_date = isset($input['preferred_date']) ? trim($input['preferred_date']) : null;
    $preferred_time = isset($input['preferred_time']) ? trim($input['preferred_time']) : null;
    $contact_phone = isset($input['contact_phone']) ? trim($input['contact_phone']) : '';
    
    // Validation
    if ($boarder_id <= 0) {
        throw new Exception("Invalid boarder_id");
    }
    
    if ($room_id <= 0) {
        throw new Exception("Invalid room_id");
    }
    
    if (empty($maintenance_type)) {
        throw new Exception("Maintenance type is required");
    }
    
    if (empty($title)) {
        throw new Exception("Title is required");
    }
    
    if (empty($description)) {
        throw new Exception("Description is required");
    }
    
    if (!in_array($priority, ['Low', 'Medium', 'High', 'Urgent'])) {
        $priority = 'Medium';
    }
    
    if (!in_array($maintenance_type, ['Plumbing', 'Electrical', 'HVAC', 'Furniture', 'Appliance', 'Security', 'Cleaning', 'Other'])) {
        throw new Exception("Invalid maintenance type");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, verify the boarder has access to this room
        $stmt = $db->prepare("
            SELECT 
                b.booking_id,
                b.user_id,
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
            WHERE b.user_id = ? AND b.room_id = ? AND b.booking_status = 'Approved' AND b.end_date > NOW()
        ");
        $stmt->execute([$boarder_id, $room_id]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$booking) {
            throw new Exception("You don't have access to this room or your booking is not active");
        }
        
        // Insert maintenance request
        $stmt = $db->prepare("
            INSERT INTO maintenance_requests (
                boarder_id, room_id, booking_id, owner_id, maintenance_type, priority, 
                title, description, location, images, preferred_date, preferred_time, 
                contact_phone, status, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending', NOW(), NOW())
        ");
        
        $images_json = !empty($images) ? json_encode($images) : null;
        $preferred_date_formatted = $preferred_date ? date('Y-m-d', strtotime($preferred_date)) : null;
        
        $stmt->execute([
            $boarder_id,
            $room_id,
            $booking['booking_id'],
            $booking['owner_id'],
            $maintenance_type,
            $priority,
            $title,
            $description,
            $location,
            $images_json,
            $preferred_date_formatted,
            $preferred_time,
            $contact_phone
        ]);
        
        $maintenance_id = $db->lastInsertId();
        
        // Commit transaction
        $db->commit();
        
        // Send notification to owner
        $notification_result = AutoNotifyMaintenance::maintenanceRequestSubmitted($booking['owner_id'], [
            'maintenance_id' => $maintenance_id,
            'boarder_name' => $booking['boarder_name'],
            'room_name' => $booking['room_name'],
            'maintenance_type' => $maintenance_type,
            'priority' => $priority,
            'title' => $title
        ]);
        
        $response = [
            'success' => true,
            'message' => 'Maintenance request submitted successfully',
            'data' => [
                'maintenance_id' => $maintenance_id,
                'boarder_name' => $booking['boarder_name'],
                'room_name' => $booking['room_name'],
                'boarding_house_name' => $booking['boarding_house_name'],
                'maintenance_type' => $maintenance_type,
                'priority' => $priority,
                'title' => $title,
                'status' => 'Pending',
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





