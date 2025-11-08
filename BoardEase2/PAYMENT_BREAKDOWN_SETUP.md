# Payment Breakdown Setup Guide

## Overview
This feature allows boarders to select which payment periods (months/days) they want to pay in advance for multi-month stays. The system tracks these selections in the database for super admin dashboard display and payment due notifications.

## Database Setup

### Step 1: Create the Payment Breakdowns Table

1. Open phpMyAdmin or your MySQL client
2. Select the `boardease2` database
3. Go to the SQL tab
4. Run the SQL script from `create_payment_breakdowns_table.sql`:

```sql
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

CREATE INDEX `idx_admin_dashboard` ON `payment_breakdowns` (`payment_status`, `due_date`, `is_selected`, `is_paid`);
```

### Step 2: Verify Table Creation

Run this query to verify the table was created:

```sql
DESCRIBE payment_breakdowns;
```

You should see all the columns listed above.

## How It Works

### For Boarders (Android App)

1. **Multi-Month Stay Detection**: If a boarder's stay is more than 1 month (or has multiple periods), checkboxes appear in the "Amount to Pay" section.

2. **Payment Period Breakdown**:
   - **Months**: Each 30-day period becomes a "1st month", "2nd month", etc.
   - **Remaining Days**: Any days less than 30 become a separate period (e.g., "3 days")

3. **Sequential Selection**:
   - First checkbox is always enabled and checked by default
   - Subsequent checkboxes are only enabled if the previous one is checked
   - If a checkbox is unchecked, all following checkboxes are automatically unchecked

4. **Total Calculation**: The total payment amount updates dynamically based on selected checkboxes.

### Example Scenario

**Stay Duration**: 2 months and 3 days (Jan 1 - Mar 3)
- Start date is NOT counted (Jan 1)
- Days counted: Jan 2 - Mar 3 = 63 days

**Payment Breakdown**:
- ✅ 1st month (Jan 2 - Jan 31) - ₱3,500
- ☐ 2nd month (Feb 1 - Mar 2) - ₱3,500
- ☐ 3 days (Mar 3) - ₱350

**If boarder selects only 1st month**: Total = ₱3,500
**If boarder selects all**: Total = ₱7,350

### For Super Admin Dashboard

The `payment_breakdowns` table can be queried to:

1. **Track Payment Schedules**:
```sql
SELECT 
    pb.*,
    b.start_date,
    b.end_date,
    u.email as boarder_email
FROM payment_breakdowns pb
JOIN bookings b ON pb.booking_id = b.booking_id
JOIN users u ON b.user_id = u.user_id
WHERE pb.is_selected = 1
AND pb.is_paid = 0
ORDER BY pb.due_date;
```

2. **Find Overdue Payments**:
```sql
SELECT * FROM payment_breakdowns
WHERE payment_status = 'Overdue'
AND is_selected = 1
AND is_paid = 0;
```

3. **Payment Due Notifications**:
```sql
SELECT * FROM payment_breakdowns
WHERE due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
AND is_selected = 1
AND is_paid = 0
AND payment_status = 'Pending';
```

## PHP Endpoint Updates

The `create_booking.php` endpoint now accepts:
- `payment_breakdown`: JSON array of payment periods with selection status

The endpoint will:
1. Create the booking
2. Create the main payment record
3. Insert payment breakdown records for each period

## Testing

1. **Test with 1 month stay**: Should show simple payment (no checkboxes)
2. **Test with 2 months**: Should show 2 checkboxes (1st month, 2nd month)
3. **Test with 2 months 3 days**: Should show 3 checkboxes (1st month, 2nd month, 3 days)
4. **Test sequential selection**: Try unchecking 1st month - 2nd month should become disabled
5. **Test total calculation**: Select different combinations and verify total updates

## Database Access

Your database is accessible at:
- **Host**: localhost (or 192.168.1.9 from network)
- **Database**: boardease2
- **Username**: boardease
- **Password**: boardease

## Next Steps

1. ✅ Create the `payment_breakdowns` table
2. ✅ Update `create_booking.php` to save payment breakdowns
3. ⏳ Create super admin dashboard queries
4. ⏳ Implement payment due notifications
5. ⏳ Add payment tracking UI for super admin

