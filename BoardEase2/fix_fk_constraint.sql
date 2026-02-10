-- Run this SQL in MySQL Workbench to fix the foreign key constraint
-- Step 1: Drop the incorrect foreign key
ALTER TABLE change_room_requests DROP FOREIGN KEY change_room_requests_ibfk_2;

-- Step 2: Add the correct foreign key pointing to users(user_id)
ALTER TABLE change_room_requests 
ADD CONSTRAINT change_room_requests_ibfk_2 
FOREIGN KEY (user_id) REFERENCES users(user_id) 
ON DELETE CASCADE;

-- Step 3: Verify the fix
SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'u223444398_boardease'
AND TABLE_NAME = 'change_room_requests'
AND COLUMN_NAME = 'user_id'
AND REFERENCED_TABLE_NAME IS NOT NULL;
