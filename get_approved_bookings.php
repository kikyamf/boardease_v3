<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Database configuration
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $ownerId = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

    if ($ownerId === 0) {
        echo json_encode(['success' => false, 'error' => 'User ID is required.']);
        exit;
    }

    // Query for approved/confirmed bookings
    // Status = 'Approved' AND NO 'Pending' payment (Waiting for boarder to act)
    // OR Status = 'Confirmed' (Booking finalized)
    $sql = "
        SELECT 
            b.booking_id,
            b.room_id,
            b.user_id as boarder_id,
            b.start_date,
            b.end_date,
            b.booking_status as status,
            b.booking_date,
            ru.room_number as room_name,
            bhr.room_category,
            bhr.price as amount,
            bh.bh_id as boarding_house_id,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            r.first_name,
            r.last_name,
            r.email as boarder_email,
            r.phone as boarder_phone,
            (SELECT payment_status FROM payments WHERE booking_id = b.booking_id ORDER BY payment_id DESC LIMIT 1) as payment_status,
            (SELECT payment_proof FROM payments WHERE booking_id = b.booking_id ORDER BY payment_id DESC LIMIT 1) as payment_proof,
            (SELECT COUNT(*) FROM payment_breakdowns WHERE booking_id = b.booking_id) as total_periods,
            (SELECT COUNT(*) FROM payment_breakdowns WHERE booking_id = b.booking_id AND is_paid = 1) as paid_periods,
            (SELECT COUNT(*) FROM payment_breakdowns WHERE booking_id = b.booking_id AND is_paid = 0) as unpaid_periods,
            (SELECT SUM(amount) FROM payment_breakdowns WHERE booking_id = b.booking_id) as total_amount_for_booking,
            (SELECT SUM(amount) FROM payment_breakdowns WHERE booking_id = b.booking_id AND is_paid = 1) as paid_amount_for_booking,
            (SELECT SUM(amount) FROM payment_breakdowns WHERE booking_id = b.booking_id AND is_paid = 0) as remaining_amount_to_pay
        FROM bookings b
        JOIN room_units ru ON b.room_id = ru.room_id
        JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        JOIN users u ON b.user_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        WHERE (bh.user_id = :owner_id OR bh.user_id = (SELECT reg_id FROM users WHERE user_id = :owner_id LIMIT 1))
        AND (
            b.booking_status IN ('Confirmed', 'Upcoming', 'Active')
            OR (
                b.booking_status = 'Approved' 
                AND NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = b.booking_id AND payment_status = 'Pending')
            )
        )
        ORDER BY b.booking_date DESC
    ";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([':owner_id' => $ownerId]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $approvedBookings = [];
    foreach ($results as $row) {
        $totalPeriods = intval($row['total_periods']);
        $paidPeriods = intval($row['paid_periods']);
        
        $progress = 0;
        if ($totalPeriods > 0) {
            $progress = ($paidPeriods / $totalPeriods) * 100;
        }

        $approvedBookings[] = [
            'booking_id' => intval($row['booking_id']),
            'boarder_name' => $row['first_name'] . ' ' . $row['last_name'],
            'boarder_email' => $row['boarder_email'],
            'boarder_phone' => $row['boarder_phone'],
            'room_name' => $row['room_name'],
            'start_date' => $row['start_date'],
            'end_date' => $row['end_date'],
            'amount' => $row['amount'],
            'rent_type' => '',
            'status' => $row['status'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'booking_date' => $row['booking_date'],
            'payment_status' => $row['payment_status'] ?? 'Pending',
            'payment_proof_url' => $row['payment_proof'] ? 'https://boardease.calapebohol.com/get_payment_proof.php?path=' . urlencode($row['payment_proof']) : null,
            'notes' => '',
            'boarder_id' => intval($row['boarder_id']),
            'room_id' => intval($row['room_id']),
            'boarding_house_id' => intval($row['boarding_house_id']),
            'total_periods' => $totalPeriods,
            'paid_periods' => $paidPeriods,
            'unpaid_periods' => intval($row['unpaid_periods']),
            'total_months_for_booking' => $totalPeriods,
            'paid_months_for_booking' => $paidPeriods,
            'remaining_months_to_pay' => intval($row['unpaid_periods']),
            'total_amount_for_booking' => $row['total_amount_for_booking'] ?? "0.00",
            'paid_amount_for_booking' => $row['paid_amount_for_booking'] ?? "0.00",
            'remaining_amount_to_pay' => $row['remaining_amount_to_pay'] ?? "0.00",
            'is_fully_paid' => ($totalPeriods > 0 && $paidPeriods >= $totalPeriods),
            'payment_progress_percent' => round($progress, 2)
        ];
    }

    echo json_encode([
        'success' => true,
        'data' => [
            'approved_bookings' => $approvedBookings
        ]
    ]);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
