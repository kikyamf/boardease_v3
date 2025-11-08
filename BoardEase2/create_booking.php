<?php
// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Start transaction to ensure all operations succeed or fail together
    $pdo->beginTransaction();
    
    // Get POST data
    $roomId = isset($_POST['room_id']) ? intval($_POST['room_id']) : 0;
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $startDate = isset($_POST['start_date']) ? trim($_POST['start_date']) : '';
    $endDate = isset($_POST['end_date']) ? trim($_POST['end_date']) : '';
    $paymentMethod = isset($_POST['payment_method']) ? trim($_POST['payment_method']) : 'Cash';
    $paymentProofBase64 = isset($_POST['payment_proof']) ? trim($_POST['payment_proof']) : '';
    
    // Get calculated payment amount and number of days from Android
    // If not provided, calculate it from room price (fallback for backward compatibility)
    $totalAmount = isset($_POST['total_amount']) ? floatval($_POST['total_amount']) : 0;
    $numberOfDays = isset($_POST['number_of_days']) ? intval($_POST['number_of_days']) : 0;
    
    // Debug logging
    error_log("create_booking.php - Received data: room_id=$roomId, user_id=$userId, start_date=$startDate, end_date=$endDate, total_amount=$totalAmount, number_of_days=$numberOfDays");
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        echo json_encode(array(
            'success' => false,
            'message' => 'Missing required fields',
            'debug' => array(
                'room_id' => $roomId,
                'user_id' => $userId,
                'start_date' => $startDate,
                'end_date' => $endDate
            )
        ));
        exit;
    }
    
    // Validate date format
    $startDateObj = DateTime::createFromFormat('Y-m-d', $startDate);
    $endDateObj = DateTime::createFromFormat('Y-m-d', $endDate);
    
    if (!$startDateObj || !$endDateObj) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        echo json_encode(array(
            'success' => false,
            'message' => 'Invalid date format. Expected YYYY-MM-DD'
        ));
        exit;
    }
    
    // Validate end date is after start date
    if ($endDateObj <= $startDateObj) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        echo json_encode(array(
            'success' => false,
            'message' => 'End date must be after start date'
        ));
        exit;
    }
    
    // Check if room unit exists in room_units table
    // Use SELECT FOR UPDATE to lock the row and prevent concurrent bookings
    // The room_id from Android is now room_units.room_id (selected by user)
    error_log("=== BOOKING PROCESS START ===");
    error_log("Step 1: Checking room unit with room_id: $roomId");
    $checkRoomUnitSql = "SELECT room_id, bhr_id, room_number, status FROM room_units WHERE room_id = :room_id FOR UPDATE";
    $checkRoomUnitStmt = $pdo->prepare($checkRoomUnitSql);
    $checkRoomUnitStmt->execute([':room_id' => $roomId]);
    $roomUnit = $checkRoomUnitStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomUnit) {
        error_log("ERROR: Room unit not found for room_id: $roomId");
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
            error_log("Transaction rolled back - Room unit not found");
        }
        echo json_encode(array(
            'success' => false,
            'message' => 'Room unit not found'
        ));
        exit;
    }
    
    error_log("Step 1 Result: Room unit found - room_id: " . $roomUnit['room_id'] . ", bhr_id: " . $roomUnit['bhr_id'] . ", status: " . $roomUnit['status']);
    
    // Check if room unit is available (check again within transaction with lock)
    if (isset($roomUnit['status']) && $roomUnit['status'] !== 'Available') {
        error_log("ERROR: Room unit status is NOT Available. Current status: " . $roomUnit['status']);
        error_log("ERROR: Exiting BEFORE creating booking - no booking should be created");
        
        // CRITICAL: Rollback transaction BEFORE exiting
        try {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
                error_log("Transaction rolled back successfully - Room not available");
            } else {
                error_log("WARNING: No active transaction to rollback (this is OK if we haven't done any writes yet)");
            }
        } catch (PDOException $rollbackError) {
            error_log("ERROR: Failed to rollback transaction: " . $rollbackError->getMessage());
        }
        
        // Return error response
        $errorResponse = json_encode(array(
            'success' => false,
            'message' => 'Selected room unit is not available. Status: ' . $roomUnit['status']
        ));
        error_log("ERROR: Returning error response: " . $errorResponse);
        error_log("ERROR: EXITING - No booking should be created after this point");
        echo $errorResponse;
        exit; // CRITICAL: Exit immediately to prevent any further code execution
    }
    
    error_log("Step 1 Success: Room unit is Available, proceeding with booking");
    
    // Use room_units.room_id directly (this is what the user selected)
    $actualRoomId = $roomId;
    
    // Check if user exists - userId from Android is users.user_id (from login.php)
    // First try to find by users.user_id (most common case since login.php returns this)
    $checkUserByUserIdSql = "SELECT r.id as reg_id, u.user_id 
                             FROM users u 
                             JOIN registrations r ON u.reg_id = r.id 
                             WHERE u.user_id = :user_id";
    $checkUserByUserIdStmt = $pdo->prepare($checkUserByUserIdSql);
    $checkUserByUserIdStmt->execute([':user_id' => $userId]);
    $user = $checkUserByUserIdStmt->fetch(PDO::FETCH_ASSOC);
    
    // If not found by users.user_id, try to find by registrations.id (fallback)
    if (!$user) {
        $checkUserSql = "SELECT r.id as reg_id, u.user_id 
                         FROM registrations r 
                         LEFT JOIN users u ON r.id = u.reg_id 
                         WHERE r.id = :user_id";
        $checkUserStmt = $pdo->prepare($checkUserSql);
        $checkUserStmt->execute([':user_id' => $userId]);
        $user = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    }
    
    if (!$user) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("User not found - searched userId: " . $userId);
        echo json_encode(array(
            'success' => false,
            'message' => 'User not found'
        ));
        exit;
    }
    
    // Get the actual user_id from users table (needed for bookings foreign key)
    $actualUserId = $user['user_id'];
    $regId = $user['reg_id'];
    
    // If user doesn't have a corresponding entry in users table, create one
    if (!$actualUserId) {
        // Insert into users table (without status column if it doesn't exist)
        try {
            $insertUserSql = "INSERT INTO users (reg_id) VALUES (:reg_id)";
            $insertUserStmt = $pdo->prepare($insertUserSql);
            $insertUserStmt->execute([':reg_id' => $regId]);
            $actualUserId = $pdo->lastInsertId();
        } catch (PDOException $e) {
            // If status column exists, try with status
            if (strpos($e->getMessage(), 'status') !== false) {
                $insertUserSql = "INSERT INTO users (reg_id, status) VALUES (:reg_id, 'Active')";
                $insertUserStmt = $pdo->prepare($insertUserSql);
                $insertUserStmt->execute([':reg_id' => $regId]);
                $actualUserId = $pdo->lastInsertId();
            } else {
                throw $e;
            }
        }
        
        if (!$actualUserId) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            echo json_encode(array(
                'success' => false,
                'message' => 'Failed to create user entry'
            ));
            exit;
        }
    }
    
    // Check for overlapping bookings using room_units.room_id (the actual room unit selected)
    // This checks if the specific room unit is already booked, not just the bhr_id
    error_log("Step 2: Checking for overlapping bookings...");
    $checkOverlapSql = "
        SELECT b.booking_id 
        FROM bookings b
        INNER JOIN room_units ru ON b.room_id = ru.room_id
        WHERE ru.room_id = :room_id 
        AND b.booking_status IN ('Pending', 'Confirmed')
        AND (
            (b.start_date <= :start_date AND b.end_date >= :start_date)
            OR (b.start_date <= :end_date AND b.end_date >= :end_date)
            OR (b.start_date >= :start_date AND b.end_date <= :end_date)
        )
        LIMIT 1
    ";
    $checkOverlapStmt = $pdo->prepare($checkOverlapSql);
    $checkOverlapStmt->execute([
        ':room_id' => $actualRoomId,  // This is room_units.room_id
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    if ($checkOverlapStmt->fetch()) {
        error_log("ERROR: Overlapping booking found for room_id: $actualRoomId");
        $pdo->rollBack();
        error_log("Transaction rolled back - Overlapping booking");
        echo json_encode(array(
            'success' => false,
            'message' => 'Room is already booked for the selected dates'
        ));
        exit;
    }
    error_log("Step 2 Success: No overlapping bookings found");
    
    // CRITICAL: Update status to 'Occupied' BEFORE creating booking to prevent race conditions
    // Use atomic UPDATE that only succeeds if status is still 'Available'
    error_log("Step 3: Attempting to reserve room by updating status to 'Occupied' (atomic operation)...");
    $updateStatusSql = "UPDATE room_units SET status = 'Occupied' WHERE room_id = :room_id AND status = 'Available'";
    $updateStatusStmt = $pdo->prepare($updateStatusSql);
    $updateStatusStmt->execute([':room_id' => $actualRoomId]);
    $rowsAffected = $updateStatusStmt->rowCount();
    error_log("Step 3 Result: Status update attempted - rows affected: $rowsAffected");
    
    // If no rows were affected, room was already booked by another request
    if ($rowsAffected == 0) {
        error_log("ERROR: Failed to reserve room - status was already changed (race condition)");
        // Re-check status to see what it actually is
        $checkStatusSql = "SELECT status FROM room_units WHERE room_id = :room_id";
        $checkStatusStmt = $pdo->prepare($checkStatusSql);
        $checkStatusStmt->execute([':room_id' => $actualRoomId]);
        $actualStatus = $checkStatusStmt->fetch(PDO::FETCH_ASSOC);
        error_log("ERROR: Current room status: " . ($actualStatus['status'] ?? 'null'));
        
        $pdo->rollBack();
        error_log("Transaction rolled back - Room reservation failed");
        echo json_encode(array(
            'success' => false,
            'message' => 'Selected room unit is not available. Status: ' . ($actualStatus['status'] ?? 'Unknown')
        ));
        exit;
    }
    error_log("Step 3 Success: Room reserved (status updated to 'Occupied')");
    
    // Now create the booking (room is already reserved, so this should always succeed)
    error_log("Step 4: Creating booking - room_id: $actualRoomId, user_id: $actualUserId, start_date: $startDate, end_date: $endDate");
    $insertSql = "
        INSERT INTO bookings (
            room_id, 
            user_id, 
            start_date, 
            end_date, 
            booking_status, 
            booking_date
        ) VALUES (
            :room_id,
            :user_id,
            :start_date,
            :end_date,
            'Pending',
            NOW()
        )
    ";
    
    $insertStmt = $pdo->prepare($insertSql);
    $insertStmt->execute([
        ':room_id' => $actualRoomId,  // This is room_units.room_id
        ':user_id' => $actualUserId,  // Use actual user_id from users table
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    $bookingId = $pdo->lastInsertId();
    error_log("Step 4 Result: Booking created with booking_id: $bookingId");
    
    if (!$bookingId) {
        error_log("ERROR: Failed to create booking - no booking_id returned");
        // Rollback status update as well
        $rollbackStatusSql = "UPDATE room_units SET status = 'Available' WHERE room_id = :room_id";
        $rollbackStatusStmt = $pdo->prepare($rollbackStatusSql);
        $rollbackStatusStmt->execute([':room_id' => $actualRoomId]);
        error_log("Rolled back room status to 'Available'");
        
        $pdo->rollBack();
        error_log("Transaction rolled back - Booking creation failed");
        echo json_encode(array(
            'success' => false,
            'message' => 'Failed to create booking'
        ));
        exit;
    }
    
    // Handle payment proof upload
    $paymentProofPath = '';
    if (!empty($paymentProofBase64)) {
        // Decode base64 image
        $imageData = base64_decode($paymentProofBase64);
        
        // Generate unique filename
        $filename = 'payment_proof_' . $bookingId . '_' . time() . '.jpg';
        $uploadDir = '../uploads/payment_proofs/';
        
        // Create directory if it doesn't exist
        if (!file_exists($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }
        
        $filePath = $uploadDir . $filename;
        
        // Save image
        if (file_put_contents($filePath, $imageData)) {
            // Store relative path from BoardEase2 directory
            $paymentProofPath = 'uploads/payment_proofs/' . $filename;
        } else {
            error_log("Failed to save payment proof image for booking_id: " . $bookingId);
        }
    }
    
    // Get owner_id from room_unit's bhr_id
    // boarding_houses.user_id is registrations.id, but we need users.user_id
    $bhrId = $roomUnit['bhr_id']; // Get bhr_id from the room_unit we already fetched
    $getOwnerSql = "SELECT bh.user_id as owner_reg_id, u.user_id as owner_user_id 
                    FROM boarding_house_rooms bhr 
                    JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id 
                    LEFT JOIN users u ON bh.user_id = u.reg_id
                    WHERE bhr.bhr_id = :bhr_id";
    $getOwnerStmt = $pdo->prepare($getOwnerSql);
    $getOwnerStmt->execute([':bhr_id' => $bhrId]);
    $ownerData = $getOwnerStmt->fetch(PDO::FETCH_ASSOC);
    $ownerId = $ownerData ? intval($ownerData['owner_user_id']) : 0;
    
    // If owner doesn't have a users entry, create one
    if (!$ownerId && $ownerData && $ownerData['owner_reg_id']) {
        try {
            // Try without status column first
            $insertOwnerSql = "INSERT INTO users (reg_id) VALUES (:reg_id)";
            $insertOwnerStmt = $pdo->prepare($insertOwnerSql);
            $insertOwnerStmt->execute([':reg_id' => $ownerData['owner_reg_id']]);
            $ownerId = $pdo->lastInsertId();
        } catch (PDOException $e) {
            // If status column exists, try with status
            if (strpos($e->getMessage(), 'status') !== false) {
                $insertOwnerSql = "INSERT INTO users (reg_id, status) VALUES (:reg_id, 'Active')";
                $insertOwnerStmt = $pdo->prepare($insertOwnerSql);
                $insertOwnerStmt->execute([':reg_id' => $ownerData['owner_reg_id']]);
                $ownerId = $pdo->lastInsertId();
            } else {
                // Log error but don't fail the booking
                error_log("Warning: Could not create owner user entry: " . $e->getMessage());
            }
        }
    }
    
    // Use calculated total amount from Android if provided, otherwise calculate from room price (fallback)
    if ($totalAmount > 0) {
        // Use the calculated amount from Android
        $paymentAmount = $totalAmount;
        error_log("Using calculated total_amount from Android: $paymentAmount");
    } else {
        // Fallback: Get room price for payment amount (using bhr_id from room_unit)
        $getRoomPriceSql = "SELECT price FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
        $getRoomPriceStmt = $pdo->prepare($getRoomPriceSql);
        $getRoomPriceStmt->execute([':bhr_id' => $bhrId]);
        $roomData = $getRoomPriceStmt->fetch(PDO::FETCH_ASSOC);
        $paymentAmount = $roomData ? floatval($roomData['price']) : 0;
        error_log("Using room price as fallback: $paymentAmount");
    }
    
    // Determine if this is a monthly payment (30+ days) or short-term (less than 30 days)
    $isMonthlyPayment = ($numberOfDays >= 30) ? 1 : 0;
    
    // Calculate payment month/year
    $paymentMonth = date('Y-m', strtotime($startDate));
    $paymentYear = intval(date('Y', strtotime($startDate)));
    $paymentMonthNumber = intval(date('m', strtotime($startDate)));
    
    error_log("Payment details - amount: $paymentAmount, days: $numberOfDays, is_monthly: $isMonthlyPayment");
    
    // Create payment record
    if ($ownerId > 0) {
        try {
            $insertPaymentSql = "
                INSERT INTO payments (
                    booking_id,
                    user_id,
                    owner_id,
                    payment_amount,
                    payment_method,
                    payment_proof,
                    payment_status,
                    payment_date,
                    payment_month,
                    payment_year,
                    payment_month_number,
                    is_monthly_payment
                ) VALUES (
                    :booking_id,
                    :user_id,
                    :owner_id,
                    :payment_amount,
                    :payment_method,
                    :payment_proof,
                    'Pending',
                    NOW(),
                    :payment_month,
                    :payment_year,
                    :payment_month_number,
                    :is_monthly_payment
                )
            ";
            
            $insertPaymentStmt = $pdo->prepare($insertPaymentSql);
            $insertPaymentStmt->execute([
                ':booking_id' => $bookingId,
                ':user_id' => $actualUserId,  // Use actual user_id from users table
                ':owner_id' => $ownerId,
                ':payment_amount' => $paymentAmount,
                ':payment_method' => $paymentMethod,
                ':payment_proof' => $paymentProofPath,
                ':payment_month' => $paymentMonth,
                ':payment_year' => $paymentYear,
                ':payment_month_number' => $paymentMonthNumber,
                ':is_monthly_payment' => $isMonthlyPayment
            ]);
            error_log("Payment record created successfully - amount: $paymentAmount, is_monthly: $isMonthlyPayment");
        } catch (PDOException $e) {
            // Log error but don't fail if payment creation fails
            error_log("Warning: Could not create payment record: " . $e->getMessage());
            // Continue anyway - booking is still valid
        }
    }
    
    // Status was already updated immediately after booking creation
    // Now commit transaction - all operations succeeded
    error_log("Step 4: Committing transaction for booking_id: $bookingId");
    $pdo->commit();
    error_log("Step 4 Success: Transaction committed successfully");
    
    error_log("=== BOOKING PROCESS COMPLETE - SUCCESS ===");
    error_log("Final result: booking_id=$bookingId, room_id=$actualRoomId, user_id=$actualUserId");
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Booking created successfully',
        'booking_id' => $bookingId
    ));
    
} catch (PDOException $e) {
    // Rollback transaction on error
    error_log("=== BOOKING PROCESS FAILED - PDOException ===");
    error_log("Exception message: " . $e->getMessage());
    error_log("Exception trace: " . $e->getTraceAsString());
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
        error_log("Transaction rolled back due to PDOException");
    } else {
        error_log("WARNING: No active transaction to rollback");
    }
    error_log("Database error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    // Rollback transaction on error
    error_log("=== BOOKING PROCESS FAILED - Exception ===");
    error_log("Exception message: " . $e->getMessage());
    error_log("Exception trace: " . $e->getTraceAsString());
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
        error_log("Transaction rolled back due to Exception");
    } else {
        error_log("WARNING: No active transaction to rollback");
    }
    error_log("Server error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
}
?>

