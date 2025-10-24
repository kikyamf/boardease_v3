# 🔧 MAINTENANCE REQUESTS SYSTEM REQUIREMENTS

## 🗄️ REQUIRED DATABASE TABLES

The maintenance requests system requires the following database tables to be created:

### 1. **maintenance_requests** table (Main Table)
```sql
CREATE TABLE `maintenance_requests` (
  `maintenance_id` int(11) NOT NULL AUTO_INCREMENT,
  `boarder_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `booking_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `maintenance_type` enum('Plumbing','Electrical','HVAC','Furniture','Appliance','Security','Cleaning','Other') NOT NULL,
  `priority` enum('Low','Medium','High','Urgent') DEFAULT 'Medium',
  `title` varchar(200) NOT NULL,
  `description` text NOT NULL,
  `location` varchar(200),
  `images` json,
  `preferred_date` date,
  `preferred_time` varchar(50),
  `contact_phone` varchar(20),
  `status` enum('Pending','In Progress','Completed','Cancelled','On Hold') DEFAULT 'Pending',
  `assigned_to` int(11),
  `estimated_cost` decimal(10,2),
  `actual_cost` decimal(10,2),
  `work_started_date` datetime,
  `work_completed_date` datetime,
  `notes` text,
  `feedback_rating` int(1),
  `feedback_comment` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`maintenance_id`),
  KEY `boarder_id` (`boarder_id`),
  KEY `room_id` (`room_id`),
  KEY `booking_id` (`booking_id`),
  KEY `owner_id` (`owner_id`),
  KEY `status` (`status`),
  KEY `priority` (`priority`),
  KEY `maintenance_type` (`maintenance_type`),
  KEY `assigned_to` (`assigned_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 2. **bookings** table (Referenced)
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
  `notes` text,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`booking_id`),
  KEY `user_id` (`user_id`),
  KEY `room_id` (`room_id`),
  KEY `booking_status` (`booking_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 3. **users** table
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

### 4. **registration** table
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

### 5. **room_units** table
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

### 6. **boarding_house_rooms** table
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

### 7. **boarding_houses** table
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

### 8. **notifications** table (for maintenance notifications)
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

The following PHP files are needed for the maintenance requests system to work:

### Core API Files:
1. **`submit_maintenance_request.php`** - Submit new maintenance requests
2. **`get_maintenance_requests.php`** - Get maintenance requests with filtering
3. **`update_maintenance_status.php`** - Update maintenance request status
4. **`get_maintenance_details.php`** - Get detailed maintenance request information
5. **`submit_maintenance_feedback.php`** - Submit feedback for completed maintenance
6. **`get_maintenance_summary.php`** - Get maintenance statistics and summary

### Helper Files:
7. **`db_helper.php`** - Database connection and helper functions
8. **`auto_notify_maintenance.php`** - Automatic maintenance notifications
9. **`notification_helper.php`** - Notification system helper

### Test Files (Optional):
10. **`test_maintenance_apis.php`** - Test all maintenance APIs

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
- `GET get_maintenance_requests.php?user_id=1&user_type=owner&status=all&priority=all&type=all&limit=50&offset=0`
- `GET get_maintenance_requests.php?user_id=1&user_type=boarder&status=pending&priority=urgent`
- `GET get_maintenance_details.php?maintenance_id=1`
- `GET get_maintenance_summary.php?user_id=1&user_type=owner&period=month`

### POST Endpoints:
- `POST submit_maintenance_request.php` (JSON: boarder_id, room_id, maintenance_type, priority, title, description, location, images, preferred_date, preferred_time, contact_phone)
- `POST update_maintenance_status.php` (JSON: maintenance_id, status, assigned_to, estimated_cost, actual_cost, work_started_date, work_completed_date, notes, updated_by)
- `POST submit_maintenance_feedback.php` (JSON: maintenance_id, rating, comment, feedback_by)

## 📱 Android Integration

The Android app can be configured to use these APIs for:
- **Submit Maintenance Requests** → `submit_maintenance_request.php`
- **View Maintenance Requests** → `get_maintenance_requests.php`
- **Update Maintenance Status** → `update_maintenance_status.php`
- **View Maintenance Details** → `get_maintenance_details.php`
- **Submit Feedback** → `submit_maintenance_feedback.php`
- **Maintenance Summary** → `get_maintenance_summary.php`

## 🔧 MAINTENANCE FEATURES

### Maintenance Types:
- **Plumbing** - Water, pipes, faucets, toilets
- **Electrical** - Wiring, outlets, lighting
- **HVAC** - Heating, ventilation, air conditioning
- **Furniture** - Chairs, tables, beds, cabinets
- **Appliance** - Refrigerator, washing machine, etc.
- **Security** - Locks, doors, windows, alarms
- **Cleaning** - General cleaning requests
- **Other** - Miscellaneous maintenance

### Priority Levels:
- **Low** - Non-urgent, can wait
- **Medium** - Standard priority
- **High** - Important, needs attention soon
- **Urgent** - Critical, needs immediate attention

### Status Types:
- **Pending** - Request submitted, waiting for review
- **In Progress** - Work has started
- **Completed** - Work finished
- **Cancelled** - Request cancelled
- **On Hold** - Temporarily paused

### Request Features:
- **Image Upload** - Multiple photos of the issue
- **Preferred Scheduling** - Preferred date and time
- **Location Details** - Specific location in room
- **Contact Information** - Phone number for updates
- **Cost Tracking** - Estimated and actual costs
- **Assignment** - Assign to maintenance staff
- **Progress Tracking** - Work started and completed dates
- **Feedback System** - Rating and comments

## 🔔 NOTIFICATION FEATURES

### Automatic Notifications:
- **New Request** - Notify owner of new maintenance request
- **Status Update** - Notify boarder of status changes
- **Work Started** - Notify when maintenance work begins
- **Work Completed** - Notify when maintenance is finished
- **Feedback Request** - Request feedback after completion

## 📊 ANALYTICS FEATURES

### Maintenance Summary:
- **Total Requests** - Number of maintenance requests
- **Pending Requests** - Requests waiting for action
- **Completed Requests** - Successfully completed requests
- **Average Response Time** - Time to start work
- **Average Completion Time** - Time to complete work
- **Cost Tracking** - Total maintenance costs
- **Type Distribution** - Most common maintenance types
- **Priority Analysis** - Urgent vs. non-urgent requests

### Performance Metrics:
- **Response Rate** - Percentage of requests addressed
- **Completion Rate** - Percentage of requests completed
- **Customer Satisfaction** - Average feedback rating
- **Cost Efficiency** - Cost per maintenance request
- **Trend Analysis** - Maintenance patterns over time

## ✅ VERIFICATION CHECKLIST

- [ ] All database tables created
- [ ] All PHP files present
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] Maintenance request submission working
- [ ] Status updates working
- [ ] Filtering and pagination working
- [ ] Feedback system working
- [ ] Maintenance summary working
- [ ] Notifications working

## 🎯 RESULT

Once all requirements are met, your maintenance requests system will have:
- ✅ Real-time maintenance request tracking
- ✅ Image upload support
- ✅ Priority and status management
- ✅ Cost tracking and assignment
- ✅ Feedback and rating system
- ✅ Maintenance analytics and reporting
- ✅ Automatic notifications
- ✅ Professional UI/UX

**Your maintenance requests system will be fully functional! 🎉**














