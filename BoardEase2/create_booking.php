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
    
    // Get POST data
    $roomId = isset($_POST['room_id']) ? intval($_POST['room_id']) : 0;
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $startDate = isset($_POST['start_date']) ? trim($_POST['start_date']) : '';
    $endDate = isset($_POST['end_date']) ? trim($_POST['end_date']) : '';
    $paymentMethod = isset($_POST['payment_method']) ? trim($_POST['payment_method']) : 'Cash';
    $paymentProofBase64 = isset($_POST['payment_proof']) ? trim($_POST['payment_proof']) : '';
    
    // Debug logging
    error_log("create_booking.php - Received data: room_id=$roomId, user_id=$userId, start_date=$startDate, end_date=$endDate");
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
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
        echo json_encode(array(
            'success' => false,
            'message' => 'Invalid date format. Expected YYYY-MM-DD'
        ));
        exit;
    }
    
    // Validate end date is after start date
    if ($endDateObj <= $startDateObj) {
        echo json_encode(array(
            'success' => false,
            'message' => 'End date must be after start date'
        ));
        exit;
    }
    
    // Check if room exists in boarding_house_rooms (bhr_id)
    $checkRoomSql = "SELECT bhr_id FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
    $checkRoomStmt = $pdo->prepare($checkRoomSql);
    $checkRoomStmt->execute([':bhr_id' => $roomId]);
    $room = $checkRoomStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$room) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Room not found'
        ));
        exit;
    }
    
    // Get or create a room_unit for this bhr_id
    // First, try to find an available room_unit
    $getRoomUnitSql = "SELECT room_id FROM room_units WHERE bhr_id = :bhr_id AND status = 'Available' LIMIT 1";
    $getRoomUnitStmt = $pdo->prepare($getRoomUnitSql);
    $getRoomUnitStmt->execute([':bhr_id' => $roomId]);
    $roomUnit = $getRoomUnitStmt->fetch(PDO::FETCH_ASSOC);
    
    $actualRoomId = null;
    if ($roomUnit) {
        // Use existing available room_unit
        $actualRoomId = $roomUnit['room_id'];
    } else {
        // Create a new room_unit for this bhr_id if none exists
        // Get room details to create appropriate room_number
        $getRoomDetailsSql = "SELECT room_name, room_category FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
        $getRoomDetailsStmt = $pdo->prepare($getRoomDetailsSql);
        $getRoomDetailsStmt->execute([':bhr_id' => $roomId]);
        $roomDetails = $getRoomDetailsStmt->fetch(PDO::FETCH_ASSOC);
        
        $roomNumber = $roomDetails ? $roomDetails['room_name'] : 'R-1';
        if (empty($roomNumber)) {
            $roomNumber = 'R-1';
        }
        
        // Insert new room_unit
        $insertRoomUnitSql = "INSERT INTO room_units (bhr_id, room_number, status) VALUES (:bhr_id, :room_number, 'Available')";
        $insertRoomUnitStmt = $pdo->prepare($insertRoomUnitSql);
        $insertRoomUnitStmt->execute([
            ':bhr_id' => $roomId,
            ':room_number' => $roomNumber
        ]);
        $actualRoomId = $pdo->lastInsertId();
    }
    
    if (!$actualRoomId) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Failed to get or create room unit'
        ));
        exit;
    }
    
    // Check if user exists in registrations and get corresponding user_id from users table
    // The userId from Android is registrations.id, but bookings needs users.user_id
    $checkUserSql = "SELECT r.id, u.user_id 
                     FROM registrations r 
                     LEFT JOIN users u ON r.id = u.reg_id 
                     WHERE r.id = :reg_id";
    $checkUserStmt = $pdo->prepare($checkUserSql);
    $checkUserStmt->execute([':reg_id' => $userId]);
    $user = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        echo json_encode(array(
            'success' => false,
            'message' => 'User not found in registrations'
        ));
        exit;
    }
    
    // Get the actual user_id from users table (needed for bookings foreign key)
    $actualUserId = $user['user_id'];
    
    // If user doesn't have a corresponding entry in users table, create one
    if (!$actualUserId) {
        // Insert into users table
        $insertUserSql = "INSERT INTO users (reg_id, status) VALUES (:reg_id, 'Active')";
        $insertUserStmt = $pdo->prepare($insertUserSql);
        $insertUserStmt->execute([':reg_id' => $userId]);
        $actualUserId = $pdo->lastInsertId();
        
        if (!$actualUserId) {
            echo json_encode(array(
                'success' => false,
                'message' => 'Failed to create user entry'
            ));
            exit;
        }
    }
    
    // Check for overlapping bookings using actual room_id
    $checkOverlapSql = "
        SELECT booking_id 
        FROM bookings 
        WHERE room_id = :room_id 
        AND booking_status IN ('Pending', 'Confirmed')
        AND (
            (start_date <= :start_date AND end_date >= :start_date)
            OR (start_date <= :end_date AND end_date >= :end_date)
            OR (start_date >= :start_date AND end_date <= :end_date)
        )
    ";
    $checkOverlapStmt = $pdo->prepare($checkOverlapSql);
    $checkOverlapStmt->execute([
        ':room_id' => $actualRoomId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    if ($checkOverlapStmt->fetch()) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Room is already booked for the selected dates'
        ));
        exit;
    }
    
    // Insert booking using actual room_id from room_units
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
        ':room_id' => $actualRoomId,
        ':user_id' => $actualUserId,  // Use actual user_id from users table
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    $bookingId = $pdo->lastInsertId();
    
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
    
    // Get owner_id from room
    // boarding_houses.user_id is registrations.id, but we need users.user_id
    $getOwnerSql = "SELECT bh.user_id as owner_reg_id, u.user_id as owner_user_id 
                    FROM boarding_house_rooms bhr 
                    JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id 
                    LEFT JOIN users u ON bh.user_id = u.reg_id
                    WHERE bhr.bhr_id = :bhr_id";
    $getOwnerStmt = $pdo->prepare($getOwnerSql);
    $getOwnerStmt->execute([':bhr_id' => $roomId]);
    $ownerData = $getOwnerStmt->fetch(PDO::FETCH_ASSOC);
    $ownerId = $ownerData ? intval($ownerData['owner_user_id']) : 0;
    
    // If owner doesn't have a users entry, create one
    if (!$ownerId && $ownerData && $ownerData['owner_reg_id']) {
        $insertOwnerSql = "INSERT INTO users (reg_id, status) VALUES (:reg_id, 'Active')";
        $insertOwnerStmt = $pdo->prepare($insertOwnerSql);
        $insertOwnerStmt->execute([':reg_id' => $ownerData['owner_reg_id']]);
        $ownerId = $pdo->lastInsertId();
    }
    
    // Get room price for payment amount (using bhr_id)
    $getRoomPriceSql = "SELECT price FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
    $getRoomPriceStmt = $pdo->prepare($getRoomPriceSql);
    $getRoomPriceStmt->execute([':bhr_id' => $roomId]);
    $roomData = $getRoomPriceStmt->fetch(PDO::FETCH_ASSOC);
    $paymentAmount = $roomData ? floatval($roomData['price']) : 0;
    
    // Calculate payment month/year
    $paymentMonth = date('Y-m', strtotime($startDate));
    $paymentYear = intval(date('Y', strtotime($startDate)));
    $paymentMonthNumber = intval(date('m', strtotime($startDate)));
    
    // Create payment record
    if ($ownerId > 0) {
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
                1
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
            ':payment_month_number' => $paymentMonthNumber
        ]);
    }
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Booking created successfully',
        'booking_id' => $bookingId
    ));
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
}
?>

