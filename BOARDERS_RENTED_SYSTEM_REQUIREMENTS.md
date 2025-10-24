# 🏠 BOARDERS RENTED SYSTEM REQUIREMENTS

## 🗄️ REQUIRED DATABASE TABLES

The boarders rented system requires the following database tables to be created:

### 1. **bookings** table (Enhanced for Boarder Management)
```sql
CREATE TABLE `bookings` (
  `booking_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `booking_status` enum('Pending','Approved','Declined','Completed','Cancelled','Problem') DEFAULT 'Pending',
  `payment_status` enum('Pending','Paid','Overdue','Failed','Refunded') DEFAULT 'Pending',
  `payment_due_date` date,
  `payment_date` timestamp NULL,
  `payment_method` varchar(50),
  `payment_reference` varchar(100),
  `payment_notes` text,
  `check_in_date` timestamp NULL,
  `check_out_date` timestamp NULL,
  `emergency_contact_name` varchar(100),
  `emergency_contact_phone` varchar(20),
  `rental_agreement_signed` boolean DEFAULT FALSE,
  `deposit_amount` decimal(10,2),
  `deposit_status` enum('Pending','Paid','Returned','Forfeited') DEFAULT 'Pending',
  `room_condition_on_checkout` text,
  `notes` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`booking_id`),
  KEY `user_id` (`user_id`),
  KEY `room_id` (`room_id`),
  KEY `booking_status` (`booking_status`),
  KEY `payment_status` (`payment_status`),
  KEY `check_in_date` (`check_in_date`),
  KEY `check_out_date` (`check_out_date`)
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

### 3. **registration** table (Enhanced for Boarder Details)
```sql
CREATE TABLE `registration` (
  `reg_id` int(11) NOT NULL AUTO_INCREMENT,
  `f_name` varchar(100) NOT NULL,
  `l_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone_number` varchar(20),
  `birth_date` date,
  `gender` enum('Male','Female','Other'),
  `address` text,
  `university` varchar(200),
  `student_id` varchar(50),
  `emergency_contact_name` varchar(100),
  `emergency_contact_phone` varchar(20),
  `emergency_contact_relationship` varchar(50),
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

### 7. **notifications** table (for boarder notifications)
```sql
CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `notif_title` varchar(150) NOT NULL,
  `notif_message` text NOT NULL,
  `notif_type` enum('booking','payment','announcement','maintenance','general','boarder') DEFAULT 'general',
  `notif_status` enum('unread','read') DEFAULT 'unread',
  `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`notif_id`),
  KEY `user_id` (`user_id`),
  KEY `notif_status` (`notif_status`),
  KEY `notif_type` (`notif_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## 📁 REQUIRED PHP FILES

The following PHP files are needed for the boarders rented system to work:

### Core API Files:
1. **`get_current_boarders.php`** - Get currently active boarders with filtering
2. **`get_boarders_history.php`** - Get past boarders (completed rentals) with pagination
3. **`manage_boarder_checkin_checkout.php`** - Handle check-in/check-out processes
4. **`get_boarder_details.php`** - Get detailed information for a specific boarder
5. **`get_boarders_summary.php`** - Get boarders statistics and summary

### Helper Files:
6. **`db_helper.php`** - Database connection and helper functions
7. **`auto_notify_booking.php`** - Automatic boarder notifications
8. **`notification_helper.php`** - Notification system helper

### Test Files (Optional):
9. **`test_boarders_rented_apis.php`** - Test all boarders rented APIs

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
- `GET get_current_boarders.php?user_id=1&status=all`
- `GET get_current_boarders.php?user_id=1&status=active`
- `GET get_current_boarders.php?user_id=1&status=checking_out`
- `GET get_current_boarders.php?user_id=1&status=problem`
- `GET get_boarders_history.php?user_id=1&status=all&limit=20&offset=0`
- `GET get_boarders_history.php?user_id=1&status=completed&limit=20&offset=0`
- `GET get_boarder_details.php?booking_id=1`
- `GET get_boarders_summary.php?user_id=1&period=month`

### POST Endpoints:
- `POST manage_boarder_checkin_checkout.php` (JSON: booking_id, action, notes, updated_by)

## 📱 Android Integration

The Android app can be configured to use these APIs for:
- **Current Boarders Management** → `get_current_boarders.php`
- **Boarders History** → `get_boarders_history.php`
- **Boarder Details** → `get_boarder_details.php`
- **Check-in/Check-out** → `manage_boarder_checkin_checkout.php`
- **Boarders Summary** → `get_boarders_summary.php`

## 🏠 BOARDER MANAGEMENT FEATURES

### Boarder Status Types:
- **Active** - Currently renting and in good standing
- **Checking Out** - Due to check out within 7 days
- **Problem** - Payment overdue or other issues
- **Completed** - Successfully completed rental
- **Cancelled** - Rental was cancelled
- **Expired** - Rental period expired

### Check-in/Check-out Process:
- **Check-in** - Boarder arrives and moves in
- **Check-out** - Boarder leaves and moves out
- **Room Condition** - Track room condition on check-out
- **Deposit Management** - Handle security deposits
- **Rental Agreement** - Track signed agreements

### Boarder Information:
- **Personal Details** - Name, email, phone, address
- **University Info** - University, student ID
- **Emergency Contacts** - Emergency contact details
- **Rental Details** - Room, dates, payment status
- **Check-in/Check-out Dates** - Move-in and move-out dates

## 🔔 NOTIFICATION FEATURES

### Automatic Notifications:
- **Check-in Completed** - Notify when boarder checks in
- **Check-out Completed** - Notify when boarder checks out
- **Check-out Reminder** - Notify boarders checking out soon
- **Payment Overdue** - Notify about overdue payments
- **Rental Agreement** - Notify about unsigned agreements

## 📊 ANALYTICS FEATURES

### Boarders Summary:
- **Total Active Boarders** - Current number of boarders
- **Occupancy Rate** - Percentage of rooms occupied
- **Revenue Tracking** - Total rental income
- **Check-out Alerts** - Boarders checking out soon
- **Problem Boarders** - Boarders with issues

### Historical Data:
- **Completed Rentals** - Past successful rentals
- **Cancelled Rentals** - Cancelled rental attempts
- **Average Stay Duration** - How long boarders typically stay
- **Revenue Trends** - Income over time

## ✅ VERIFICATION CHECKLIST

- [ ] All database tables created
- [ ] All PHP files present
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] Current boarders filtering working
- [ ] Boarders history pagination working
- [ ] Check-in/check-out functionality working
- [ ] Boarder details retrieval working
- [ ] Boarders summary statistics working
- [ ] Boarder notifications working

## 🎯 RESULT

Once all requirements are met, your boarders rented system will have:
- ✅ Real-time boarder tracking
- ✅ Check-in/check-out management
- ✅ Boarder history with pagination
- ✅ Detailed boarder profiles
- ✅ Boarder statistics and analytics
- ✅ Automatic notifications
- ✅ Deposit management
- ✅ Room condition tracking
- ✅ Professional UI/UX

**Your boarders rented system will be fully functional! 🎉**














