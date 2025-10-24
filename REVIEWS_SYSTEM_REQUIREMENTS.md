# ⭐ REVIEWS SYSTEM REQUIREMENTS

## 🗄️ REQUIRED DATABASE TABLES

The reviews system requires the following database tables to be created:

### 1. **reviews** table (Main Table)
```sql
CREATE TABLE `reviews` (
  `review_id` int(11) NOT NULL AUTO_INCREMENT,
  `boarder_id` int(11) NOT NULL,
  `boarding_house_id` int(11) NOT NULL,
  `booking_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `overall_rating` int(1) NOT NULL,
  `cleanliness_rating` int(1) NOT NULL,
  `location_rating` int(1) NOT NULL,
  `value_rating` int(1) NOT NULL,
  `amenities_rating` int(1) NOT NULL,
  `safety_rating` int(1) NOT NULL,
  `management_rating` int(1) NOT NULL,
  `title` varchar(200) NOT NULL,
  `review_text` text NOT NULL,
  `images` json,
  `would_recommend` boolean,
  `stay_duration` varchar(50),
  `visit_type` enum('Business','Leisure','Student') DEFAULT 'Student',
  `status` enum('Published','Pending','Rejected') DEFAULT 'Pending',
  `helpful_count` int(11) DEFAULT 0,
  `report_count` int(11) DEFAULT 0,
  `owner_response` text,
  `owner_response_date` timestamp NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`review_id`),
  KEY `boarder_id` (`boarder_id`),
  KEY `boarding_house_id` (`boarding_house_id`),
  KEY `booking_id` (`booking_id`),
  KEY `owner_id` (`owner_id`),
  KEY `room_id` (`room_id`),
  KEY `overall_rating` (`overall_rating`),
  KEY `status` (`status`),
  KEY `created_at` (`created_at`)
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
  `university` varchar(200),
  `student_id` varchar(50),
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

### 8. **notifications** table (for review notifications)
```sql
CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `notif_title` varchar(150) NOT NULL,
  `notif_message` text NOT NULL,
  `notif_type` enum('booking','payment','announcement','maintenance','general','review') DEFAULT 'general',
  `notif_status` enum('unread','read') DEFAULT 'unread',
  `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`notif_id`),
  KEY `user_id` (`user_id`),
  KEY `notif_status` (`notif_status`),
  KEY `notif_type` (`notif_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## 📁 REQUIRED PHP FILES

The following PHP files are needed for the reviews system to work:

### Core API Files:
1. **`submit_review.php`** - Submit new reviews (BOARDERS ONLY)
2. **`get_reviews.php`** - Get reviews with filtering and sorting
3. **`get_review_details.php`** - Get detailed review information
4. **`submit_owner_response.php`** - Submit owner responses to reviews
5. **`get_reviews_summary.php`** - Get review statistics and summary

### Helper Files:
6. **`db_helper.php`** - Database connection and helper functions
7. **`auto_notify_announcement.php`** - Review notifications
8. **`notification_helper.php`** - Notification system helper

### Test Files (Optional):
9. **`test_reviews_apis.php`** - Test all review APIs

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
- `GET get_reviews.php?boarding_house_id=1&rating=all&status=published&sort_by=newest&limit=20&offset=0`
- `GET get_reviews.php?owner_id=1&status=all&sort_by=highest_rating`
- `GET get_reviews.php?boarder_id=1&status=published`
- `GET get_review_details.php?review_id=1`
- `GET get_reviews_summary.php?boarding_house_id=1&period=month`

### POST Endpoints:
- `POST submit_review.php` (JSON: boarder_id, boarding_house_id, booking_id, overall_rating, cleanliness_rating, location_rating, value_rating, amenities_rating, safety_rating, management_rating, title, review_text, images, would_recommend, stay_duration, visit_type)
- `POST submit_owner_response.php` (JSON: review_id, owner_id, response_text)

## 📱 Android Integration

The Android app can be configured to use these APIs for:
- **Submit Reviews** → `submit_review.php` (BOARDERS ONLY)
- **View Reviews** → `get_reviews.php`
- **View Review Details** → `get_review_details.php`
- **Submit Owner Response** → `submit_owner_response.php` (OWNERS ONLY)
- **Reviews Summary** → `get_reviews_summary.php`

## ⭐ REVIEW FEATURES

### Multi-Category Ratings:
- **Overall Rating** - Overall experience (1-5 stars)
- **Cleanliness Rating** - How clean the place is (1-5 stars)
- **Location Rating** - Location convenience (1-5 stars)
- **Value Rating** - Value for money (1-5 stars)
- **Amenities Rating** - Available amenities (1-5 stars)
- **Safety Rating** - Safety and security (1-5 stars)
- **Management Rating** - Owner/management quality (1-5 stars)

### Review Content:
- **Title** - Brief review title
- **Review Text** - Detailed review content
- **Images** - Multiple photos of the experience
- **Recommendation** - Would recommend to others
- **Stay Duration** - How long they stayed
- **Visit Type** - Business, Leisure, or Student

### Review Status:
- **Published** - Live and visible to everyone
- **Pending** - Awaiting approval
- **Rejected** - Not approved for publication

### Advanced Features:
- **Owner Responses** - Owners can respond to reviews
- **Review Filtering** - Filter by rating, status, date
- **Review Sorting** - Sort by newest, oldest, highest rating
- **Pagination** - For large lists of reviews
- **Review Analytics** - Statistics and summaries

## 🔔 NOTIFICATION FEATURES

### Automatic Notifications:
- **New Review** - Notify owner of new review
- **Owner Response** - Notify boarder of owner response
- **Review Published** - Notify boarder when review is published
- **Review Rejected** - Notify boarder if review is rejected

## 📊 ANALYTICS FEATURES

### Review Summary:
- **Total Reviews** - Number of reviews
- **Average Rating** - Overall average rating
- **Rating Distribution** - Breakdown by star rating
- **Recommendation Rate** - Percentage who would recommend
- **Response Rate** - Percentage of reviews with owner responses

### Performance Metrics:
- **Review Volume** - Number of reviews over time
- **Rating Trends** - Rating changes over time
- **Category Performance** - Ratings by category
- **Response Time** - Time to respond to reviews

## ✅ VERIFICATION CHECKLIST

- [ ] All database tables created
- [ ] All PHP files present
- [ ] Database connection working
- [ ] API endpoints responding
- [ ] Review submission working
- [ ] Review filtering and sorting working
- [ ] Owner responses working
- [ ] Review summary working
- [ ] Notifications working

## 🎯 RESULT

Once all requirements are met, your reviews system will have:
- ✅ Multi-category rating system
- ✅ Image upload support
- ✅ Review filtering and sorting
- ✅ Owner response system
- ✅ Review analytics and reporting
- ✅ Automatic notifications
- ✅ Professional UI/UX

**Your reviews system will be fully functional! 🎉**
