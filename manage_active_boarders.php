<?php
header("Content-Type: application/json");
include 'dbConfig.php';

$response = [];

// Get action from POST data
$action = isset($_POST['action']) ? $_POST['action'] : '';
$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$booking_id = isset($_POST['booking_id']) ? intval($_POST['booking_id']) : 0;
$active_boarder_id = isset($_POST['active_boarder_id']) ? intval($_POST['active_boarder_id']) : 0;

if ($user_id <= 0) {
    echo json_encode([
        "success" => false, 
        "error" => "Invalid user_id"
    ], JSON_UNESCAPED_SLASHES);
    exit;
}

try {
    switch ($action) {
        case 'add_active_boarder':
            // Add a boarder to active_boarders table when they start renting
            if ($booking_id <= 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Invalid booking_id"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            // Get booking details
            $sql = "SELECT 
                        b.user_id, b.room_id, b.start_date, b.end_date,
                        bhr.bh_id as boarding_house_id
                    FROM bookings b
                    JOIN room_units ru ON b.room_id = ru.room_id
                    JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                    JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                    WHERE b.booking_id = ? AND bh.user_id = ?";
            
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("ii", $booking_id, $user_id);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($result->num_rows === 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Booking not found or not authorized"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            $booking = $result->fetch_assoc();
            
            // Check if boarder is already active
            $check_sql = "SELECT active_boarder_id FROM active_boarders 
                         WHERE user_id = ? AND room_id = ? AND status = 'Active'";
            $check_stmt = $conn->prepare($check_sql);
            $check_stmt->bind_param("ii", $booking['user_id'], $booking['room_id']);
            $check_stmt->execute();
            $check_result = $check_stmt->get_result();
            
            if ($check_result->num_rows > 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Boarder is already active"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            // Add to active_boarders
            $insert_sql = "INSERT INTO active_boarders 
                          (user_id, room_id, boarding_house_id, start_date, end_date, status, created_at) 
                          VALUES (?, ?, ?, ?, ?, 'Active', NOW())";
            $insert_stmt = $conn->prepare($insert_sql);
            $insert_stmt->bind_param("iiiss", 
                $booking['user_id'], 
                $booking['room_id'], 
                $booking['boarding_house_id'],
                $booking['start_date'], 
                $booking['end_date']
            );
            
            if ($insert_stmt->execute()) {
                echo json_encode([
                    "success" => true, 
                    "message" => "Boarder added to active rentals",
                    "active_boarder_id" => $conn->insert_id
                ], JSON_UNESCAPED_SLASHES);
            } else {
                echo json_encode([
                    "success" => false, 
                    "error" => "Failed to add boarder to active rentals"
                ], JSON_UNESCAPED_SLASHES);
            }
            break;
            
        case 'remove_active_boarder':
            // Remove a boarder from active_boarders table when they complete their rental
            if ($active_boarder_id <= 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Invalid active_boarder_id"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            // Verify the active boarder belongs to this owner
            $verify_sql = "SELECT ab.active_boarder_id 
                          FROM active_boarders ab
                          JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
                          WHERE ab.active_boarder_id = ? AND bh.user_id = ?";
            $verify_stmt = $conn->prepare($verify_sql);
            $verify_stmt->bind_param("ii", $active_boarder_id, $user_id);
            $verify_stmt->execute();
            $verify_result = $verify_stmt->get_result();
            
            if ($verify_result->num_rows === 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Active boarder not found or not authorized"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            // Remove from active_boarders
            $delete_sql = "DELETE FROM active_boarders WHERE active_boarder_id = ?";
            $delete_stmt = $conn->prepare($delete_sql);
            $delete_stmt->bind_param("i", $active_boarder_id);
            
            if ($delete_stmt->execute()) {
                echo json_encode([
                    "success" => true, 
                    "message" => "Boarder removed from active rentals"
                ], JSON_UNESCAPED_SLASHES);
            } else {
                echo json_encode([
                    "success" => false, 
                    "error" => "Failed to remove boarder from active rentals"
                ], JSON_UNESCAPED_SLASHES);
            }
            break;
            
        case 'complete_rental':
            // Complete a rental: remove from active_boarders and update booking status
            if ($active_boarder_id <= 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Invalid active_boarder_id"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            // Get active boarder details
            $get_sql = "SELECT ab.user_id, ab.room_id, ab.boarding_house_id
                       FROM active_boarders ab
                       JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
                       WHERE ab.active_boarder_id = ? AND bh.user_id = ?";
            $get_stmt = $conn->prepare($get_sql);
            $get_stmt->bind_param("ii", $active_boarder_id, $user_id);
            $get_stmt->execute();
            $get_result = $get_stmt->get_result();
            
            if ($get_result->num_rows === 0) {
                echo json_encode([
                    "success" => false, 
                    "error" => "Active boarder not found or not authorized"
                ], JSON_UNESCAPED_SLASHES);
                exit;
            }
            
            $active_boarder = $get_result->fetch_assoc();
            
            // Start transaction
            $conn->begin_transaction();
            
            try {
                // Update booking status to Completed
                $update_booking_sql = "UPDATE bookings 
                                      SET booking_status = 'Completed', updated_at = NOW()
                                      WHERE user_id = ? AND room_id = ? AND booking_status = 'Confirmed'";
                $update_booking_stmt = $conn->prepare($update_booking_sql);
                $update_booking_stmt->bind_param("ii", $active_boarder['user_id'], $active_boarder['room_id']);
                $update_booking_stmt->execute();
                
                // Remove from active_boarders
                $delete_sql = "DELETE FROM active_boarders WHERE active_boarder_id = ?";
                $delete_stmt = $conn->prepare($delete_sql);
                $delete_stmt->bind_param("i", $active_boarder_id);
                $delete_stmt->execute();
                
                // Commit transaction
                $conn->commit();
                
                echo json_encode([
                    "success" => true, 
                    "message" => "Rental completed successfully"
                ], JSON_UNESCAPED_SLASHES);
                
            } catch (Exception $e) {
                // Rollback transaction
                $conn->rollback();
                throw $e;
            }
            break;
            
        default:
            echo json_encode([
                "success" => false, 
                "error" => "Invalid action. Use: add_active_boarder, remove_active_boarder, or complete_rental"
            ], JSON_UNESCAPED_SLASHES);
            break;
    }

} catch (Exception $e) {
    echo json_encode([
        "success" => false, 
        "error" => "Database error: " . $e->getMessage()
    ], JSON_UNESCAPED_SLASHES);
}
?>











