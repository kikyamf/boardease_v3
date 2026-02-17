<?php
header('Content-Type: application/json');

// Database configuration
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Update bookings table status enum
    $sql1 = "ALTER TABLE bookings MODIFY COLUMN booking_status ENUM('Pending', 'Approved', 'Confirmed', 'Upcoming', 'Active', 'Completed', 'Cancelled', 'Expired', 'Declined') NOT NULL DEFAULT 'Pending'";
    $pdo->exec($sql1);

    echo json_encode(['success' => true, 'message' => 'Booking status enum updated successfully.']);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
?>
