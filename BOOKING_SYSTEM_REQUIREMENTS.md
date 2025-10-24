# 📋 BOOKING SYSTEM REQUIREMENTS

## 🗄️ REQUIRED DATABASE TABLES

The booking system requires the following database tables to be created:

### 1. **bookings** table
```sql
CREATE TABLE `bookings` (
  `booking_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `booking_status` enum('Pending','Approved','Declined','Completed','Cancelled') DEFAULT 'Pending',
  `payment_status` enum('Pending','Paid','Overdue','Refunded') DEFAULT 'Pending',
  `notes` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`booking_id`),
  KEY `user_id` (`user_id`),
  KEY `room_id` (`room_id`),
  KEY `booking_status` (`booking_status`)
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

### 7. **notifications** table (for booking notifications)
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

The following PHP files are needed for the booking system to work:

### Core API Files:
1. **`get_bookings.php`** - Get all bookings for a user
2. **`get_approved_bookings.php`** - Get approved bookings only
3. **`get_pending_bookings.php`** - Get pending bookings only
4. **`get_booking_history.php`** - Get completed/expired/cancelled bookings
5. **`get_booking_details.php`** - Get detailed information for a specific booking
6. **`approve_booking.php`** - Approve a pending booking
7. **`decline_booking.php`** - Decline a pending booking

### Helper Files:
8. **`db_helper.php`** - Database connection and helper functions
9. **`auto_notify_booking.php`** - Automatic booking notifications
10. **`notification_helper.php`** - Notification system helper

### Test Files (Optional):
11. **`test_booking_apis.php`** - Test all booking APIs
12. **`test_complete_booking_system.php`** - Complete system test

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
- `GET get_bookings.php?user_id=1&user_type=owner`
- `GET get_approved_bookings.php?user_id=1&user_type=owner`
- `GET get_pending_bookings.php?user_id=1&user_type=owner`
- `GET get_booking_history.php?user_id=1&user_type=owner`
- `GET get_booking_details.php?booking_id=1`

### POST Endpoints:
- `POST approve_booking.php` (JSON: booking_id, owner_id)
- `POST decline_booking.php` (JSON: booking_id, owner_id, reason)

## 📱 Android Integration

The Android app is already configured to use these APIs:
- **ApprovedBookingsFragment** → `get_approved_bookings.php`
- **PendingBookingsFragment** → `get_pending_bookings.php`
- **BookingHistoryFragment** → `get_booking_history.php`
- **BookingDetailsActivity** → `get_booking_details.php`

## ✅ VERIFICATION CHECKLIST

- [ ] All database tables created
- [ ] All PHP files present
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] Android app connecting to APIs
- [ ] Notifications working
- [ ] Approve/decline functionality working

## 🎯 RESULT

Once all requirements are met, your booking system will have:
- ✅ Real-time booking data from database
- ✅ Professional loading dialogs
- ✅ Real approve/decline functionality
- ✅ Complete booking information
- ✅ Automatic notifications
- ✅ Error handling and user feedback
- ✅ Professional UI/UX

**Your booking system will be fully functional! 🎉**














