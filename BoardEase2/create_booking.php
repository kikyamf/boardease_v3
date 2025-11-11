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

// Start output buffering to prevent any unwanted output
ob_start();

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
    
    // Get POST data - handle both POST and JSON input
    $inputData = [];
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!empty($_POST)) {
            $inputData = $_POST;
        } else {
            // Try to get JSON input
            $jsonInput = file_get_contents('php://input');
            if (!empty($jsonInput)) {
                $decoded = json_decode($jsonInput, true);
                if (json_last_error() === JSON_ERROR_NONE) {
                    $inputData = $decoded;
                }
            }
        }
    }
    
    $roomId = isset($inputData['room_id']) ? intval($inputData['room_id']) : 0;
    $userId = isset($inputData['user_id']) ? intval($inputData['user_id']) : 0;
    $startDate = isset($inputData['start_date']) ? trim($inputData['start_date']) : '';
    $endDate = isset($inputData['end_date']) ? trim($inputData['end_date']) : '';
    $paymentMethod = isset($inputData['payment_method']) ? trim($inputData['payment_method']) : 'Cash';
    $paymentProofBase64 = isset($inputData['payment_proof']) ? trim($inputData['payment_proof']) : '';
    
    // Get calculated payment amount and number of days from Android
    // If not provided, calculate it from room price (fallback for backward compatibility)
    $totalAmount = isset($inputData['total_amount']) ? floatval($inputData['total_amount']) : 0;
    $numberOfDays = isset($inputData['number_of_days']) ? intval($inputData['number_of_days']) : 0;
    $paymentBreakdownJson = isset($inputData['payment_breakdown']) ? $inputData['payment_breakdown'] : '';
    
    // Debug logging
    error_log("create_booking.php - Received data: room_id=$roomId, user_id=$userId, start_date=$startDate, end_date=$endDate, total_amount=$totalAmount, number_of_days=$numberOfDays");
    error_log("Payment breakdown JSON length: " . strlen($paymentBreakdownJson));
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
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
        ob_end_flush();
        exit;
    }
    
    // Validate date format
    $startDateObj = DateTime::createFromFormat('Y-m-d', $startDate);
    $endDateObj = DateTime::createFromFormat('Y-m-d', $endDate);
    
    if (!$startDateObj || !$endDateObj) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'success' => false,
            'message' => 'Invalid date format. Expected YYYY-MM-DD'
        ));
        ob_end_flush();
        exit;
    }
    
    // Validate end date is after start date
    if ($endDateObj <= $startDateObj) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'success' => false,
            'message' => 'End date must be after start date'
        ));
        ob_end_flush();
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
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
            error_log("Transaction rolled back - Room unit not found");
        }
        http_response_code(404);
        echo json_encode(array(
            'success' => false,
            'message' => 'Room unit not found'
        ));
        ob_end_flush();
        exit;
    }
    
    error_log("Step 1 Result: Room unit found - room_id: " . $roomUnit['room_id'] . ", bhr_id: " . $roomUnit['bhr_id'] . ", status: " . $roomUnit['status']);
    
    // Get room category and capacity from boarding_house_rooms
    $bhrId = $roomUnit['bhr_id'];
    $getRoomInfoSql = "SELECT room_category, capacity FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
    $getRoomInfoStmt = $pdo->prepare($getRoomInfoSql);
    $getRoomInfoStmt->execute([':bhr_id' => $bhrId]);
    $roomInfo = $getRoomInfoStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomInfo) {
        error_log("ERROR: Room category not found for bhr_id: $bhrId");
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(404);
        echo json_encode(array(
            'success' => false,
            'message' => 'Room information not found'
        ));
        ob_end_flush();
        exit;
    }
    
    $roomCategory = $roomInfo['room_category'];
    $capacity = intval($roomInfo['capacity']);
    error_log("Step 1.1: Room category: $roomCategory, capacity: $capacity");
    
    // Check if room unit is available (for Private Room only - Bed Spacer uses capacity logic)
    if ($roomCategory === 'Private Room') {
        if (isset($roomUnit['status']) && $roomUnit['status'] !== 'Available') {
            error_log("ERROR: Private Room unit status is NOT Available. Current status: " . $roomUnit['status']);
            error_log("ERROR: Exiting BEFORE creating booking - no booking should be created");
            
            // CRITICAL: Rollback transaction BEFORE exiting
            ob_clean();
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
            http_response_code(400);
            $errorResponse = json_encode(array(
                'success' => false,
                'message' => 'Selected room unit is not available. Status: ' . $roomUnit['status']
            ));
            error_log("ERROR: Returning error response: " . $errorResponse);
            error_log("ERROR: EXITING - No booking should be created after this point");
            echo $errorResponse;
            ob_end_flush();
            exit; // CRITICAL: Exit immediately to prevent any further code execution
        }
        error_log("Step 1 Success: Private Room unit is Available, proceeding with booking");
    } else {
        error_log("Step 1 Success: Bed Spacer room - will check capacity during overlap check");
    }
    
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
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("User not found - searched userId: " . $userId);
        http_response_code(404);
        echo json_encode(array(
            'success' => false,
            'message' => 'User not found'
        ));
        ob_end_flush();
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
            ob_clean();
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            http_response_code(500);
            echo json_encode(array(
                'success' => false,
                'message' => 'Failed to create user entry'
            ));
            ob_end_flush();
            exit;
        }
    }
    
    // Check for overlapping bookings using room_units.room_id (the actual room unit selected)
    // Different logic for Private Room vs Bed Spacer
    error_log("Step 2: Checking for overlapping bookings (room_category: $roomCategory)...");
    
    // Initialize overlap count (used for Bed Spacer)
    $overlapCount = 0;
    
    if ($roomCategory === 'Private Room') {
        // Private Room: Check if ANY overlapping booking exists
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
            ':room_id' => $actualRoomId,
            ':start_date' => $startDate,
            ':end_date' => $endDate
        ]);
        
        if ($checkOverlapStmt->fetch()) {
            error_log("ERROR: Overlapping booking found for Private Room - room_id: $actualRoomId");
            ob_clean();
            $pdo->rollBack();
            error_log("Transaction rolled back - Overlapping booking");
            http_response_code(400);
            echo json_encode(array(
                'success' => false,
                'message' => 'Room is already booked for the selected dates'
            ));
            ob_end_flush();
            exit;
        }
        error_log("Step 2 Success: No overlapping bookings found for Private Room");
        
    } else if ($roomCategory === 'Bed Spacer') {
        // Bed Spacer: Count overlapping bookings and check against capacity
        $checkOverlapSql = "
            SELECT COUNT(b.booking_id) as overlap_count
            FROM bookings b
            INNER JOIN room_units ru ON b.room_id = ru.room_id
            WHERE ru.room_id = :room_id 
            AND b.booking_status IN ('Pending', 'Confirmed')
            AND (
                (b.start_date <= :start_date AND b.end_date >= :start_date)
                OR (b.start_date <= :end_date AND b.end_date >= :end_date)
                OR (b.start_date >= :start_date AND b.end_date <= :end_date)
            )
        ";
        $checkOverlapStmt = $pdo->prepare($checkOverlapSql);
        $checkOverlapStmt->execute([
            ':room_id' => $actualRoomId,
            ':start_date' => $startDate,
            ':end_date' => $endDate
        ]);
        
        $overlapResult = $checkOverlapStmt->fetch(PDO::FETCH_ASSOC);
        $overlapCount = intval($overlapResult['overlap_count']);
        error_log("Step 2: Bed Spacer - Found $overlapCount overlapping bookings, capacity: $capacity");
        
        // For Bed Spacer, allow booking if overlap_count < capacity
        if ($overlapCount >= $capacity) {
            error_log("ERROR: Bed Spacer room is at full capacity - room_id: $actualRoomId, overlap_count: $overlapCount, capacity: $capacity");
            ob_clean();
            $pdo->rollBack();
            error_log("Transaction rolled back - Bed Spacer at full capacity");
            http_response_code(400);
            echo json_encode(array(
                'success' => false,
                'message' => 'Room is already fully booked for the selected dates. All ' . $capacity . ' beds are occupied.'
            ));
            ob_end_flush();
            exit;
        }
        error_log("Step 2 Success: Bed Spacer has capacity available ($overlapCount/$capacity beds occupied)");
        
    } else {
        // Unknown room category - use Private Room logic (conservative approach)
        error_log("WARNING: Unknown room category: $roomCategory, using Private Room logic");
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
            ':room_id' => $actualRoomId,
            ':start_date' => $startDate,
            ':end_date' => $endDate
        ]);
        
        if ($checkOverlapStmt->fetch()) {
            error_log("ERROR: Overlapping booking found - room_id: $actualRoomId");
            ob_clean();
            $pdo->rollBack();
            error_log("Transaction rolled back - Overlapping booking");
            http_response_code(400);
            echo json_encode(array(
                'success' => false,
                'message' => 'Room is already booked for the selected dates'
            ));
            ob_end_flush();
            exit;
        }
        error_log("Step 2 Success: No overlapping bookings found");
    }
    
    // Update room status based on room category
    // Private Room: Set to 'Occupied' immediately (only one booking allowed)
    // Bed Spacer: Only set to 'Occupied' if capacity is reached after this booking
    error_log("Step 3: Updating room status (room_category: $roomCategory)...");
    
    if ($roomCategory === 'Private Room') {
        // Private Room: Update status to 'Occupied' BEFORE creating booking to prevent race conditions
        // Use atomic UPDATE that only succeeds if status is still 'Available'
        error_log("Step 3: Attempting to reserve Private Room by updating status to 'Occupied' (atomic operation)...");
        $updateStatusSql = "UPDATE room_units SET status = 'Occupied' WHERE room_id = :room_id AND status = 'Available'";
        $updateStatusStmt = $pdo->prepare($updateStatusSql);
        $updateStatusStmt->execute([':room_id' => $actualRoomId]);
        $rowsAffected = $updateStatusStmt->rowCount();
        error_log("Step 3 Result: Status update attempted - rows affected: $rowsAffected");
        
        // If no rows were affected, room was already booked by another request
        if ($rowsAffected == 0) {
            error_log("ERROR: Failed to reserve Private Room - status was already changed (race condition)");
            // Re-check status to see what it actually is
            $checkStatusSql = "SELECT status FROM room_units WHERE room_id = :room_id";
            $checkStatusStmt = $pdo->prepare($checkStatusSql);
            $checkStatusStmt->execute([':room_id' => $actualRoomId]);
            $actualStatus = $checkStatusStmt->fetch(PDO::FETCH_ASSOC);
            error_log("ERROR: Current room status: " . ($actualStatus['status'] ?? 'null'));
            
            ob_clean();
            $pdo->rollBack();
            error_log("Transaction rolled back - Room reservation failed");
            http_response_code(400);
            echo json_encode(array(
                'success' => false,
                'message' => 'Selected room unit is not available. Status: ' . ($actualStatus['status'] ?? 'Unknown')
            ));
            ob_end_flush();
            exit;
        }
        error_log("Step 3 Success: Private Room reserved (status updated to 'Occupied')");
        
    } else if ($roomCategory === 'Bed Spacer') {
        // Bed Spacer: Check if this booking will fill the capacity
        // Count current active bookings (including this one we're about to create)
        $currentBookingCount = $overlapCount + 1; // +1 for the booking we're about to create
        error_log("Step 3: Bed Spacer - Current bookings after this one: $currentBookingCount/$capacity");
        
        if ($currentBookingCount >= $capacity) {
            // This booking will fill the capacity, update status to 'Occupied'
            error_log("Step 3: Bed Spacer will be at full capacity, updating status to 'Occupied'...");
            $updateStatusSql = "UPDATE room_units SET status = 'Occupied' WHERE room_id = :room_id";
            $updateStatusStmt = $pdo->prepare($updateStatusSql);
            $updateStatusStmt->execute([':room_id' => $actualRoomId]);
            error_log("Step 3 Success: Bed Spacer status updated to 'Occupied' (at full capacity)");
        } else {
            // Still has capacity, keep status as 'Available' (or don't change it)
            error_log("Step 3 Success: Bed Spacer still has capacity ($currentBookingCount/$capacity), status remains 'Available'");
        }
        
    } else {
        // Unknown category - use Private Room logic
        error_log("Step 3: Unknown room category, using Private Room logic...");
        $updateStatusSql = "UPDATE room_units SET status = 'Occupied' WHERE room_id = :room_id AND status = 'Available'";
        $updateStatusStmt = $pdo->prepare($updateStatusSql);
        $updateStatusStmt->execute([':room_id' => $actualRoomId]);
        $rowsAffected = $updateStatusStmt->rowCount();
        error_log("Step 3 Result: Status update attempted - rows affected: $rowsAffected");
    }
    
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
        
        ob_clean();
        $pdo->rollBack();
        error_log("Transaction rolled back - Booking creation failed");
        http_response_code(500);
        echo json_encode(array(
            'success' => false,
            'message' => 'Failed to create booking'
        ));
        ob_end_flush();
        exit;
    }
    
    // Handle payment proof upload
    $paymentProofPath = '';
    if (!empty($paymentProofBase64)) {
        error_log("Step 5: Processing payment proof upload...");
        try {
            // Remove data URL prefix if present
            $base64Data = $paymentProofBase64;
            if (preg_match('/^data:image\/(\w+);base64,/', $paymentProofBase64, $matches)) {
                $base64Data = preg_replace('/^data:image\/\w+;base64,/', '', $paymentProofBase64);
            }
            
            // Decode base64 image
            $imageData = base64_decode($base64Data, true);
            
            if ($imageData === false) {
                error_log("Warning: Failed to decode payment proof base64 data");
            } else {
                // Generate unique filename
                $filename = 'payment_proof_' . $bookingId . '_' . time() . '.jpg';
                $uploadDir = dirname(__DIR__) . '/uploads/payment_proofs/';
                
                // Create directory if it doesn't exist
                if (!file_exists($uploadDir)) {
                    mkdir($uploadDir, 0777, true);
                    error_log("Created payment proof directory: $uploadDir");
                }
                
                $filePath = $uploadDir . $filename;
                
                // Save image
                if (file_put_contents($filePath, $imageData)) {
                    // Store relative path from BoardEase2 directory (for get_payment_proof.php)
                    $paymentProofPath = 'uploads/payment_proofs/' . $filename;
                    error_log("Payment proof saved successfully: $paymentProofPath");
                } else {
                    error_log("Warning: Failed to save payment proof image for booking_id: $bookingId");
                }
            }
        } catch (Exception $e) {
            error_log("Warning: Error processing payment proof: " . $e->getMessage());
            // Continue anyway - booking is still valid
        }
    }
    
    // Get owner_id from room_unit's bhr_id
    // boarding_houses.user_id is registrations.id, but we need users.user_id
    // Note: $bhrId is already set from Step 1.1 above
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
    
    // Calculate number of days if not provided
    if ($numberOfDays == 0) {
        $numberOfDays = $startDateObj->diff($endDateObj)->days;
        if ($numberOfDays == 0) {
            $numberOfDays = 1; // Minimum 1 day
        }
        error_log("Calculated number_of_days: $numberOfDays");
    }
    
    // Determine if this is a monthly payment (30+ days) or short-term (less than 30 days)
    $isMonthlyPayment = ($numberOfDays >= 30) ? 1 : 0;
    
    // Calculate payment month/year
    $paymentMonth = date('Y-m', strtotime($startDate));
    $paymentYear = intval(date('Y', strtotime($startDate)));
    $paymentMonthNumber = intval(date('m', strtotime($startDate)));
    
    error_log("Payment details - amount: $paymentAmount, days: $numberOfDays, is_monthly: $isMonthlyPayment");
    
    // Create payment record
    $paymentId = null;
    if ($ownerId > 0) {
        error_log("Step 6: Creating payment record...");
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
            $paymentId = $pdo->lastInsertId(); // Get payment_id after insertion
            error_log("Payment record created successfully - payment_id: $paymentId, amount: $paymentAmount, is_monthly: $isMonthlyPayment");
            
            // Save payment breakdown if provided
            if (!empty($paymentBreakdownJson) && $paymentId) {
                error_log("Step 7: Processing payment breakdown...");
                try {
                    // Parse JSON breakdown
                    $breakdownArray = json_decode($paymentBreakdownJson, true);
                    
                    if (json_last_error() !== JSON_ERROR_NONE) {
                        error_log("Warning: Invalid payment breakdown JSON - Error: " . json_last_error_msg());
                    } elseif (is_array($breakdownArray) && !empty($breakdownArray)) {
                        error_log("Saving payment breakdown - " . count($breakdownArray) . " periods");
                        
                        // Check if payment_breakdowns table exists by attempting to describe it
                        $tableExists = false;
                        try {
                            $checkTableSql = "DESCRIBE payment_breakdowns";
                            $pdo->query($checkTableSql);
                            $tableExists = true;
                            error_log("payment_breakdowns table exists");
                        } catch (PDOException $e) {
                            error_log("Warning: payment_breakdowns table does not exist or cannot be accessed: " . $e->getMessage());
                            $tableExists = false;
                        }
                        
                        if ($tableExists) {
                            $insertBreakdownSql = "
                                INSERT INTO payment_breakdowns (
                                    booking_id,
                                    payment_id,
                                    period_type,
                                    period_number,
                                    period_label,
                                    period_start_date,
                                    period_end_date,
                                    amount,
                                    is_selected,
                                    payment_status,
                                    due_date
                                ) VALUES (
                                    :booking_id,
                                    :payment_id,
                                    :period_type,
                                    :period_number,
                                    :period_label,
                                    :period_start_date,
                                    :period_end_date,
                                    :amount,
                                    :is_selected,
                                    'Pending',
                                    :due_date
                                )
                            ";
                            
                            $insertBreakdownStmt = $pdo->prepare($insertBreakdownSql);
                            $breakdownCount = 0;
                            
                            foreach ($breakdownArray as $index => $period) {
                                try {
                                    $periodType = isset($period['period_type']) ? trim($period['period_type']) : 'month';
                                    $periodNumber = isset($period['period_number']) ? intval($period['period_number']) : ($index + 1);
                                    $periodLabel = isset($period['label']) ? trim($period['label']) : (isset($period['period_label']) ? trim($period['period_label']) : 'Period ' . ($index + 1));
                                    $periodStartDate = isset($period['start_date']) ? trim($period['start_date']) : $startDate;
                                    $periodEndDate = isset($period['end_date']) ? trim($period['end_date']) : $endDate;
                                    $periodAmount = isset($period['amount']) ? floatval($period['amount']) : 0;
                                    $isSelected = isset($period['is_selected']) ? (bool)$period['is_selected'] : false;
                                    
                                    // Validate period dates
                                    $periodStartObj = DateTime::createFromFormat('Y-m-d', $periodStartDate);
                                    $periodEndObj = DateTime::createFromFormat('Y-m-d', $periodEndDate);
                                    
                                    if (!$periodStartObj || !$periodEndObj) {
                                        error_log("Warning: Invalid date format in breakdown period $index, skipping");
                                        continue;
                                    }
                                    
                                    // Set due date as period start date (can be adjusted later)
                                    $dueDate = $periodStartDate;
                                    
                                    $insertBreakdownStmt->execute([
                                        ':booking_id' => $bookingId,
                                        ':payment_id' => $paymentId,
                                        ':period_type' => $periodType,
                                        ':period_number' => $periodNumber,
                                        ':period_label' => $periodLabel,
                                        ':period_start_date' => $periodStartDate,
                                        ':period_end_date' => $periodEndDate,
                                        ':amount' => $periodAmount,
                                        ':is_selected' => $isSelected ? 1 : 0,
                                        ':due_date' => $dueDate
                                    ]);
                                    
                                    $breakdownCount++;
                                    error_log("Saved breakdown period $index: $periodLabel - Amount: $periodAmount - Selected: " . ($isSelected ? 'Yes' : 'No'));
                                } catch (PDOException $e) {
                                    error_log("Warning: Failed to save breakdown period $index: " . $e->getMessage());
                                    // Continue with next period
                                }
                            }
                            
                            if ($breakdownCount > 0) {
                                error_log("Payment breakdown saved successfully - $breakdownCount periods");
                            } else {
                                error_log("Warning: No breakdown periods were saved");
                            }
                        } else {
                            error_log("Warning: payment_breakdowns table does not exist - skipping breakdown save");
                        }
                    } else {
                        error_log("Warning: Payment breakdown JSON is empty or not an array");
                    }
                } catch (Exception $e) {
                    error_log("Warning: Error processing payment breakdown: " . $e->getMessage());
                    error_log("Breakdown error trace: " . $e->getTraceAsString());
                    // Continue anyway - booking and payment are still valid
                }
            } else {
                if (empty($paymentBreakdownJson)) {
                    error_log("No payment breakdown JSON provided");
                } else {
                    error_log("Warning: Payment ID is null, cannot save breakdown");
                }
            }
        } catch (PDOException $e) {
            // Log error but don't fail if payment creation fails
            error_log("Warning: Could not create payment record: " . $e->getMessage());
            error_log("Payment error trace: " . $e->getTraceAsString());
            // Continue anyway - booking is still valid
        }
    } else {
        error_log("Warning: Owner ID is 0 or not found - skipping payment creation");
    }
    
    // Commit transaction - all operations succeeded
    error_log("Step 8: Committing transaction for booking_id: $bookingId");
    $pdo->commit();
    error_log("Step 8 Success: Transaction committed successfully");
    
    error_log("=== BOOKING PROCESS COMPLETE - SUCCESS ===");
    error_log("Final result: booking_id=$bookingId, room_id=$actualRoomId, user_id=$actualUserId, payment_id=" . ($paymentId ?? 'null'));
    
    ob_clean();
    http_response_code(200);
    echo json_encode(array(
        'success' => true,
        'message' => 'Booking created successfully',
        'booking_id' => $bookingId,
        'payment_id' => $paymentId
    ));
    ob_end_flush();
    
} catch (PDOException $e) {
    // Rollback transaction on error
    error_log("=== BOOKING PROCESS FAILED - PDOException ===");
    error_log("Exception message: " . $e->getMessage());
    error_log("Exception trace: " . $e->getTraceAsString());
    ob_clean();
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
        error_log("Transaction rolled back due to PDOException");
    } else {
        error_log("WARNING: No active transaction to rollback");
    }
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ));
    ob_end_flush();
} catch (Exception $e) {
    // Rollback transaction on error
    error_log("=== BOOKING PROCESS FAILED - Exception ===");
    error_log("Exception message: " . $e->getMessage());
    error_log("Exception trace: " . $e->getTraceAsString());
    ob_clean();
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
        error_log("Transaction rolled back due to Exception");
    } else {
        error_log("WARNING: No active transaction to rollback");
    }
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
    ob_end_flush();
}
?>
