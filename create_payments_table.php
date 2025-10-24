<?php
// Create Payments Table
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== CREATING PAYMENTS TABLE ===\n\n";

try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "✅ Database connection successful\n\n";
} catch (Exception $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "\n";
    exit;
}

try {
    // Start transaction
    $db->beginTransaction();
    
    echo "📋 Creating payments table...\n";
    
    // Check if payments table already exists
    $stmt = $db->prepare("SHOW TABLES LIKE 'payments'");
    $stmt->execute();
    if ($stmt->rowCount() > 0) {
        echo "⚠️  Payments table already exists. Dropping and recreating...\n";
        $db->exec("DROP TABLE payments");
    }
    
    // Create payments table
    $createTableSQL = "
    CREATE TABLE `payments` (
      `payment_id` int(11) NOT NULL AUTO_INCREMENT,
      `booking_id` int(11) DEFAULT NULL,
      `bill_id` int(11) DEFAULT NULL,
      `user_id` int(11) NOT NULL,
      `owner_id` int(11) NOT NULL,
      `payment_amount` decimal(10,2) NOT NULL,
      `payment_method` enum('Cash','GCash','Bank Transfer','Check') NOT NULL DEFAULT 'Cash',
      `payment_proof` text DEFAULT NULL,
      `payment_status` enum('Pending','Completed','Failed','Refunded') NOT NULL DEFAULT 'Pending',
      `payment_date` datetime NOT NULL DEFAULT current_timestamp(),
      `receipt_url` varchar(500) DEFAULT NULL,
      `notes` text DEFAULT NULL,
      `payment_month` varchar(7) NOT NULL,
      `payment_year` int(4) NOT NULL,
      `payment_month_number` int(2) NOT NULL,
      `is_monthly_payment` tinyint(1) NOT NULL DEFAULT 1,
      `total_months_required` int(3) DEFAULT NULL,
      `months_paid` int(3) DEFAULT 1,
      `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
      `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
      PRIMARY KEY (`payment_id`),
      KEY `booking_id` (`booking_id`),
      KEY `bill_id` (`bill_id`),
      KEY `user_id` (`user_id`),
      KEY `owner_id` (`owner_id`),
      KEY `payment_status` (`payment_status`),
      KEY `payment_date` (`payment_date`),
      KEY `payment_month` (`payment_month`),
      KEY `payment_year` (`payment_year`),
      KEY `payment_month_number` (`payment_month_number`),
      CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`) ON DELETE SET NULL,
      CONSTRAINT `payments_ibfk_2` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`bill_id`) ON DELETE SET NULL,
      CONSTRAINT `payments_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
      CONSTRAINT `payments_ibfk_4` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    ";
    
    $db->exec($createTableSQL);
    echo "✅ Payments table created successfully\n";
    
    // Add additional indexes
    echo "📋 Adding performance indexes...\n";
    $db->exec("CREATE INDEX `idx_payments_user_owner` ON `payments` (`user_id`, `owner_id`)");
    $db->exec("CREATE INDEX `idx_payments_status_date` ON `payments` (`payment_status`, `payment_date`)");
    $db->exec("CREATE INDEX `idx_payments_method` ON `payments` (`payment_method`)");
    $db->exec("CREATE INDEX `idx_payments_monthly_tracking` ON `payments` (`user_id`, `payment_month`, `payment_status`)");
    $db->exec("CREATE INDEX `idx_payments_owner_month` ON `payments` (`owner_id`, `payment_month`, `payment_status`)");
    echo "✅ Performance indexes added\n";
    
    // Add payment_id column to bills table if it doesn't exist
    echo "📋 Updating bills table...\n";
    $stmt = $db->prepare("SHOW COLUMNS FROM bills LIKE 'payment_id'");
    $stmt->execute();
    if ($stmt->rowCount() == 0) {
        $db->exec("ALTER TABLE `bills` ADD COLUMN `payment_id` int(11) DEFAULT NULL AFTER `status`");
        $db->exec("ALTER TABLE `bills` ADD KEY `payment_id` (`payment_id`)");
        $db->exec("ALTER TABLE `bills` ADD CONSTRAINT `bills_ibfk_2` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) ON DELETE SET NULL");
        echo "✅ Added payment_id column to bills table\n";
    } else {
        echo "⚠️  payment_id column already exists in bills table\n";
    }
    
    // Insert sample data
    echo "📋 Inserting sample payment data...\n";
    
    $samplePayments = [
        [1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-10-07 10:30:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 3, 1],
        [1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-11-07 10:30:00', NULL, 'Monthly rent payment - November 2025', '2025-11', 2025, 11, 1, 3, 1],
        [1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-12-07 10:30:00', NULL, 'Monthly rent payment - December 2025', '2025-12', 2025, 12, 1, 3, 1],
        [2, 2, 2, 1, 3500.00, 'GCash', 'GCash Ref: 1234567890', 'Completed', '2025-10-07 11:15:00', 'uploads/receipts/gcash_receipt_001.jpg', 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 2, 1],
        [2, 2, 2, 1, 3500.00, 'GCash', 'GCash Ref: 1234567891', 'Pending', '2025-11-07 11:15:00', 'uploads/receipts/gcash_receipt_002.jpg', 'Monthly rent payment - November 2025', '2025-11', 2025, 11, 1, 2, 1],
        [3, 3, 3, 2, 2500.00, 'Cash', 'Cash payment received', 'Completed', '2025-10-07 12:00:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 1, 1],
        [4, 4, 4, 1, 4000.00, 'GCash', 'GCash Ref: 1234567892', 'Pending', '2025-10-07 13:30:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 2, 1]
    ];
    
    $stmt = $db->prepare("
        INSERT INTO `payments` 
        (`booking_id`, `bill_id`, `user_id`, `owner_id`, `payment_amount`, `payment_method`, `payment_proof`, `payment_status`, `payment_date`, `receipt_url`, `notes`, `payment_month`, `payment_year`, `payment_month_number`, `is_monthly_payment`, `total_months_required`, `months_paid`) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ");
    
    foreach ($samplePayments as $payment) {
        $stmt->execute($payment);
    }
    
    echo "✅ Sample payment data inserted\n";
    
    // Commit transaction
    $db->commit();
    
    echo "\n🎉 Payments table created successfully!\n\n";
    
    // Show table structure
    echo "📋 Payments table structure:\n";
    $stmt = $db->prepare("DESCRIBE payments");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    
    // Show sample data
    echo "\n📊 Sample payment data:\n";
    $stmt = $db->prepare("SELECT * FROM payments LIMIT 3");
    $stmt->execute();
    $payments = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($payments as $payment) {
        echo "   - Payment ID: {$payment['payment_id']}, Amount: P{$payment['payment_amount']}, Method: {$payment['payment_method']}, Status: {$payment['payment_status']}\n";
    }
    
    echo "\n✅ Payments system is ready!\n";
    
} catch (Exception $e) {
    // Rollback transaction on error
    $db->rollback();
    echo "❌ Error creating payments table: " . $e->getMessage() . "\n";
}
?>
