-- Insert user 28 as boarder with correct table structure
-- Table structure: active_id, user_id, status, room_id, boarding_house_id

-- Clear any existing entry for user 28
DELETE FROM active_boarders WHERE user_id = 28;

-- Insert new entry
INSERT INTO active_boarders (user_id, status, room_id, boarding_house_id) 
VALUES (28, 'Active', 81, 85);

-- Verify the insertion
SELECT * FROM active_boarders WHERE user_id = 28;

-- Show all active_boarders data
SELECT * FROM active_boarders ORDER BY active_id;




