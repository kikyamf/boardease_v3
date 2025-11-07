-- SQL script to add foreign key constraint to bookings table
-- This makes room_id reference boarding_house_rooms.bhr_id

USE boardease2;

-- Check if there are any existing foreign key constraints on room_id
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = 'boardease2'
    AND TABLE_NAME = 'bookings'
    AND COLUMN_NAME = 'room_id'
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- If the above query returns any rows, drop those constraints first
-- (Replace CONSTRAINT_NAME with the actual name from above)
-- ALTER TABLE bookings DROP FOREIGN KEY [CONSTRAINT_NAME];

-- Add new foreign key constraint pointing to room_units
-- room_id in bookings should reference room_units.room_id (the specific room unit selected by user)
ALTER TABLE bookings 
ADD CONSTRAINT bookings_ibfk_room 
FOREIGN KEY (room_id) REFERENCES room_units(room_id) ON DELETE CASCADE;

-- Verify the change
SHOW CREATE TABLE bookings;

