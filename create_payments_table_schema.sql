-- Payments Table Schema
-- This table will store individual payment transactions

CREATE TABLE `payments` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `booking_id` int(11) DEFAULT NULL,
  `bill_id` int(11) DEFAULT NULL, -- Reference to bills table for backward compatibility
  `user_id` int(11) NOT NULL, -- Boarder who made the payment
  `owner_id` int(11) NOT NULL, -- Owner who received the payment
  `payment_amount` decimal(10,2) NOT NULL,
  `payment_method` enum('Cash','GCash','Bank Transfer','Check') NOT NULL DEFAULT 'Cash',
  `payment_proof` text DEFAULT NULL, -- Description or reference number
  `payment_status` enum('Pending','Completed','Failed','Refunded') NOT NULL DEFAULT 'Pending',
  `payment_date` datetime NOT NULL DEFAULT current_timestamp(),
  `receipt_url` varchar(500) DEFAULT NULL, -- URL to receipt image
  `notes` text DEFAULT NULL,
  `payment_month` varchar(7) NOT NULL, -- Format: YYYY-MM (e.g., 2025-10)
  `payment_year` int(4) NOT NULL, -- Year for easier querying
  `payment_month_number` int(2) NOT NULL, -- Month number (1-12)
  `is_monthly_payment` tinyint(1) NOT NULL DEFAULT 1, -- 1 for monthly, 0 for one-time
  `total_months_required` int(3) DEFAULT NULL, -- Total months in rental period
  `months_paid` int(3) DEFAULT 1, -- Number of months this payment covers
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Add indexes for better performance
CREATE INDEX `idx_payments_user_owner` ON `payments` (`user_id`, `owner_id`);
CREATE INDEX `idx_payments_status_date` ON `payments` (`payment_status`, `payment_date`);
CREATE INDEX `idx_payments_method` ON `payments` (`payment_method`);
CREATE INDEX `idx_payments_monthly_tracking` ON `payments` (`user_id`, `payment_month`, `payment_status`);
CREATE INDEX `idx_payments_owner_month` ON `payments` (`owner_id`, `payment_month`, `payment_status`);

-- Sample data for testing with monthly tracking
INSERT INTO `payments` (`booking_id`, `bill_id`, `user_id`, `owner_id`, `payment_amount`, `payment_method`, `payment_proof`, `payment_status`, `payment_date`, `receipt_url`, `notes`, `payment_month`, `payment_year`, `payment_month_number`, `is_monthly_payment`, `total_months_required`, `months_paid`) VALUES
(1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-10-07 10:30:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 3, 1),
(1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-11-07 10:30:00', NULL, 'Monthly rent payment - November 2025', '2025-11', 2025, 11, 1, 3, 1),
(1, 1, 1, 1, 5000.00, 'Cash', 'Cash payment received', 'Completed', '2025-12-07 10:30:00', NULL, 'Monthly rent payment - December 2025', '2025-12', 2025, 12, 1, 3, 1),
(2, 2, 2, 1, 3500.00, 'GCash', 'GCash Ref: 1234567890', 'Completed', '2025-10-07 11:15:00', 'uploads/receipts/gcash_receipt_001.jpg', 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 2, 1),
(2, 2, 2, 1, 3500.00, 'GCash', 'GCash Ref: 1234567891', 'Pending', '2025-11-07 11:15:00', 'uploads/receipts/gcash_receipt_002.jpg', 'Monthly rent payment - November 2025', '2025-11', 2025, 11, 1, 2, 1),
(3, 3, 3, 2, 2500.00, 'Cash', 'Cash payment received', 'Completed', '2025-10-07 12:00:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 1, 1),
(4, 4, 4, 1, 4000.00, 'GCash', 'GCash Ref: 1234567892', 'Pending', '2025-10-07 13:30:00', NULL, 'Monthly rent payment - October 2025', '2025-10', 2025, 10, 1, 2, 1);

-- Update the bills table to reference payments
ALTER TABLE `bills` ADD COLUMN `payment_id` int(11) DEFAULT NULL AFTER `status`;
ALTER TABLE `bills` ADD KEY `payment_id` (`payment_id`);
ALTER TABLE `bills` ADD CONSTRAINT `bills_ibfk_2` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) ON DELETE SET NULL;
