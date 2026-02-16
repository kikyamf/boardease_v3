<?php
// BoardEase2/apply_expiration_schema.php
// Script to update the database schema for booking expiration logic on Live DB

header('Content-Type: application/json');

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        echo json_encode(array('success' => false, 'error' => 'Database connection failed: ' . $conn->connect_error));
        exit;
    }
    
    echo "Connected to live database successfully.<br>";
    
    // 1. Update the bookings table enum and add approval_date
    $sql = "ALTER TABLE `bookings` 
            MODIFY COLUMN `booking_status` ENUM('Pending', 'Confirmed', 'Cancelled', 'Completed', 'Approved', 'Expired') NOT NULL DEFAULT 'Pending',
            ADD COLUMN IF NOT EXISTS `approval_date` DATETIME NULL AFTER `booking_date`";
    
    if ($conn->query($sql) === TRUE) {
        echo "Updated 'bookings' table schema (added 'Approved', 'Expired' statuses and 'approval_date' column).<br>";
    } else {
        echo "Error updating table: " . $conn->error . "<br>";
    }
    
    echo "Database schema update complete.";
    $conn->close();
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
?>
