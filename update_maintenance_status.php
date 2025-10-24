<?php
// Update Maintenance Status API - Updates maintenance request status and details
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
    $status = isset($input['status']) ? trim($input['status']) : '';
    $assigned_to = isset($input['assigned_to']) ? intval($input['assigned_to']) : null;
    $estimated_cost = isset($input['estimated_cost']) ? floatval($input['estimated_cost']) : null;
    $actual_cost = isset($input['actual_cost']) ? floatval($input['actual_cost']) : null;
    $work_started_date = isset($input['work_started_date']) ? trim($input['work_started_date']) : null;
    $work_completed_date = isset($input['work_completed_date']) ? trim($input['work_completed_date']) : null;
    $notes = isset($input['notes']) ? trim($input['notes']) : '';
    $updated_by = isset($input['updated_by']) ? intval($input['updated_by']) : 0;
    
    if ($maintenance_id <= 0) {
        throw new Exception("Invalid maintenance_id");
    }
    
    if (!in_array($status, ['Pending', 'In Progress', 'Completed', 'Cancelled', 'On Hold'])) {
        throw new Exception("Invalid status");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, get maintenance request details
        $stmt = $db->prepare("
            SELECT 
                mr.boarder_id,
                mr.owner_id,
                mr.room_id,
                mr.title,
                mr.maintenance_type,
                mr.priority,
                mr.status as current_status,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name
            FROM maintenance_requests mr
            JOIN users u ON mr.boarder_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON mr.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE mr.maintenance_id = ?
        ");
        $stmt->execute([$maintenance_id]);
        $maintenance = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$maintenance) {
            throw new Exception("Maintenance request not found");
        }
        
        // Build update query dynamically based on provided fields
        $update_fields = [];
        $update_values = [];
        
        if (!empty($status)) {
            $update_fields[] = "status = ?";
            $update_values[] = $status;
        }
        
        if ($assigned_to !== null) {
            $update_fields[] = "assigned_to = ?";
            $update_values[] = $assigned_to;
        }
        
        if ($estimated_cost !== null) {
            $update_fields[] = "estimated_cost = ?";
            $update_values[] = $estimated_cost;
        }
        
        if ($actual_cost !== null) {
            $update_fields[] = "actual_cost = ?";
            $update_values[] = $actual_cost;
        }
        
        if ($work_started_date) {
            $update_fields[] = "work_started_date = ?";
            $update_values[] = date('Y-m-d H:i:s', strtotime($work_started_date));
        }
        
        if ($work_completed_date) {
            $update_fields[] = "work_completed_date = ?";
            $update_values[] = date('Y-m-d H:i:s', strtotime($work_completed_date));
        }
        
        if (!empty($notes)) {
            $update_fields[] = "notes = CONCAT(COALESCE(notes, ''), '\n', ?)";
            $update_values[] = date('Y-m-d H:i:s') . ' - ' . $notes;
        }
        
        if (empty($update_fields)) {
            throw new Exception("No fields to update");
        }
        
        $update_fields[] = "updated_at = NOW()";
        $update_values[] = $maintenance_id;
        
        $sql = "UPDATE maintenance_requests SET " . implode(', ', $update_fields) . " WHERE maintenance_id = ?";
        $stmt = $db->prepare($sql);
        $stmt->execute($update_values);
        
        // Commit transaction
        $db->commit();
        
        // Send appropriate notifications based on status change
        $notification_result = null;
        
        if ($status === 'In Progress') {
            $notification_result = AutoNotifyMaintenance::maintenanceStarted($maintenance['boarder_id'], [
                'maintenance_id' => $maintenance_id,
                'title' => $maintenance['title'],
                'maintenance_type' => $maintenance['maintenance_type'],
                'room_name' => $maintenance['room_name']
            ]);
        } elseif ($status === 'Completed') {
            $notification_result = AutoNotifyMaintenance::maintenanceCompleted($maintenance['boarder_id'], [
                'maintenance_id' => $maintenance_id,
                'title' => $maintenance['title'],
                'maintenance_type' => $maintenance['maintenance_type'],
                'room_name' => $maintenance['room_name'],
                'actual_cost' => $actual_cost ? "P" . number_format($actual_cost, 2) : null
            ]);
        } elseif ($status === 'Cancelled') {
            $notification_result = AutoNotifyMaintenance::maintenanceCancelled($maintenance['boarder_id'], [
                'maintenance_id' => $maintenance_id,
                'title' => $maintenance['title'],
                'maintenance_type' => $maintenance['maintenance_type'],
                'room_name' => $maintenance['room_name'],
                'reason' => $notes
            ]);
        }
        
        $response = [
            'success' => true,
            'message' => 'Maintenance request updated successfully',
            'data' => [
                'maintenance_id' => $maintenance_id,
                'status' => $status,
                'assigned_to' => $assigned_to,
                'estimated_cost' => $estimated_cost ? "P" . number_format($estimated_cost, 2) : null,
                'actual_cost' => $actual_cost ? "P" . number_format($actual_cost, 2) : null,
                'work_started_date' => $work_started_date ? date('Y-m-d H:i:s', strtotime($work_started_date)) : null,
                'work_completed_date' => $work_completed_date ? date('Y-m-d H:i:s', strtotime($work_completed_date)) : null,
                'notes' => $notes,
                'boarder_name' => $maintenance['boarder_name'],
                'room_name' => $maintenance['room_name'],
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





