# 💰 PAYMENT STATUS SYSTEM REQUIREMENTS

## 🗄️ REQUIRED DATABASE TABLES

The payment status system requires the following database tables to be created:

### 1. **bookings** table (Enhanced for Payment)
```sql
CREATE TABLE `bookings` (
  `booking_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `booking_status` enum('Pending','Approved','Declined','Completed','Cancelled') DEFAULT 'Pending',
  `payment_status` enum('Pending','Paid','Overdue','Failed','Refunded') DEFAULT 'Pending',
  `payment_due_date` date,
  `payment_date` timestamp NULL,
  `payment_method` varchar(50),
  `payment_reference` varchar(100),
  `payment_notes` text,
  `notes` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`booking_id`),
  KEY `user_id` (`user_id`),
  KEY `room_id` (`room_id`),
  KEY `booking_status` (`booking_status`),
  KEY `payment_status` (`payment_status`),
  KEY `payment_due_date` (`payment_due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 2. **users** table
```sql
CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `reg_id` int(11) NOT NULL,
  `profile_picture` varchar(255),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`user_id`),
  KEY `reg_id` (`reg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 3. **registration** table
```sql
CREATE TABLE `registration` (
  `reg_id` int(11) NOT NULL AUTO_INCREMENT,
  `f_name` varchar(100) NOT NULL,
  `l_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone_number` varchar(20),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`reg_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 4. **room_units** table
```sql
CREATE TABLE `room_units` (
  `room_id` int(11) NOT NULL AUTO_INCREMENT,
  `room_number` varchar(20) NOT NULL,
  `bhr_id` int(11) NOT NULL,
  `room_status` enum('Available','Occupied','Maintenance','Reserved') DEFAULT 'Available',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`room_id`),
  KEY `bhr_id` (`bhr_id`),
  KEY `room_status` (`room_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 5. **boarding_house_rooms** table
```sql
CREATE TABLE `boarding_house_rooms` (
  `bhr_id` int(11) NOT NULL AUTO_INCREMENT,
  `bh_id` int(11) NOT NULL,
  `room_category` varchar(100) NOT NULL,
  `room_price` decimal(10,2) NOT NULL,
  `room_description` text,
  `room_capacity` int(11) DEFAULT 1,
  `room_amenities` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`bhr_id`),
  KEY `bh_id` (`bh_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 6. **boarding_houses** table
```sql
CREATE TABLE `boarding_houses` (
  `bh_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `bh_name` varchar(200) NOT NULL,
  `bh_address` text NOT NULL,
  `bh_contact` varchar(20),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`bh_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 7. **notifications** table (for payment notifications)
```sql
CREATE TABLE `notifications` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## 📁 REQUIRED PHP FILES

The following PHP files are needed for the payment status system to work:

### Core API Files:
1. **`get_payment_status.php`** - Get payment status for bookings with filtering
2. **`update_payment_status.php`** - Update payment status for a booking
3. **`get_payment_history.php`** - Get payment history with pagination
4. **`get_payment_summary.php`** - Get payment statistics and summary
5. **`mark_payments_overdue.php`** - Mark payments as overdue automatically

### Helper Files:
6. **`db_helper.php`** - Database connection and helper functions
7. **`auto_notify_payment.php`** - Automatic payment notifications
8. **`notification_helper.php`** - Notification system helper

### Test Files (Optional):
9. **`test_payment_apis.php`** - Test all payment APIs

## 🔧 SETUP INSTRUCTIONS

### 1. Database Setup
```bash
# Run the SQL commands above to create all required tables
# Make sure your database connection is configured in db_helper.php
```

### 2. PHP Files Setup
```bash
# All PHP files are already created in your project
# Make sure they're in the correct directory (same as your Android app's API calls)
```

### 3. Configuration
```php
// Update db_helper.php with your database credentials
// Update API URLs in Android app to match your server
```

## 🚀 API ENDPOINTS

### GET Endpoints:
- `GET get_payment_status.php?user_id=1&user_type=owner&status=all`
- `GET get_payment_status.php?user_id=1&user_type=owner&status=pending`
- `GET get_payment_status.php?user_id=1&user_type=owner&status=paid`
- `GET get_payment_status.php?user_id=1&user_type=owner&status=overdue`
- `GET get_payment_history.php?user_id=1&user_type=owner&limit=20&offset=0`
- `GET get_payment_summary.php?user_id=1&user_type=owner&period=month`

### POST Endpoints:
- `POST update_payment_status.php` (JSON: booking_id, payment_status, payment_method, payment_reference, payment_notes, updated_by)
- `POST mark_payments_overdue.php` (JSON: user_id, user_type)

## 📱 Android Integration

The Android app can be configured to use these APIs for:
- **Payment Status Tracking** → `get_payment_status.php`
- **Payment History** → `get_payment_history.php`
- **Payment Summary** → `get_payment_summary.php`
- **Update Payment Status** → `update_payment_status.php`

## 💰 PAYMENT FEATURES

### Payment Status Types:
- **Pending** - Payment not yet received
- **Paid** - Payment received and confirmed
- **Overdue** - Payment past due date
- **Failed** - Payment failed or rejected
- **Refunded** - Payment refunded

### Payment Methods:
- **Cash** - Cash payment
- **GCash** - GCash mobile payment
- **Bank Transfer** - Bank transfer
- **PayPal** - PayPal payment
- **Credit Card** - Credit card payment

### Payment Tracking:
- **Payment Due Date** - When payment is due
- **Payment Date** - When payment was received
- **Payment Method** - How payment was made
- **Payment Reference** - Transaction reference number
- **Payment Notes** - Additional payment notes

## 🔔 NOTIFICATION FEATURES

### Automatic Notifications:
- **Payment Received** - Notify when payment is confirmed
- **Payment Overdue** - Notify when payment is past due
- **Payment Failed** - Notify when payment fails
- **Payment Refunded** - Notify when payment is refunded

## ✅ VERIFICATION CHECKLIST

- [ ] All database tables created
- [ ] All PHP files present
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] Payment status filtering working
- [ ] Payment history pagination working
- [ ] Payment summary statistics working
- [ ] Payment notifications working
- [ ] Overdue payment detection working

## 🎯 RESULT

Once all requirements are met, your payment status system will have:
- ✅ Real-time payment status tracking
- ✅ Payment history with pagination
- ✅ Payment statistics and summaries
- ✅ Automatic overdue detection
- ✅ Payment method tracking
- ✅ Payment reference tracking
- ✅ Automatic notifications
- ✅ Professional UI/UX

**Your payment status system will be fully functional! 🎉**














