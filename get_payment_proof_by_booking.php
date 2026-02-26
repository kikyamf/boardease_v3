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

    $bookingId = isset($_GET['booking_id']) ? intval($_GET['booking_id']) : 0;

    if ($bookingId === 0) {
        echo json_encode(['success' => false, 'error' => 'Booking ID is required.']);
        exit;
    }

    // Get base URL for images
    $baseUrl = 'https://boardease.calapebohol.com/';

    // Fetch the latest pending payment for this booking
    $sql = "
        SELECT payment_proof
        FROM payments 
        WHERE booking_id = :booking_id 
        AND payment_status = 'Pending'
        ORDER BY payment_id DESC 
        LIMIT 1
    ";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([':booking_id' => $bookingId]);
    $payment = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($payment) {
        $proofUrl = $payment['payment_proof'] ? $baseUrl . "get_payment_proof.php?path=" . urlencode($payment['payment_proof']) : null;
        echo json_encode([
            'success' => true,
            'payment_proof_url' => $proofUrl,
            'receipt_url' => $proofUrl
        ]);
    } else {
        // Try to fetch ANY payment if no pending one is found (fallback)
        $sqlFallback = "
            SELECT payment_proof
            FROM payments 
            WHERE booking_id = :booking_id 
            ORDER BY payment_id DESC 
            LIMIT 1
        ";
        $stmtFallback = $pdo->prepare($sqlFallback);
        $stmtFallback->execute([':booking_id' => $bookingId]);
        $paymentFallback = $stmtFallback->fetch(PDO::FETCH_ASSOC);

        if ($paymentFallback) {
             $proofUrlFallback = $paymentFallback['payment_proof'] ? $baseUrl . "get_payment_proof.php?path=" . urlencode($paymentFallback['payment_proof']) : null;
             echo json_encode([
                'success' => true,
                'payment_proof_url' => $proofUrlFallback,
                'receipt_url' => $proofUrlFallback
            ]);
        } else {
            echo json_encode(['success' => false, 'error' => 'No payment proof found for this booking.']);
        }
    }

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
