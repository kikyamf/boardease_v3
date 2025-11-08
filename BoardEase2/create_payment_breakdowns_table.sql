-- Table for tracking payment breakdown selections for multi-month stays
-- This allows boarders to choose which months/days to pay in advance
-- Super admin can use this to track payment schedules and send notifications

CREATE TABLE IF NOT EXISTS `payment_breakdowns` (
  `breakdown_id` int(11) NOT NULL AUTO_INCREMENT,
  `booking_id` int(11) NOT NULL,
  `payment_id` int(11) DEFAULT NULL COMMENT 'Links to payments table if payment is made',
  `period_type` enum('month','days') NOT NULL COMMENT 'Type of period: month or days',
  `period_number` int(3) NOT NULL COMMENT 'Month number (1, 2, 3...) or 0 for days',
  `period_label` varchar(50) NOT NULL COMMENT 'Display label: "1st month", "2nd month", "3 days", etc.',
  `period_start_date` date NOT NULL COMMENT 'Start date of this payment period',
  `period_end_date` date NOT NULL COMMENT 'End date of this payment period',
  `amount` decimal(10,2) NOT NULL COMMENT 'Amount for this period',
  `is_selected` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether boarder selected this period for payment',
  `is_paid` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether this period has been paid',
  `due_date` date DEFAULT NULL COMMENT 'Due date for this payment period',
  `payment_status` enum('Pending','Paid','Overdue','Cancelled') NOT NULL DEFAULT 'Pending',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`breakdown_id`),
  KEY `booking_id` (`booking_id`),
  KEY `payment_id` (`payment_id`),
  KEY `is_selected` (`is_selected`),
  KEY `is_paid` (`is_paid`),
  KEY `payment_status` (`payment_status`),
  KEY `due_date` (`due_date`),
  KEY `idx_booking_selected` (`booking_id`,`is_selected`),
  KEY `idx_booking_paid` (`booking_id`,`is_paid`),
  KEY `idx_payment_status_due` (`payment_status`,`due_date`),
  CONSTRAINT `fk_breakdown_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_breakdown_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Add index for super admin dashboard queries
CREATE INDEX `idx_admin_dashboard` ON `payment_breakdowns` (`payment_status`, `due_date`, `is_selected`, `is_paid`);

-- Example usage:
-- For a 2 months and 3 days stay:
-- breakdown_id | booking_id | period_type | period_number | period_label | amount | is_selected
-- 1            | 100        | month       | 1             | 1st month    | 3500  | 1
-- 2            | 100        | month       | 2             | 2nd month    | 3500  | 1
-- 3            | 100        | days        | 0             | 3 days       | 350   | 0

