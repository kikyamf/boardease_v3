-- SQL script to update bookings table foreign key constraint
-- This changes room_id to reference boarding_house_rooms.bhr_id instead of room_units.room_id

USE boardease2;

-- Step 1: Find and drop ALL foreign key constraints on bookings table
-- First, let's see what constraints exist
SHOW CREATE TABLE bookings;

-- Step 2: Drop the foreign key constraint (try common names)
-- If the error says it doesn't exist, check the actual constraint name from Step 1
-- Common constraint names:
-- ALTER TABLE bookings DROP FOREIGN KEY bookings_ibfk_1;
-- ALTER TABLE bookings DROP FOREIGN KEY bookings_ibfk_room;
-- Or use this to find and drop dynamically:

SET @constraint_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = 'boardease2' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'room_id' 
    AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @sql = CONCAT('ALTER TABLE bookings DROP FOREIGN KEY ', @constraint_name);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Add new foreign key constraint pointing to boarding_house_rooms
ALTER TABLE bookings 
ADD CONSTRAINT bookings_ibfk_room 
FOREIGN KEY (room_id) REFERENCES boarding_house_rooms(bhr_id) ON DELETE CASCADE;

-- Verify the change
SHOW CREATE TABLE bookings;

