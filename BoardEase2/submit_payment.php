<?php
// submit_payment.php
// Handle payment submission from boarder with payment proof upload

// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

// Log script execution start
error_log("=== submit_payment.php STARTED ===");
error_log("Request method: " . $_SERVER['REQUEST_METHOD']);
error_log("Script location: " . __FILE__);

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    error_log("Database connection successful");
    
    // Get POST data
    $inputData = json_decode(file_get_contents('php://input'), true);
    if (!$inputData) {
        $inputData = $_POST;
    }
    
    $bookingId = isset($inputData['booking_id']) ? intval($inputData['booking_id']) : 0;
    $paymentMethod = isset($inputData['payment_method']) ? trim($inputData['payment_method']) : '';
    $totalAmount = isset($inputData['total_amount']) ? floatval($inputData['total_amount']) : 0;
    $paymentProofBase64 = isset($inputData['payment_proof']) ? trim($inputData['payment_proof']) : '';
    $breakdownIdsJson = isset($inputData['breakdown_ids']) ? $inputData['breakdown_ids'] : '[]';
    
    error_log("submit_payment.php - Received booking_id: $bookingId, method: $paymentMethod, amount: $totalAmount");
    
    // Validation
    if ($bookingId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Booking ID is required.'
        ));
        exit();
    }
    
    if (empty($paymentMethod) || !in_array($paymentMethod, ['Cash', 'GCash'])) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Valid payment method (Cash or GCash) is required.'
        ));
        exit();
    }
    
    if ($totalAmount <= 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Total amount must be greater than 0.'
        ));
        exit();
    }
    
    if (empty($paymentProofBase64)) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Payment proof image is required.'
        ));
        exit();
    }
    
    // Parse breakdown IDs
    $breakdownIds = json_decode($breakdownIdsJson, true);
    if (!is_array($breakdownIds) || empty($breakdownIds)) {
        echo json_encode(array(
            'success' => false,
            'error' => 'At least one payment breakdown must be selected.'
        ));
        exit();
    }
    
    // Verify booking exists and get user_id and owner_id
    // Correct relationship: bookings -> room_units -> boarding_house_rooms -> boarding_houses
    $bookingSql = "SELECT b.user_id, bhr.bh_id, bh.user_id as owner_id 
                   FROM bookings b
                   INNER JOIN room_units ru ON b.room_id = ru.room_id
                   INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                   INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                   WHERE b.booking_id = :booking_id";
    $bookingStmt = $pdo->prepare($bookingSql);
    $bookingStmt->execute([':booking_id' => $bookingId]);
    $booking = $bookingStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$booking) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Booking not found.'
        ));
        exit();
    }
    
    $userId = $booking['user_id'];
    $ownerId = $booking['owner_id'];
    $bhId = $booking['bh_id'];
    
    // Process payment proof image
    $paymentProofPath = '';
    if (!empty($paymentProofBase64)) {
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
                echo json_encode(array(
                    'success' => false,
                    'error' => 'Invalid payment proof image format.'
                ));
                exit();
            }
            
            // Generate unique filename
            $filename = 'payment_proof_' . $bookingId . '_' . time() . '.jpg';
            
            // Use boardease_v3 directory structure
            // Path structure: boardease_v3/uploads/payment_proofs/
            // __DIR__ = boardease_v3/BoardEase2, so dirname(__DIR__) = boardease_v3
            $baseDir = dirname(__DIR__); // Gets boardease_v3 directory
            $uploadDir = $baseDir . DIRECTORY_SEPARATOR . 'uploads' . DIRECTORY_SEPARATOR . 'payment_proofs' . DIRECTORY_SEPARATOR;
            
            error_log("Base directory: $baseDir");
            error_log("Upload directory: $uploadDir");
            
            // Create directory if it doesn't exist
            if (!file_exists($uploadDir)) {
                mkdir($uploadDir, 0777, true);
                error_log("Created payment proof directory: $uploadDir");
            }
            
            $filePath = $uploadDir . $filename;
            
            // Save image
            if (file_put_contents($filePath, $imageData)) {
                // Store relative path from boardease_v3 root (for web access)
                $paymentProofPath = 'uploads/payment_proofs/' . $filename;
                error_log("Payment proof saved successfully: $paymentProofPath");
                error_log("Full file path: $filePath");
            } else {
                error_log("Warning: Failed to save payment proof image for booking_id: $bookingId");
                echo json_encode(array(
                    'success' => false,
                    'error' => 'Failed to save payment proof image.'
                ));
                exit();
            }
        } catch (Exception $e) {
            error_log("Error processing payment proof: " . $e->getMessage());
            echo json_encode(array(
                'success' => false,
                'error' => 'Error processing payment proof: ' . $e->getMessage()
            ));
            exit();
        }
    }
    
    // Start transaction
    $pdo->beginTransaction();
    
    try {
        // Create payment record with status 'Pending' (waiting for BH Owner approval)
        $paymentMonth = date('Y-m');
        $paymentYear = intval(date('Y'));
        $paymentMonthNumber = intval(date('m'));
        
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
        
        try {
            $insertPaymentStmt = $pdo->prepare($insertPaymentSql);
            $insertPaymentStmt->execute([
                ':booking_id' => $bookingId,
                ':user_id' => $userId,
                ':owner_id' => $ownerId,
                ':payment_amount' => $totalAmount,
                ':payment_method' => $paymentMethod,
                ':payment_proof' => $paymentProofPath,
                ':payment_month' => $paymentMonth,
                ':payment_year' => $paymentYear,
                ':payment_month_number' => $paymentMonthNumber
            ]);
            
            $paymentId = $pdo->lastInsertId();
            error_log("Payment record created with payment_id: $paymentId");
        } catch (PDOException $e) {
            error_log("Error inserting payment: " . $e->getMessage());
            error_log("SQL: " . $insertPaymentSql);
            throw $e; // Re-throw to be caught by outer catch
        }
        
        // Update payment_breakdowns to link to payment_id and set status to 'For Approval'
        // Note: Make sure you've run update_payment_breakdowns_enum.sql to add 'For Approval' to the enum
        $updateBreakdownSql = "
            UPDATE payment_breakdowns 
            SET payment_id = :payment_id,
                payment_status = 'For Approval',
                updated_at = NOW()
            WHERE breakdown_id = :breakdown_id
              AND booking_id = :booking_id
              AND is_paid = 0
        ";
        
        try {
            $updateBreakdownStmt = $pdo->prepare($updateBreakdownSql);
            $breakdownsUpdated = 0;
            
            foreach ($breakdownIds as $breakdownId) {
                try {
                    $updateBreakdownStmt->execute([
                        ':payment_id' => $paymentId,
                        ':breakdown_id' => intval($breakdownId),
                        ':booking_id' => $bookingId
                    ]);
                    
                    if ($updateBreakdownStmt->rowCount() > 0) {
                        $breakdownsUpdated++;
                    }
                } catch (PDOException $e) {
                    error_log("Error updating breakdown_id $breakdownId: " . $e->getMessage());
                    error_log("SQL: " . $updateBreakdownSql);
                    // Continue with other breakdowns
                }
            }
            
            error_log("Updated $breakdownsUpdated payment breakdowns to 'For Approval' status");
        } catch (PDOException $e) {
            error_log("Error preparing breakdown update: " . $e->getMessage());
            error_log("SQL: " . $updateBreakdownSql);
            throw $e; // Re-throw to be caught by outer catch
        }
        
        // Commit transaction
        $pdo->commit();
        
        echo json_encode(array(
            'success' => true,
            'message' => 'Payment submitted successfully. Waiting for owner approval.',
            'payment_id' => $paymentId,
            'breakdowns_updated' => $breakdownsUpdated
        ));
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $pdo->rollBack();
        error_log("Error in payment submission transaction: " . $e->getMessage());
        throw $e;
    }
    
} catch (PDOException $e) {
    error_log("Database error in submit_payment.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in submit_payment.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

