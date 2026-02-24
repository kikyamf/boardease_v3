<?php
// Suppress all error output to prevent HTML from breaking JSON
error_reporting(0);
ini_set('display_errors', 0);
ini_set('display_startup_errors', 0);

// Start output buffering early to catch any output
ob_start();

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    ob_clean();
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
header('Cache-Control: no-cache, must-revalidate');
header('Expires: Mon, 26 Jul 1997 05:00:00 GMT');

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

$host = DB_HOST;
$dbname = DB_NAME;
$username = DB_USER;
$password = DB_PASS;

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get JSON input
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        ob_clean();
        echo json_encode(array(
            'success' => false,
            'error' => 'Invalid JSON data'
        ));
        ob_end_flush();
        exit;
    }
    
    $ownerId = isset($data['owner_id']) ? intval($data['owner_id']) : 0;
    $status = isset($data['status']) ? trim($data['status']) : 'all';
    
    // Log the request for debugging
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Request received: owner_id=" . $ownerId . ", status=" . $status);
        error_log("get_payment_status.php - Full request data: " . json_encode($data));
    }
    
    if ($ownerId == 0) {
        ob_clean();
        echo json_encode(array(
            'success' => false,
            'error' => 'Owner ID is required'
        ));
        ob_end_flush();
        exit;
    }
    
    // IMPORTANT: The owner_id in payments table might not match the user_id directly
    // We need to get payments for boarding houses owned by this user
    // So we need to join with bookings -> room_units -> boarding_house_rooms -> boarding_houses
    // and check if boarding_houses.user_id = :owner_id
    
    // First, let's check if there are any payments for this owner (for debugging)
    $checkPaymentsSql = "SELECT COUNT(*) as count FROM payments WHERE owner_id = :owner_id";
    $checkPaymentsStmt = $pdo->prepare($checkPaymentsSql);
    $checkPaymentsStmt->execute([':owner_id' => $ownerId]);
    $paymentCount = $checkPaymentsStmt->fetch(PDO::FETCH_ASSOC);
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Total payments found for owner_id " . $ownerId . " (direct): " . $paymentCount['count']);
    }
    
    // Also check payments via boarding houses owned by this user
    $checkPaymentsViaBhSql = "
        SELECT COUNT(*) as count 
        FROM payments p
        LEFT JOIN bookings b ON p.booking_id = b.booking_id
        LEFT JOIN room_units ru ON b.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE bh.user_id = :owner_id
    ";
    $checkPaymentsViaBhStmt = $pdo->prepare($checkPaymentsViaBhSql);
    $checkPaymentsViaBhStmt->execute([':owner_id' => $ownerId]);
    $paymentCountViaBh = $checkPaymentsViaBhStmt->fetch(PDO::FETCH_ASSOC);
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Total payments found for owner_id " . $ownerId . " (via boarding houses): " . $paymentCountViaBh['count']);
    }
    
    // Also check what owner_ids exist in payments table
    $checkAllOwnersSql = "SELECT DISTINCT owner_id, COUNT(*) as count FROM payments GROUP BY owner_id";
    $checkAllOwnersStmt = $pdo->prepare($checkAllOwnersSql);
    $checkAllOwnersStmt->execute();
    $allOwners = $checkAllOwnersStmt->fetchAll(PDO::FETCH_ASSOC);
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Payments by owner_id: " . json_encode($allOwners));
    }
    
    // Check if there are any payments at all
    $checkAllPaymentsSql = "SELECT payment_id, owner_id, payment_status FROM payments LIMIT 10";
    $checkAllPaymentsStmt = $pdo->prepare($checkAllPaymentsSql);
    $checkAllPaymentsStmt->execute();
    $samplePayments = $checkAllPaymentsStmt->fetchAll(PDO::FETCH_ASSOC);
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Sample payments in database: " . json_encode($samplePayments));
    }
    
    // Verify owner exists (but don't fail if not found, just log)
    $getOwnerSql = "SELECT user_id FROM users WHERE user_id = :owner_id";
    $getOwnerStmt = $pdo->prepare($getOwnerSql);
    $getOwnerStmt->execute([':owner_id' => $ownerId]);
    $ownerData = $getOwnerStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$ownerData && function_exists('error_log')) {
        error_log("get_payment_status.php - Warning: Owner ID " . $ownerId . " not found in users table");
        // Don't exit, continue with the query anyway
    }
    
    // IMPORTANT: Get payments for boarding houses owned by this user
    // The owner_id in payments table might be incorrect or not match the boarding house owner
    // So we need to get payments via bookings -> room_units -> boarding_house_rooms -> boarding_houses
    // and check if boarding_houses.user_id = :owner_id
    
    // This is the CORRECT way to get payments - via boarding house ownership
    $simpleSqlViaBh = "
        SELECT DISTINCT p.payment_id, p.booking_id, p.user_id, p.owner_id, p.payment_amount, 
               p.payment_method, p.payment_status, p.payment_date, p.notes, p.payment_proof, 
               p.receipt_url, p.created_at, p.updated_at,
               bh.user_id as actual_owner_id
        FROM payments p
        LEFT JOIN bookings b ON p.booking_id = b.booking_id
        LEFT JOIN room_units ru ON b.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE bh.user_id = :owner_id
    ";
    
    // Also try direct owner_id match as fallback (in case payments.owner_id is correct)
    $simpleSql = "SELECT p.payment_id, p.booking_id, p.user_id, p.owner_id, p.payment_amount, 
                  p.payment_method, p.payment_status, p.payment_date, p.notes, p.payment_proof, 
                  p.receipt_url, p.created_at, p.updated_at
                  FROM payments p
                  WHERE p.owner_id = :owner_id";
    
    // Add status filter to queries
    // For 'all', show all payments regardless of status
    if ($status === 'pending') {
        $simpleSql .= " AND COALESCE(p.payment_status, 'Pending') IN ('Pending', 'For Approval')";
        $simpleSqlViaBh .= " AND COALESCE(p.payment_status, 'Pending') IN ('Pending', 'For Approval')";
    } elseif ($status === 'paid' || $status === 'completed') {
        // Include 'Completed', 'Partially Paid', and 'Paid' in the Completed tab
        $simpleSql .= " AND COALESCE(p.payment_status, 'Pending') IN ('Completed', 'Partially Paid', 'Paid')";
        $simpleSqlViaBh .= " AND COALESCE(p.payment_status, 'Pending') IN ('Completed', 'Partially Paid', 'Paid')";
    } elseif (strtolower($status) === 'overdue') {
        // For overdue, check payment_breakdowns
        // Include Pending AND Partially Paid payments that are overdue
        $overdueCondition = " AND COALESCE(p.payment_status, 'Pending') IN ('Pending', 'Partially Paid') 
                              AND EXISTS (
                                  SELECT 1 FROM payment_breakdowns pb 
                                  WHERE pb.booking_id = p.booking_id 
                                  AND (
                                      pb.payment_status = 'Overdue' 
                                      OR (pb.due_date < CURDATE() AND pb.is_paid = 0)
                                  )
                              )";
        $simpleSql .= $overdueCondition;
        $simpleSqlViaBh .= $overdueCondition;
    } elseif ($status === 'fully_paid') {
        // For fully_paid, include both Confirmed and Completed bookings
        // Payment progress filtering will be done after getting the results
        $simpleSql .= " AND EXISTS (SELECT 1 FROM bookings b2 WHERE b2.booking_id = p.booking_id AND COALESCE(b2.booking_status, 'Pending') IN ('Confirmed', 'Completed'))";
        $simpleSqlViaBh .= " AND EXISTS (SELECT 1 FROM bookings b2 WHERE b2.booking_id = p.booking_id AND COALESCE(b2.booking_status, 'Pending') IN ('Confirmed', 'Completed'))";
    } elseif ($status === 'remaining') {
        // For remaining, only include Confirmed bookings
        // Payment progress filtering will be done after getting the results
        $simpleSql .= " AND EXISTS (SELECT 1 FROM bookings b2 WHERE b2.booking_id = p.booking_id AND COALESCE(b2.booking_status, 'Pending') = 'Confirmed')";
        $simpleSqlViaBh .= " AND EXISTS (SELECT 1 FROM bookings b2 WHERE b2.booking_id = p.booking_id AND COALESCE(b2.booking_status, 'Pending') = 'Confirmed')";
    }
    // For 'all', no additional filter - show all payments (Pending, Completed, Failed, Refunded)
    
    $simpleSql .= " ORDER BY p.payment_date DESC, p.created_at DESC";
    $simpleSqlViaBh .= " ORDER BY p.payment_date DESC, p.created_at DESC";
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Simple query (direct): " . $simpleSql);
        error_log("get_payment_status.php - Simple query (via BH): " . $simpleSqlViaBh);
        error_log("get_payment_status.php - Requested status: " . $status);
        error_log("get_payment_status.php - Owner ID: " . $ownerId);
    }
    
    // PRIORITY: Try via boarding houses first (this is the correct way)
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Trying via boarding houses first...");
    }
    $simpleStmtViaBh = $pdo->prepare($simpleSqlViaBh);
    $simpleStmtViaBh->execute([':owner_id' => $ownerId]);
    $simplePayments = $simpleStmtViaBh->fetchAll(PDO::FETCH_ASSOC);
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Query (via BH) returned " . count($simplePayments) . " payments");
    }
    
    // If no results via boarding houses, try direct owner_id match as fallback
    if (count($simplePayments) == 0) {
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - No payments found via boarding houses, trying direct owner_id match...");
        }
        $simpleStmt = $pdo->prepare($simpleSql);
        $simpleStmt->execute([':owner_id' => $ownerId]);
        $directPayments = $simpleStmt->fetchAll(PDO::FETCH_ASSOC);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Query (direct) returned " . count($directPayments) . " payments");
        }
        
        // Combine results (should be empty if via BH didn't work)
        $simplePayments = $directPayments;
    }
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Final simple query returned " . count($simplePayments) . " payments");
    }
    
    // Debug: Show what we got
    if (count($simplePayments) > 0) {
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - First payment: " . json_encode($simplePayments[0]));
        }
    } else {
        // Check if payments exist for this owner at all
        $checkSql = "SELECT payment_id, owner_id, payment_status FROM payments WHERE owner_id = :owner_id LIMIT 5";
        $checkStmt = $pdo->prepare($checkSql);
        $checkStmt->execute([':owner_id' => $ownerId]);
        $checkPayments = $checkStmt->fetchAll(PDO::FETCH_ASSOC);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Direct check for owner_id " . $ownerId . ": " . json_encode($checkPayments));
        }
        
        // Also check what owner_ids exist
        $allOwnersSql = "SELECT DISTINCT owner_id, COUNT(*) as count FROM payments GROUP BY owner_id";
        $allOwnersStmt = $pdo->prepare($allOwnersSql);
        $allOwnersStmt->execute();
        $allOwners = $allOwnersStmt->fetchAll(PDO::FETCH_ASSOC);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - All owner_ids in payments table: " . json_encode($allOwners));
        }
    }
    
    // Build the main SQL query - ALWAYS use boarding house ownership for accuracy
    // Use COALESCE to handle NULL payment_status (default to 'Pending')
    $sql = "
        SELECT
            p.payment_id,
            p.booking_id,
            p.user_id,
            p.owner_id,
            p.payment_amount,
            p.payment_method,
            COALESCE(p.payment_status, 'Pending') as payment_status,
            DATE_FORMAT(p.payment_date, '%Y-%m-%d %H:%i:%s') as payment_date,
            p.notes,
            p.payment_proof,
            p.receipt_url,
            DATE_FORMAT(p.created_at, '%Y-%m-%d %H:%i:%s') as created_at,
            DATE_FORMAT(p.updated_at, '%Y-%m-%d %H:%i:%s') as updated_at,
            -- Boarder information (use LEFT JOIN to handle missing data)
            COALESCE(reg.first_name, '') as first_name,
            COALESCE(reg.middle_name, '') as middle_name,
            COALESCE(reg.last_name, '') as last_name,
            COALESCE(reg.suffix, '') as suffix,
            TRIM(CONCAT(COALESCE(reg.first_name, ''), ' ', COALESCE(reg.middle_name, ''), ' ', COALESCE(reg.last_name, ''), ' ', COALESCE(reg.suffix, ''))) as boarder_name,
            COALESCE(reg.email, '') as boarder_email,
            COALESCE(reg.phone, '') as boarder_phone,
            COALESCE(u_boarder.profile_picture, '') as profile_picture,
            -- Room information
            COALESCE(bhr.room_name, 'Room N/A') as room_name,
            COALESCE(bhr.room_category, 'Monthly') as rent_type,
            COALESCE(bhr.price, p.payment_amount) as total_amount,
            COALESCE(ru.room_number, '') as room_number,
            CASE 
                WHEN bhr.room_name IS NOT NULL AND ru.room_number IS NOT NULL 
                THEN CONCAT(bhr.room_name, ' - Room ', ru.room_number)
                WHEN bhr.room_name IS NOT NULL 
                THEN bhr.room_name
                ELSE CONCAT('Room ', COALESCE(CAST(ru.room_number AS CHAR), 'N/A'))
            END as room,
            -- Booking information
            COALESCE(b.booking_status, 'Pending') as rental_status,
            DATE_FORMAT(b.start_date, '%Y-%m-%d') as start_date,
            DATE_FORMAT(b.end_date, '%Y-%m-%d') as end_date,
            -- Get next due date from payment_breakdowns (earliest unpaid period)
            (SELECT MIN(pb.due_date)
             FROM payment_breakdowns pb
             WHERE pb.booking_id = b.booking_id
               AND pb.is_paid = 0
               AND pb.due_date IS NOT NULL
            ) as due_date,
            -- Boarding house information
            COALESCE(bh.bh_name, '') as boarding_house_name,
            COALESCE(bh.bh_address, '') as boarding_house_address,
            bh.user_id as actual_owner_id
        FROM payments p
        LEFT JOIN users u_boarder ON p.user_id = u_boarder.user_id
        LEFT JOIN registrations reg ON u_boarder.reg_id = reg.id
        LEFT JOIN bookings b ON p.booking_id = b.booking_id
        LEFT JOIN room_units ru ON b.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE bh.user_id = :owner_id
    ";
    
    // Add status filter (use COALESCE to handle NULL values)
    if ($status === 'pending') {
        // Show both 'Pending' and 'For Approval' statuses. 
        // We've removed the strict payment_proof check to ensure all pending items are visible.
        $sql .= " AND COALESCE(p.payment_status, 'Pending') IN ('Pending', 'For Approval')";
    } elseif ($status === 'paid' || $status === 'completed') {
        // Include 'Completed', 'Partially Paid', and 'Paid' in the Completed tab
        $sql .= " AND COALESCE(p.payment_status, 'Pending') IN ('Completed', 'Partially Paid', 'Paid')";
    } elseif (strtolower($status) === 'overdue') {
        // For overdue, check if the payment is linked to an overdue breakdown
        // OR if the payment itself is pending/partially paid and past due
        $sql .= " AND COALESCE(p.payment_status, 'Pending') IN ('Pending', 'Partially Paid') 
                  AND EXISTS (
                      SELECT 1 FROM payment_breakdowns pb 
                      WHERE pb.booking_id = p.booking_id 
                      AND (
                          pb.payment_status = 'Overdue' 
                          OR (pb.due_date < CURDATE() AND pb.is_paid = 0)
                      )
                  )";
    } elseif ($status === 'fully_paid') {
        // For fully paid, include both Confirmed and Completed bookings
        // Payment progress filtering will be done after getting the results
        $sql .= " AND COALESCE(b.booking_status, 'Pending') IN ('Confirmed', 'Completed')";
    } elseif ($status === 'remaining') {
        // For remaining, only include Confirmed bookings
        // Payment progress filtering will be done after getting the results
        $sql .= " AND COALESCE(b.booking_status, 'Pending') = 'Confirmed'";
    }
    // For 'all', no additional filter
    
    $sql .= " ORDER BY p.payment_date DESC, p.created_at DESC";
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Executing main query (via BH): " . $sql);
        error_log("get_payment_status.php - With owner_id: " . $ownerId);
    }
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':owner_id' => $ownerId]);
    $payments = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Main query returned " . count($payments) . " payments");
    }
    
    // If no payments found, let's check why
    if (count($payments) == 0) {
        // Check if there are payments with different owner_id
        $checkOtherOwnersSql = "SELECT payment_id, owner_id, user_id, payment_status FROM payments LIMIT 5";
        $checkOtherOwnersStmt = $pdo->prepare($checkOtherOwnersSql);
        $checkOtherOwnersStmt->execute();
        $otherPayments = $checkOtherOwnersStmt->fetchAll(PDO::FETCH_ASSOC);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Sample payments (any owner): " . json_encode($otherPayments));
        }
        
        // Check if the JOINs are causing issues
        $simpleCheckSql = "SELECT p.payment_id, p.owner_id, p.user_id, p.payment_status, 
                           u_boarder.user_id as boarder_user_id, reg.id as reg_id
                           FROM payments p
                           LEFT JOIN users u_boarder ON p.user_id = u_boarder.user_id
                           LEFT JOIN registrations reg ON u_boarder.reg_id = reg.id
                           WHERE p.owner_id = :owner_id
                           LIMIT 5";
        $simpleCheckStmt = $pdo->prepare($simpleCheckSql);
        $simpleCheckStmt->execute([':owner_id' => $ownerId]);
        $simpleCheck = $simpleCheckStmt->fetchAll(PDO::FETCH_ASSOC);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Simple check (with JOINs): " . json_encode($simpleCheck));
        }
    }
    
    // Calculate payment progress for each booking using payment_breakdowns table
    // This is more accurate because it tracks each period individually
    $bookingProgress = array();
    $progressSql = "
        SELECT 
            b.booking_id,
            b.user_id,
            b.start_date,
            b.end_date,
            -- Count total periods for this booking
            COUNT(pb.breakdown_id) as total_periods,
            -- Count paid periods (ONLY is_paid = 1, not just payment_id IS NOT NULL)
            -- CRITICAL: payment_id can exist even if period is not paid (when booking is created)
            SUM(CASE WHEN pb.is_paid = 1 THEN 1 ELSE 0 END) as paid_periods,
            -- Count unpaid periods
            SUM(CASE WHEN pb.is_paid = 0 OR pb.is_paid IS NULL THEN 1 ELSE 0 END) as unpaid_periods,
            -- Total amount for all periods
            COALESCE(SUM(pb.amount), 0) as total_amount,
            -- Total amount paid (only for paid periods where is_paid = 1)
            COALESCE(SUM(CASE WHEN pb.is_paid = 1 THEN pb.amount ELSE 0 END), 0) as paid_amount,
            -- Count months (period_type = 'month')
            SUM(CASE WHEN pb.period_type = 'month' THEN 1 ELSE 0 END) as total_months,
            SUM(CASE WHEN pb.period_type = 'month' AND pb.is_paid = 1 THEN 1 ELSE 0 END) as paid_months
        FROM bookings b
        LEFT JOIN payment_breakdowns pb ON b.booking_id = pb.booking_id
        LEFT JOIN room_units ru ON b.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE bh.user_id = :owner_id
            AND COALESCE(b.booking_status, 'Pending') IN ('Confirmed', 'Pending', 'Approved')
        GROUP BY b.booking_id, b.user_id, b.start_date, b.end_date
    ";
    $progressStmt = $pdo->prepare($progressSql);
    $progressStmt->execute([':owner_id' => $ownerId]);
    $progressResults = $progressStmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($progressResults as $progress) {
        $totalPeriods = intval($progress['total_periods']);
        $paidPeriods = intval($progress['paid_periods']);
        $unpaidPeriods = intval($progress['unpaid_periods']);
        $totalMonths = intval($progress['total_months']);
        $paidMonths = intval($progress['paid_months']);
        
        $bookingProgress[$progress['booking_id']] = array(
            'total_periods' => $totalPeriods,
            'paid_periods' => $paidPeriods,
            'unpaid_periods' => $unpaidPeriods,
            'total_months' => $totalMonths,
            'paid_months' => $paidMonths,
            'remaining_months' => $totalMonths > 0 ? max(0, $totalMonths - $paidMonths) : null,
            'total_amount' => floatval($progress['total_amount']),
            'paid_amount' => floatval($progress['paid_amount']),
            'remaining_amount' => floatval($progress['total_amount']) - floatval($progress['paid_amount']),
            'is_fully_paid' => $totalPeriods > 0 && $paidPeriods >= $totalPeriods,
            'payment_progress_percent' => $totalPeriods > 0 ? round(($paidPeriods / $totalPeriods) * 100, 2) : 0
        );
    }
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Payment progress calculated for " . count($bookingProgress) . " bookings using payment_breakdowns");
    }
    
    // Format payments for response
    $formattedPayments = array();

    // If status is 'overdue', also fetch UNPAID overdue breakdowns that don't have a payment record yet
    if (strtolower($status) === 'overdue') {
        $overdueBreakdownsSql = "
            SELECT 
                pb.breakdown_id,
                pb.booking_id,
                pb.amount as payment_amount,
                pb.due_date,
                pb.payment_status,
                pb.period_label,
                b.user_id,
                b.start_date, b.end_date,
                -- Boarder Info
                COALESCE(u.profile_picture, '') as profile_picture,
                reg.first_name, reg.middle_name, reg.last_name, reg.suffix,
                reg.email, reg.phone,
                -- Room Info
                bhr.room_name, bhr.room_category, bhr.price, ru.room_number,
                bh.bh_name, bh.bh_address,
                b.booking_status as rental_status
            FROM payment_breakdowns pb
            JOIN bookings b ON pb.booking_id = b.booking_id
            LEFT JOIN payments p_link ON pb.payment_id = p_link.payment_id
            LEFT JOIN users u ON b.user_id = u.user_id
            LEFT JOIN registrations reg ON u.reg_id = reg.id
            LEFT JOIN room_units ru ON b.room_id = ru.room_id
            LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE (bh.user_id = :owner_id OR p_link.owner_id = :owner_id)
              AND (pb.payment_status = 'Overdue' OR (pb.due_date < CURDATE() AND pb.is_paid = 0))
              AND (
                  pb.payment_id IS NULL 
                  OR 
                  pb.payment_id NOT IN (
                      SELECT payment_id FROM payments 
                      WHERE payment_status IN ('Pending', 'Partially Paid', 'Fully Paid', 'Completed')
                  )
              )
            ORDER BY pb.due_date ASC
        ";
        
        $overdueStmt = $pdo->prepare($overdueBreakdownsSql);
        $overdueStmt->execute([':owner_id' => $ownerId]);
        $overdueBreakdowns = $overdueStmt->fetchAll(PDO::FETCH_ASSOC);
        
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Found " . count($overdueBreakdowns) . " overdue breakdowns");
        }
        
        foreach ($overdueBreakdowns as $bd) {
            // Format boarder name
            $boarderName = trim(($bd['first_name'] ?? '') . ' ' . 
                              ($bd['middle_name'] ?? '') . ' ' . 
                              ($bd['last_name'] ?? '') . ' ' . 
                              ($bd['suffix'] ?? ''));
            if (empty($boarderName)) $boarderName = 'Unknown';
            
            // Format room name
            $roomName = $bd['room_name'] ?? 'Room N/A';
            if (!empty($bd['room_number'])) {
                $roomName .= ' - Room ' . $bd['room_number'];
            }
            
            // Format amount
            $amount = '₱' . number_format(floatval($bd['payment_amount']), 2, '.', ',');
            
            // Get progress
            $bookingId = intval($bd['booking_id']);
            $progress = isset($bookingProgress[$bookingId]) ? $bookingProgress[$bookingId] : array(
                'total_periods' => 0, 'paid_periods' => 0, 'unpaid_periods' => 0,
                'total_months' => 0, 'paid_months' => 0, 'remaining_months' => 0,
                'total_amount' => 0, 'paid_amount' => 0, 'remaining_amount' => 0,
                'is_fully_paid' => false, 'payment_progress_percent' => 0
            );
            
            $formattedPayments[] = array(
                'payment_id' => -1 * intval($bd['breakdown_id']), // Negative ID to indicate virtual payment
                'booking_id' => $bookingId,
                'user_id' => intval($bd['user_id']),
                'boarder_name' => $boarderName,
                'room' => $roomName,
                'rent_type' => $bd['room_category'] ?? 'Monthly',
                'amount_paid' => $amount, // Amount due
                'total_amount' => $amount,
                'payment_status' => 'Overdue', // Force status to Overdue
                'rental_status' => $bd['rental_status'] ?? 'Active',
                'payment_date' => '', // No payment date yet
                'due_date' => $bd['due_date'],
                'payment_method' => 'Unpaid',
                'notes' => 'Overdue Bill: ' . $bd['period_label'],
                'payment_proof' => '',
                'receipt_url' => '',
                'profile_picture' => $bd['profile_picture'],
                // Progress info
                'total_periods' => $progress['total_periods'],
                'paid_periods' => $progress['paid_periods'],
                'unpaid_periods' => $progress['unpaid_periods'],
                'total_months_for_booking' => $progress['total_months'],
                'paid_months_for_booking' => $progress['paid_months'],
                'remaining_months_to_pay' => $progress['remaining_months'],
                'total_amount_for_booking' => number_format($progress['total_amount'], 2, '.', ''),
                'paid_amount_for_booking' => number_format($progress['paid_amount'], 2, '.', ''),
                'remaining_amount_to_pay' => number_format($progress['remaining_amount'], 2, '.', ''),
                'is_fully_paid' => $progress['is_fully_paid'],
                'payment_progress_percent' => $progress['payment_progress_percent'],
                'created_at' => '',
                'updated_at' => '',
                'boarder_email' => $bd['email'] ?? '',
                'boarder_phone' => $bd['phone'] ?? '',
                'boarding_house_name' => $bd['bh_name'] ?? '',
                'boarding_house_address' => $bd['bh_address'] ?? ''
            );
        }
    }
    
    // Always use simple query results if they exist (more reliable)
    // If simple query has results, use those and enrich with additional data
    if (count($simplePayments) > 0) {
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Simple query returned " . count($simplePayments) . " payments. Using simple query results and enriching with additional data.");
        }
        
        // Use simple payments and try to enrich with additional data
        foreach ($simplePayments as $simplePayment) {
            // Try to get boarder info
            $boarderInfo = null;
            $profilePicture = '';
            if (!empty($simplePayment['user_id'])) {
                $boarderSql = "SELECT reg.first_name, reg.middle_name, reg.last_name, reg.suffix, 
                              reg.email, reg.phone, COALESCE(u.profile_picture, '') as profile_picture
                              FROM users u 
                              LEFT JOIN registrations reg ON u.reg_id = reg.id 
                              WHERE u.user_id = :user_id";
                $boarderStmt = $pdo->prepare($boarderSql);
                $boarderStmt->execute([':user_id' => $simplePayment['user_id']]);
                $boarderInfo = $boarderStmt->fetch(PDO::FETCH_ASSOC);
                if ($boarderInfo) {
                    $profilePicture = $boarderInfo['profile_picture'] ?? '';
                }
            }
            
            // Try to get room/booking info
            $roomInfo = null;
            if (!empty($simplePayment['booking_id'])) {
                $roomSql = "SELECT b.booking_status, b.start_date, b.end_date,
                           bhr.room_name, bhr.room_category, bhr.price,
                           ru.room_number, bh.bh_name, bh.bh_address
                           FROM bookings b
                           LEFT JOIN room_units ru ON b.room_id = ru.room_id
                           LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                           LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                           WHERE b.booking_id = :booking_id";
                $roomStmt = $pdo->prepare($roomSql);
                $roomStmt->execute([':booking_id' => $simplePayment['booking_id']]);
                $roomInfo = $roomStmt->fetch(PDO::FETCH_ASSOC);
            }
            
            // Format boarder name
            $boarderName = 'Unknown';
            if ($boarderInfo) {
                $boarderName = trim(($boarderInfo['first_name'] ?? '') . ' ' . 
                                  ($boarderInfo['middle_name'] ?? '') . ' ' . 
                                  ($boarderInfo['last_name'] ?? '') . ' ' . 
                                  ($boarderInfo['suffix'] ?? ''));
                if (trim($boarderName) == '') {
                    $boarderName = 'Unknown';
                }
            }
            
            // Format room name
            $roomName = 'Room N/A';
            if ($roomInfo && !empty($roomInfo['room_name'])) {
                if (!empty($roomInfo['room_number'])) {
                    $roomName = $roomInfo['room_name'] . ' - Room ' . $roomInfo['room_number'];
                } else {
                    $roomName = $roomInfo['room_name'];
                }
            }
            
            // Format amounts
            $amountPaid = '₱' . number_format(floatval($simplePayment['payment_amount']), 2, '.', ',');
            $totalAmount = $amountPaid;
            if ($roomInfo && !empty($roomInfo['price'])) {
                $totalAmount = '₱' . number_format(floatval($roomInfo['price']), 2, '.', ',');
            }
            
            // Format dates
            $paymentDate = '';
            if (!empty($simplePayment['payment_date'])) {
                $paymentDate = date('Y-m-d H:i:s', strtotime($simplePayment['payment_date']));
            }
            
            
            // Get due date from payment_breakdowns table (most accurate)
            $dueDate = '';
            if (!empty($simplePayment['booking_id'])) {
                $dueDateSql = "SELECT MIN(pb.due_date) as next_due_date
                               FROM payment_breakdowns pb
                               WHERE pb.booking_id = :booking_id
                                 AND pb.is_paid = 0
                                 AND pb.due_date IS NOT NULL
                               ORDER BY pb.due_date ASC
                               LIMIT 1";
                $dueDateStmt = $pdo->prepare($dueDateSql);
                $dueDateStmt->execute([':booking_id' => $simplePayment['booking_id']]);
                $dueDateResult = $dueDateStmt->fetch(PDO::FETCH_ASSOC);
                if ($dueDateResult && !empty($dueDateResult['next_due_date'])) {
                    $dueDate = $dueDateResult['next_due_date'];
                }
            }
            
            // Get payment proof URL (prefer receipt_url, fallback to payment_proof)
            $paymentProofUrl = '';
            if (!empty($simplePayment['receipt_url'])) {
                $paymentProofUrl = $simplePayment['receipt_url'];
            } elseif (!empty($simplePayment['payment_proof'])) {
                $paymentProofUrl = $simplePayment['payment_proof'];
            }
            
            // Get payment progress for this booking from payment_breakdowns
            $bookingId = intval($simplePayment['booking_id'] ?? 0);
            $progress = isset($bookingProgress[$bookingId]) ? $bookingProgress[$bookingId] : array(
                'total_periods' => 0,
                'paid_periods' => 0,
                'unpaid_periods' => 0,
                'total_months' => 0,
                'paid_months' => 0,
                'remaining_months' => null,
                'total_amount' => 0,
                'paid_amount' => 0,
                'remaining_amount' => 0,
                'is_fully_paid' => false,
                'payment_progress_percent' => 0
            );
            
            $formattedPayments[] = array(
                'payment_id' => intval($simplePayment['payment_id']),
                'booking_id' => $bookingId,
                'user_id' => intval($simplePayment['user_id']),
                'boarder_name' => $boarderName,
                'room' => $roomName,
                'rent_type' => $roomInfo['room_category'] ?? 'Monthly',
                'amount_paid' => $amountPaid,
                'total_amount' => $totalAmount,
                'payment_status' => $simplePayment['payment_status'] ?? 'Pending',
                'rental_status' => $roomInfo['booking_status'] ?? 'Pending',
                'payment_date' => $paymentDate,
                'due_date' => $dueDate,
                'payment_method' => $simplePayment['payment_method'] ?? 'Cash',
                'notes' => $simplePayment['notes'] ?? '',
                'payment_proof' => $paymentProofUrl,
                'receipt_url' => $simplePayment['receipt_url'] ?? '',
                'profile_picture' => $profilePicture,
                // Payment progress information from payment_breakdowns
                'total_periods' => $progress['total_periods'],
                'paid_periods' => $progress['paid_periods'],
                'unpaid_periods' => $progress['unpaid_periods'],
                'total_months_for_booking' => $progress['total_months'],
                'paid_months_for_booking' => $progress['paid_months'],
                'remaining_months_to_pay' => $progress['remaining_months'],
                'total_amount_for_booking' => number_format($progress['total_amount'], 2, '.', ''),
                'paid_amount_for_booking' => number_format($progress['paid_amount'], 2, '.', ''),
                'remaining_amount_to_pay' => number_format($progress['remaining_amount'], 2, '.', ''),
                'is_fully_paid' => $progress['is_fully_paid'],
                'payment_progress_percent' => $progress['payment_progress_percent'],
                'created_at' => !empty($simplePayment['created_at']) ? date('Y-m-d H:i:s', strtotime($simplePayment['created_at'])) : '',
                'updated_at' => !empty($simplePayment['updated_at']) ? date('Y-m-d H:i:s', strtotime($simplePayment['updated_at'])) : '',
                'boarder_email' => $boarderInfo['email'] ?? '',
                'boarder_phone' => $boarderInfo['phone'] ?? '',
                'boarding_house_name' => $roomInfo['bh_name'] ?? '',
                'boarding_house_address' => $roomInfo['bh_address'] ?? ''
            );
        }
    } else {
        // Use main query results
        foreach ($payments as $payment) {
            // Format amount
            $amountPaid = '₱' . number_format(floatval($payment['payment_amount']), 2, '.', ',');
            $totalAmount = '₱' . number_format(floatval($payment['total_amount'] ?? $payment['payment_amount']), 2, '.', ',');
            
            // Format boarder name
            $boarderName = trim($payment['boarder_name'] ?? '');
            if (empty($boarderName) || $boarderName == '   ') {
                $boarderName = trim(($payment['first_name'] ?? '') . ' ' . 
                                  ($payment['middle_name'] ?? '') . ' ' . 
                                  ($payment['last_name'] ?? '') . ' ' . 
                                  ($payment['suffix'] ?? ''));
                if (empty($boarderName) || trim($boarderName) == '') {
                    $boarderName = 'Unknown';
                }
            }
            
            // Format room name
            $roomName = $payment['room'] ?? '';
            if (empty($roomName) || $roomName == 'Room N/A') {
                if (!empty($payment['room_name'])) {
                    $roomName = $payment['room_name'];
                    if (!empty($payment['room_number'])) {
                        $roomName .= ' - Room ' . $payment['room_number'];
                    }
                } else {
                    $roomName = 'Room N/A';
                }
            }
            
            // Get payment proof URL (prefer receipt_url, fallback to payment_proof)
            $paymentProofUrl = '';
            if (!empty($payment['receipt_url'])) {
                $paymentProofUrl = $payment['receipt_url'];
            } elseif (!empty($payment['payment_proof'])) {
                $paymentProofUrl = $payment['payment_proof'];
            }
            
            // Get payment progress for this booking from payment_breakdowns
            $bookingId = intval($payment['booking_id'] ?? 0);
            $progress = isset($bookingProgress[$bookingId]) ? $bookingProgress[$bookingId] : array(
                'total_periods' => 0,
                'paid_periods' => 0,
                'unpaid_periods' => 0,
                'total_months' => 0,
                'paid_months' => 0,
                'remaining_months' => null,
                'total_amount' => 0,
                'paid_amount' => 0,
                'remaining_amount' => 0,
                'is_fully_paid' => false,
                'payment_progress_percent' => 0
            );
            
            $formattedPayments[] = array(
                'payment_id' => intval($payment['payment_id']),
                'booking_id' => $bookingId,
                'user_id' => intval($payment['user_id']),
                'boarder_name' => $boarderName,
                'room' => $roomName,
                'rent_type' => $payment['rent_type'] ?? 'Monthly',
                'amount_paid' => $amountPaid,
                'total_amount' => $totalAmount,
                'payment_status' => $payment['payment_status'] ?? 'Pending',
                'rental_status' => $payment['rental_status'] ?? 'Pending',
                'payment_date' => $payment['payment_date'] ?? '',
                'due_date' => $payment['due_date'] ?? '',
                'payment_method' => $payment['payment_method'] ?? 'Cash',
                'notes' => $payment['notes'] ?? '',
                'payment_proof' => $paymentProofUrl,
                'receipt_url' => $payment['receipt_url'] ?? '',
                'profile_picture' => $payment['profile_picture'] ?? '',
                // Payment progress information from payment_breakdowns
                'total_periods' => $progress['total_periods'],
                'paid_periods' => $progress['paid_periods'],
                'unpaid_periods' => $progress['unpaid_periods'],
                'total_months_for_booking' => $progress['total_months'],
                'paid_months_for_booking' => $progress['paid_months'],
                'remaining_months_to_pay' => $progress['remaining_months'],
                'total_amount_for_booking' => number_format($progress['total_amount'], 2, '.', ''),
                'paid_amount_for_booking' => number_format($progress['paid_amount'], 2, '.', ''),
                'remaining_amount_to_pay' => number_format($progress['remaining_amount'], 2, '.', ''),
                'is_fully_paid' => $progress['is_fully_paid'],
                'payment_progress_percent' => $progress['payment_progress_percent'],
                'created_at' => $payment['created_at'] ?? '',
                'updated_at' => $payment['updated_at'] ?? '',
                'boarder_email' => $payment['boarder_email'] ?? '',
                'boarder_phone' => $payment['boarder_phone'] ?? '',
                'boarding_house_name' => $payment['boarding_house_name'] ?? '',
                'boarding_house_address' => $payment['boarding_house_address'] ?? ''
            );
        }
    }
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Formatted " . count($formattedPayments) . " payments for response");
    }
    
    // Deduplicate payments by booking_id - keep only the most recent payment per booking
    // This prevents showing duplicate "Partially Paid" entries when multiple payments exist for the same booking
    $bookingPaymentMap = array(); // booking_id => most recent payment
    $paymentsWithoutBooking = array(); // Payments without valid booking_id (edge case)
    
    foreach ($formattedPayments as $payment) {
        $bookingId = intval($payment['booking_id'] ?? 0);
        
        if ($bookingId > 0) {
            // Determine the timestamp for comparison (prefer payment_date, fallback to created_at)
            $paymentTimestamp = '';
            if (!empty($payment['payment_date'])) {
                $paymentTimestamp = $payment['payment_date'];
            } elseif (!empty($payment['created_at'])) {
                $paymentTimestamp = $payment['created_at'];
            }
            
            // If we haven't seen this booking yet, or this payment is more recent, keep it
            if (!isset($bookingPaymentMap[$bookingId])) {
                $bookingPaymentMap[$bookingId] = $payment;
            } else {
                // Compare timestamps to find the most recent payment
                $existingTimestamp = '';
                if (!empty($bookingPaymentMap[$bookingId]['payment_date'])) {
                    $existingTimestamp = $bookingPaymentMap[$bookingId]['payment_date'];
                } elseif (!empty($bookingPaymentMap[$bookingId]['created_at'])) {
                    $existingTimestamp = $bookingPaymentMap[$bookingId]['created_at'];
                }
                
                // If current payment is more recent, replace the existing one
                if (!empty($paymentTimestamp) && !empty($existingTimestamp)) {
                    if (strtotime($paymentTimestamp) > strtotime($existingTimestamp)) {
                        $bookingPaymentMap[$bookingId] = $payment;
                    }
                } elseif (!empty($paymentTimestamp)) {
                    // Current payment has timestamp, existing doesn't - use current
                    $bookingPaymentMap[$bookingId] = $payment;
                }
            }
        } else {
            // Edge case: payment without valid booking_id - include it anyway
            $paymentsWithoutBooking[] = $payment;
        }
    }
    
    // Convert the map back to array and merge with payments without booking_id
    // This gives us one payment per booking (the most recent) plus any edge case payments
    $formattedPayments = array_merge(array_values($bookingPaymentMap), $paymentsWithoutBooking);
    
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - After deduplication: " . count($formattedPayments) . " payments (removed duplicates by booking_id)");
    }
    
    // Filter for fully_paid status if requested
    if ($status === 'fully_paid') {
        $formattedPayments = array_filter($formattedPayments, function($payment) {
            // Include both Confirmed and Completed bookings that are fully paid
            $rentalStatus = isset($payment['rental_status']) ? $payment['rental_status'] : '';
            $isConfirmed = $rentalStatus === 'Confirmed';
            $isCompleted = $rentalStatus === 'Completed';
            $isValidStatus = $isConfirmed || $isCompleted;
            
            // Check if fully paid via is_fully_paid flag
            $isFullyPaid = isset($payment['is_fully_paid']) && $payment['is_fully_paid'] === true;
            
            // Also check payment_status field for "Fully Paid" status
            $paymentStatus = isset($payment['payment_status']) ? strtolower($payment['payment_status']) : '';
            $isFullyPaidStatus = $paymentStatus === 'fully paid';
            
            // Check if all periods are paid (paid_periods >= total_periods)
            $totalPeriods = isset($payment['total_periods']) ? intval($payment['total_periods']) : 0;
            $paidPeriods = isset($payment['paid_periods']) ? intval($payment['paid_periods']) : 0;
            $allPeriodsPaid = $totalPeriods > 0 && $paidPeriods >= $totalPeriods;
            
            // Payment is fully paid if rental status is Confirmed or Completed AND any payment condition is true
            return $isValidStatus && ($isFullyPaid || $isFullyPaidStatus || $allPeriodsPaid);
        });
        // Re-index array after filtering
        $formattedPayments = array_values($formattedPayments);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Filtered to " . count($formattedPayments) . " fully paid bookings (Confirmed or Completed)");
        }
    }
    
    // Filter for remaining status if requested (bookings with unpaid periods OR Partially Paid status)
    if ($status === 'remaining') {
        $formattedPayments = array_filter($formattedPayments, function($payment) {
            // Only include Confirmed bookings
            $isConfirmed = isset($payment['rental_status']) && $payment['rental_status'] === 'Confirmed';
            if (!$isConfirmed) {
                return false;
            }
            
            // Check if payment status is "Partially Paid" - include these
            $paymentStatus = isset($payment['payment_status']) ? strtolower($payment['payment_status']) : '';
            $isPartiallyPaid = $paymentStatus === 'partially paid' || 
                              $paymentStatus === 'partially_paid' ||
                              (strpos($paymentStatus, 'partially') !== false && strpos($paymentStatus, 'paid') !== false) ||
                              // Legacy support for old "Completed/Partially" status
                              $paymentStatus === 'completed/partially' || 
                              $paymentStatus === 'completed_partially' ||
                              (strpos($paymentStatus, 'completed') !== false && strpos($paymentStatus, 'partially') !== false);
            
            // Show bookings that are NOT fully paid (has remaining balance)
            $isFullyPaid = isset($payment['is_fully_paid']) && $payment['is_fully_paid'] === true;
            $hasUnpaidPeriods = isset($payment['unpaid_periods']) && intval($payment['unpaid_periods']) > 0;
            $hasRemainingMonths = isset($payment['remaining_months_to_pay']) && 
                                  $payment['remaining_months_to_pay'] !== null && 
                                  intval($payment['remaining_months_to_pay']) > 0;
            $hasRemainingAmount = isset($payment['remaining_amount_to_pay']) && 
                                 floatval($payment['remaining_amount_to_pay']) > 0;
            
            // Include if: (1) has "Partially Paid" status, OR (2) has remaining balance
            return $isPartiallyPaid || !$isFullyPaid || $hasUnpaidPeriods || $hasRemainingMonths || $hasRemainingAmount;
        });
        // Re-index array after filtering
        $formattedPayments = array_values($formattedPayments);
        if (function_exists('error_log')) {
            error_log("get_payment_status.php - Filtered to " . count($formattedPayments) . " Confirmed bookings with remaining balance or Partially Paid status");
        }
    }
    
    // Include debug info in response for troubleshooting
    $response = array(
        'success' => true,
        'data' => array(
            'payments' => $formattedPayments,
            'count' => count($formattedPayments)
        ),
        'debug' => array(
            'requested_owner_id' => $ownerId,
            'requested_status' => $status,
            'simple_query_count' => count($simplePayments),
            'main_query_count' => count($payments),
            'formatted_count' => count($formattedPayments)
        )
    );
    
    ob_clean();
    echo json_encode($response);
    ob_end_flush();
    
    // Log the final response for debugging
    if (function_exists('error_log')) {
        error_log("get_payment_status.php - Final response count: " . count($formattedPayments));
        if (count($formattedPayments) == 0) {
            error_log("get_payment_status.php - WARNING: Returning 0 payments for owner_id " . $ownerId . " with status " . $status);
        }
    }
    
} catch (PDOException $e) {
    // Log to error log (not output)
    if (function_exists('error_log')) {
        error_log("Database error in get_payment_status.php: " . $e->getMessage());
        error_log("Stack trace: " . $e->getTraceAsString());
    }
    ob_clean();
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error occurred. Please try again.'
    ));
    ob_end_flush();
} catch (Exception $e) {
    // Log to error log (not output)
    if (function_exists('error_log')) {
        error_log("Error in get_payment_status.php: " . $e->getMessage());
        error_log("Stack trace: " . $e->getTraceAsString());
    }
    ob_clean();
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error occurred. Please try again.'
    ));
    ob_end_flush();
} catch (Error $e) {
    // Catch fatal errors (PHP 7+)
    if (function_exists('error_log')) {
        error_log("Fatal error in get_payment_status.php: " . $e->getMessage());
        error_log("Stack trace: " . $e->getTraceAsString());
    }
    ob_clean();
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'error' => 'Fatal error occurred. Please try again.'
    ));
    ob_end_flush();
}
