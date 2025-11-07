-- Check existing foreign key constraints on bookings table
USE boardease2;

-- Show all constraints on bookings table
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
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Alternative: Show create table statement
SHOW CREATE TABLE bookings;

