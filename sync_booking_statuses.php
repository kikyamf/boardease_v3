<?php
/**
 * sync_booking_statuses.php - Automated Booking Status Sync
 * This script should be run daily at midnight (e.g., via Cron Job)
 */

// Database configuration
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $today = date('Y-m-d');
    $pdo->beginTransaction();

    // 1. Transition Confirmed -> Upcoming (if start date is in the future)
    $sql1 = "UPDATE bookings SET booking_status = 'Upcoming' 
             WHERE booking_status = 'Confirmed' AND start_date > ?";
    $stmt1 = $pdo->prepare($sql1);
    $stmt1->execute([$today]);
    $upcomingCount = $stmt1->rowCount();

    // 2. Transition Confirmed/Upcoming -> Active (REMOVED - now handled via manual Check In)
    /*
    $sql2 = "UPDATE bookings SET booking_status = 'Active' 
             WHERE booking_status IN ('Confirmed', 'Upcoming') 
             AND start_date <= ? AND end_date >= ?";
    $stmt2 = $pdo->prepare($sql2);
    $stmt2->execute([$today, $today]);
    $activeCount = $stmt2->rowCount();
    */
    $activeCount = 0;

    // 3. Transition Confirmed/Upcoming/Active -> Completed (if end date passed)
    $sql3 = "UPDATE bookings SET booking_status = 'Completed' 
             WHERE booking_status IN ('Confirmed', 'Upcoming', 'Active') 
             AND end_date < ?";
    $stmt3 = $pdo->prepare($sql3);
    $stmt3->execute([$today]);
    $completedCount = $stmt3->rowCount();

    // 4. Update Room Status (Occupied)
    // Find all rooms with at least one ACTIVE booking
    $sql4 = "UPDATE room_units SET status = 'Occupied' 
             WHERE room_id IN (SELECT room_id FROM bookings WHERE booking_status = 'Active')";
    $pdo->exec($sql4);

    // 5. Update Room Status (Available)
    // Find rooms that NO LONGER have any ACTIVE bookings and set them back to Available
    $sql5 = "UPDATE room_units ru 
             SET ru.status = 'Available' 
             WHERE ru.room_id NOT IN (SELECT room_id FROM bookings WHERE booking_status = 'Active')";
    $pdo->exec($sql5);

    $pdo->commit();

    echo json_encode([
        'success' => true,
        'message' => 'Sync completed.',
        'transitions' => [
            'upcoming' => $upcomingCount,
            'active' => $activeCount,
            'completed' => $completedCount
        ]
    ]);

} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
?>
