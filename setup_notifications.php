<?php
// Setup notifications table and test data
require_once 'dbConfig.php';

echo "Setting up notifications system...\n";

try {
    // Check if notifications table exists
    $result = $conn->query("SHOW TABLES LIKE 'notifications'");
    if ($result->num_rows == 0) {
        echo "Creating notifications table...\n";
        
        $create_sql = "CREATE TABLE `notifications` (
            `notif_id` int(11) NOT NULL AUTO_INCREMENT,
            `user_id` int(11) NOT NULL,
            `notif_title` varchar(150) NOT NULL,
            `notif_message` text NOT NULL,
            `notif_type` enum('booking','payment','announcement','maintenance','general') DEFAULT 'general',
            `notif_status` enum('unread','read') DEFAULT 'unread',
            `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp(),
            PRIMARY KEY (`notif_id`),
            KEY `user_id` (`user_id`),
            KEY `notif_status` (`notif_status`),
            KEY `notif_type` (`notif_type`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
        
        if ($conn->query($create_sql) === TRUE) {
            echo "✓ Notifications table created successfully!\n";
        } else {
            echo "✗ Error creating table: " . $conn->error . "\n";
            exit;
        }
    } else {
        echo "✓ Notifications table already exists.\n";
    }
    
    // Insert test notifications
    echo "Inserting test notifications...\n";
    
    $test_notifications = [
        [1, 'Welcome!', 'Welcome to BoardEase! Your account is ready.', 'general', 'unread'],
        [1, 'New Booking', 'You have a new booking request from John Doe.', 'booking', 'unread'],
        [1, 'Payment Received', 'Payment of ₱5,000 received from Jane Smith.', 'payment', 'unread']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($test_notifications as $notif) {
        $stmt->bind_param("issss", $notif[0], $notif[1], $notif[2], $notif[3], $notif[4]);
        if ($stmt->execute()) {
            echo "✓ Test notification inserted: {$notif[1]}\n";
        } else {
            echo "✗ Error inserting notification: " . $stmt->error . "\n";
        }
    }
    
    // Check unread count
    $count_sql = "SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = 1 AND notif_status = 'unread'";
    $result = $conn->query($count_sql);
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "\n✓ Setup complete! User 1 has {$unread_count} unread notifications.\n";
    echo "You can now test the notification badge in your Android app!\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















