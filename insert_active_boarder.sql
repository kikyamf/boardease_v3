-- Insert user 28 as boarder in boarding house 85
-- First, ensure the user exists
INSERT IGNORE INTO registrations (id, role, first_name, last_name, email, password, status, created_at) 
VALUES (28, 'Boarder', 'Test', 'User28', 'user28@test.com', 'password123', 'approved', NOW());

INSERT IGNORE INTO users (user_id, reg_id, status, created_at) 
VALUES (28, 28, 'Active', NOW());

-- Ensure boarding house exists
INSERT IGNORE INTO boarding_houses (bh_id, user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
VALUES (85, 1, 'Test Boarding House 85', '123 Test St', 'Test description', 'Test rules', 2, 100, 2024, 'active', NOW());

-- Create active_boarders table if it doesn't exist
CREATE TABLE IF NOT EXISTS active_boarders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    boarding_house_id INT NOT NULL,
    room_id INT NULL,
    status ENUM('Active', 'Inactive', 'Moved') DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Clear existing entry for user 28
DELETE FROM active_boarders WHERE user_id = 28;

-- Insert new entry
INSERT INTO active_boarders (user_id, boarding_house_id, room_id, status, created_at, updated_at) 
VALUES (28, 85, 81, 'Active', NOW(), NOW());

-- Verify the insertion
SELECT ab.*, bh.bh_name, bh.user_id as owner_id 
FROM active_boarders ab 
JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id 
WHERE ab.user_id = 28;




