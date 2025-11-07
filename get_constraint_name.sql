-- Get the constraint name for bookings.room_id foreign key
USE boardease2;

-- Method 1: Show full CREATE TABLE (look for CONSTRAINT name in the output)
SHOW CREATE TABLE bookings;

-- Method 2: Query information schema directly
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

