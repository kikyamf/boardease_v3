# Maintenance Requests Table Structure

## Overview
The `maintenance_requests` table is responsible for storing maintenance reports and requests from boarders in the BoardEase system.

## Current Database Table Structure

### Table Name: `maintenance_requests`

```sql
CREATE TABLE `maintenance_requests` (
  `request_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `mr_description` text NOT NULL,
  `mr_status` enum('Pending','In Progress','Resolved') NOT NULL DEFAULT 'Pending',
  `mr_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## Column Breakdown

### 1. `request_id` (Primary Key)
- **Type**: `int(11)`
- **Constraints**: `NOT NULL`, `AUTO_INCREMENT`
- **Purpose**: Unique identifier for each maintenance request
- **Auto-generated**: Yes (database auto-increments this value)
- **Index**: PRIMARY KEY

### 2. `user_id` (Foreign Key)
- **Type**: `int(11)`
- **Constraints**: `NOT NULL`
- **Purpose**: Links the maintenance request to the user (boarder) who submitted it
- **Foreign Key**: References `users.user_id` with `ON DELETE CASCADE`
  - This means if a user is deleted, all their maintenance requests are also deleted
- **Index**: KEY `user_id` (`user_id`)

### 3. `mr_description` (Description)
- **Type**: `text`
- **Constraints**: `NOT NULL`
- **Purpose**: Stores the detailed description of the maintenance issue
- **Storage**: Can store large amounts of text (up to 65,535 bytes)
- **Example**: "The sink in the bathroom is leaking and water is pooling on the floor"

### 4. `mr_status` (Status)
- **Type**: `enum('Pending','In Progress','Resolved')`
- **Constraints**: `NOT NULL`, `DEFAULT 'Pending'`
- **Purpose**: Tracks the current status of the maintenance request
- **Possible Values**:
  - `'Pending'`: Request has been submitted but not yet started
  - `'In Progress'`: Maintenance work is currently being performed
  - `'Resolved'`: Maintenance has been completed
- **Default**: New requests automatically start as `'Pending'`

### 5. `mr_created_at` (Creation Timestamp)
- **Type**: `timestamp`
- **Constraints**: `NOT NULL`, `DEFAULT current_timestamp()`
- **Purpose**: Records when the maintenance request was created
- **Auto-generated**: Yes (automatically set to current date/time when record is created)
- **Format**: YYYY-MM-DD HH:MM:SS (e.g., '2025-01-15 14:30:00')

## Table Relationships

### Foreign Key Relationship
- **References**: `users` table via `user_id`
- **Constraint Name**: `maintenance_requests_ibfk_1`
- **Cascade Rule**: `ON DELETE CASCADE`
  - When a user is deleted, all their maintenance requests are automatically deleted

## Indexes

1. **Primary Key Index**: `request_id`
   - Ensures uniqueness and fast lookups by request ID

2. **Foreign Key Index**: `user_id`
   - Improves query performance when joining with users table
   - Speeds up filtering requests by user

## Current Limitations & Mismatch with Application

### ⚠️ **IMPORTANT NOTE**: 
The database table has only **4 columns**, but the Android application (`MaintenanceRequest.java`) expects many more fields:

#### Fields Expected by Application (but NOT in database):
- `boarderName` - Name of the boarder
- `boardingHouseName` - Name of the boarding house
- `roomNumber` - Room number where maintenance is needed
- `maintenanceType` - Type/category of maintenance (e.g., "Plumbing", "Electrical")
- `priority` - Priority level (e.g., "High", "Medium", "Low")
- `title` - Short title/summary of the request
- `location` - Specific location within the room/boarding house
- `contactPhone` - Contact phone number
- `preferredDate` - Preferred date for maintenance
- `preferredTime` - Preferred time for maintenance
- `assignedTo` - Person assigned to handle the request
- `estimatedCost` - Estimated cost of maintenance
- `actualCost` - Actual cost after completion
- `workStartedDate` - Date when work started
- `workCompletedDate` - Date when work was completed
- `notes` - Additional notes/comments
- `images` - Images attached to the request
- `feedbackRating` - Rating given after completion
- `feedbackComment` - Feedback comment after completion

### How This Might Work Currently:
The PHP backend likely:
1. Joins with other tables (like `users`, `bookings`, `room_units`) to get additional information
2. Stores some data in JSON format within `mr_description`
3. Or the table schema needs to be updated to include these fields

## Example Queries

### Get all maintenance requests for a user:
```sql
SELECT * FROM maintenance_requests 
WHERE user_id = 1 
ORDER BY mr_created_at DESC;
```

### Get pending maintenance requests:
```sql
SELECT * FROM maintenance_requests 
WHERE mr_status = 'Pending' 
ORDER BY mr_created_at ASC;
```

### Get maintenance requests with user information:
```sql
SELECT mr.*, u.first_name, u.last_name, u.email 
FROM maintenance_requests mr
JOIN users u ON mr.user_id = u.user_id
WHERE mr.mr_status = 'Pending';
```

### Update request status:
```sql
UPDATE maintenance_requests 
SET mr_status = 'In Progress' 
WHERE request_id = 1;
```

### Mark request as resolved:
```sql
UPDATE maintenance_requests 
SET mr_status = 'Resolved' 
WHERE request_id = 1;
```

## Recommendations

If you want to match the application's expectations, consider adding these columns to the table:

```sql
ALTER TABLE maintenance_requests
  ADD COLUMN `room_id` int(11) DEFAULT NULL AFTER `user_id`,
  ADD COLUMN `maintenance_type` varchar(50) DEFAULT NULL,
  ADD COLUMN `priority` enum('Low','Medium','High','Urgent') DEFAULT 'Medium',
  ADD COLUMN `title` varchar(200) DEFAULT NULL,
  ADD COLUMN `location` varchar(200) DEFAULT NULL,
  ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL,
  ADD COLUMN `preferred_date` date DEFAULT NULL,
  ADD COLUMN `preferred_time` time DEFAULT NULL,
  ADD COLUMN `assigned_to` int(11) DEFAULT NULL COMMENT 'user_id of assignee',
  ADD COLUMN `estimated_cost` decimal(10,2) DEFAULT NULL,
  ADD COLUMN `actual_cost` decimal(10,2) DEFAULT NULL,
  ADD COLUMN `work_started_date` timestamp NULL DEFAULT NULL,
  ADD COLUMN `work_completed_date` timestamp NULL DEFAULT NULL,
  ADD COLUMN `notes` text DEFAULT NULL,
  ADD COLUMN `images` json DEFAULT NULL,
  ADD COLUMN `feedback_rating` int(1) DEFAULT NULL COMMENT '1-5 rating',
  ADD COLUMN `feedback_comment` text DEFAULT NULL,
  ADD COLUMN `mr_updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp();
```

This would align the database structure with what the Android application expects.

