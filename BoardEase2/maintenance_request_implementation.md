# Maintenance Request Implementation

## Overview
Implementation of the maintenance request feature with database structure modification and full functionality to save data.

## Database Structure Modification

### SQL Script: `alter_maintenance_requests_table.sql`

The maintenance_requests table has been modified to include the following structure:

#### New Table Structure:
```sql
CREATE TABLE `maintenance_requests` (
  `request_id` int(11) NOT NULL AUTO_INCREMENT,  -- Primary Key
  `user_id` int(11) NOT NULL,                     -- Foreign Key to users
  `room_id` int(11) DEFAULT NULL,                 -- Foreign Key to room_units (NEW)
  `subject` varchar(255) NOT NULL,                -- NEW
  `area_for_maintenance` varchar(50) NOT NULL,    -- NEW
  `mr_description` text NOT NULL,                 -- Existing
  `mr_status` enum('Pending','In Progress','Resolved') NOT NULL DEFAULT 'Pending',
  `mr_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

#### New Fields Added:
1. **room_id** (int, nullable)
   - Foreign key to `room_units.room_id`
   - Can be NULL if room information is not available
   - Constraint: `ON DELETE SET NULL ON UPDATE CASCADE`

2. **subject** (varchar(255), NOT NULL)
   - Subject/title of the maintenance request
   - Required field

3. **area_for_maintenance** (varchar(50), NOT NULL)
   - Area where maintenance is needed
   - Valid values: 'BH Room', 'Bathroom', 'Kitchen', 'Others'
   - Required field

#### Foreign Key Constraints:
- `fk_maintenance_room`: Links `room_id` to `room_units.room_id`
- `maintenance_requests_ibfk_1`: Links `user_id` to `users.user_id` (existing)

## Files Created/Modified

### 1. Database Script
- **File**: `BoardEase2/alter_maintenance_requests_table.sql`
- **Purpose**: ALTER TABLE script to add new columns and foreign key constraints

### 2. PHP Backend
- **File**: `BoardEase2/submit_maintenance_request.php`
- **Purpose**: API endpoint to handle maintenance request submissions
- **Features**:
  - Validates required fields (user_id, subject, area_for_maintenance, description)
  - Validates area_for_maintenance against allowed values
  - Handles room_id as optional (can be NULL)
  - Returns JSON response with success/error status
  - Includes CORS headers for cross-origin requests
  - Handles OPTIONS preflight requests

### 3. Android Implementation
- **File**: `app/src/main/java/com/example/mock/BoarderBookingFragment.java`
- **Changes**:
  - Added `roomId` field to `Booking` class
  - Updated `parseBookings()` to extract `room_id` from API response
  - Implemented `submitMaintenanceRequest()` method
  - Added form validation in `showMaintenanceReportDialog()`
  - Integrated with Volley for HTTP requests

## API Endpoint

### Endpoint: `POST /BoardEase2/submit_maintenance_request.php`

#### Request Body (JSON):
```json
{
  "user_id": 1,
  "room_id": 5,  // Optional, can be omitted if not available
  "subject": "Leaking faucet",
  "area_for_maintenance": "Bathroom",
  "description": "The faucet in the bathroom is leaking..."
}
```

#### Response (Success):
```json
{
  "success": true,
  "message": "Maintenance request submitted successfully",
  "request_id": 1
}
```

#### Response (Error):
```json
{
  "success": false,
  "error": "Error message here"
}
```

#### Validation Rules:
- **user_id**: Required, must be a valid integer
- **room_id**: Optional, can be NULL or omitted
- **subject**: Required, cannot be empty
- **area_for_maintenance**: Required, must be one of: 'BH Room', 'Bathroom', 'Kitchen', 'Others'
- **description**: Required, cannot be empty

## Android Form Fields

### Form Structure:
1. **Subject of Report** (TextInputLayout)
   - Single-line text input
   - Required field
   - Orange hint color

2. **Area for Maintenance** (RadioGroup)
   - Options: BH Room, Bathroom, Kitchen, Others
   - Required selection
   - Orange radio button indicators

3. **Description** (TextInputLayout)
   - Multi-line text input (4-6 lines)
   - Required field
   - Orange hint color

### Form Validation:
- Subject: Must not be empty
- Area: Must have a selection
- Description: Must not be empty
- Shows error messages for invalid fields
- Prevents submission if validation fails

## Data Flow

1. **User Action**: User clicks "Report for Maintenance" button in booking details dialog
2. **Dialog Display**: Maintenance report dialog appears with form fields
3. **User Input**: User fills in subject, selects area, and provides description
4. **Form Validation**: Android app validates all required fields
5. **API Request**: Sends POST request to `submit_maintenance_request.php` with JSON data
6. **Server Validation**: PHP validates data and checks constraints
7. **Database Insert**: Inserts new record into `maintenance_requests` table
8. **Response**: Returns success/error message to Android app
9. **User Feedback**: Shows toast message and closes dialog on success

## Implementation Steps

### Step 1: Update Database
Run the SQL script to modify the table structure:
```sql
-- Execute: BoardEase2/alter_maintenance_requests_table.sql
```

### Step 2: Deploy PHP File
Upload `submit_maintenance_request.php` to the server:
- Location: `BoardEase2/submit_maintenance_request.php`
- Ensure proper file permissions

### Step 3: Test the Implementation
1. Open the app and navigate to Current Boarding House Booked section
2. Click on a booking card
3. Click "Report for Maintenance" button
4. Fill in the form:
   - Enter subject
   - Select area (BH Room, Bathroom, Kitchen, or Others)
   - Enter description
5. Click "Submit Report"
6. Verify success message and check database

## Database Queries

### Insert Maintenance Request:
```sql
INSERT INTO maintenance_requests (user_id, room_id, subject, area_for_maintenance, mr_description, mr_status) 
VALUES (1, 5, 'Leaking faucet', 'Bathroom', 'The faucet is leaking...', 'Pending');
```

### Query Maintenance Requests:
```sql
SELECT * FROM maintenance_requests 
WHERE user_id = 1 
ORDER BY mr_created_at DESC;
```

### Query with Room Information:
```sql
SELECT mr.*, ru.room_number, bhr.room_category, bh.bh_name
FROM maintenance_requests mr
LEFT JOIN room_units ru ON mr.room_id = ru.room_id
LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
WHERE mr.user_id = 1
ORDER BY mr.mr_created_at DESC;
```

## Error Handling

### Android App:
- Validates form fields before submission
- Shows error messages for empty/invalid fields
- Displays network error messages
- Shows success/error toast messages
- Handles JSON parsing errors

### PHP Backend:
- Validates all required fields
- Validates area_for_maintenance against allowed values
- Handles database connection errors
- Returns descriptive error messages
- Logs errors for debugging

## Testing Checklist

- [ ] Run SQL script to update database structure
- [ ] Verify table structure matches requirements
- [ ] Test PHP endpoint with valid data
- [ ] Test PHP endpoint with missing fields
- [ ] Test PHP endpoint with invalid area_for_maintenance
- [ ] Test Android form validation
- [ ] Test Android form submission with valid data
- [ ] Test Android form submission with missing fields
- [ ] Verify data is saved correctly in database
- [ ] Test with room_id and without room_id
- [ ] Verify error messages are displayed correctly

## Notes

- The `room_id` field is optional and can be NULL if room information is not available
- The `area_for_maintenance` field is validated against a fixed list of values
- All maintenance requests are created with status 'Pending' by default
- The `mr_created_at` timestamp is automatically set by the database
- The implementation uses Volley for HTTP requests in Android
- CORS headers are included for cross-origin requests
- The API supports both JSON and POST form data

## Future Enhancements

1. Add image upload functionality
2. Add maintenance request history view
3. Add status update functionality
4. Add notifications for maintenance requests
5. Add priority levels
6. Add estimated completion time
7. Add assigned maintenance staff
8. Add cost tracking

