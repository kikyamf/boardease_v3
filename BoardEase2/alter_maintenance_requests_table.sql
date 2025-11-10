-- Alter maintenance_requests table to add new fields
-- This script adds: room_id, subject, and area_for_maintenance

-- Add room_id column (Foreign Key to room_units)
ALTER TABLE `maintenance_requests`
  ADD COLUMN `room_id` int(11) DEFAULT NULL AFTER `user_id`,
  ADD KEY `room_id` (`room_id`);

-- Add subject column
ALTER TABLE `maintenance_requests`
  ADD COLUMN `subject` varchar(255) NOT NULL AFTER `room_id`;

-- Add area_for_maintenance column
ALTER TABLE `maintenance_requests`
  ADD COLUMN `area_for_maintenance` varchar(50) NOT NULL AFTER `subject`;

-- Add foreign key constraint for room_id
ALTER TABLE `maintenance_requests`
  ADD CONSTRAINT `fk_maintenance_room` FOREIGN KEY (`room_id`) 
  REFERENCES `room_units` (`room_id`) 
  ON DELETE SET NULL 
  ON UPDATE CASCADE;

-- Verify the table structure
-- DESCRIBE maintenance_requests;

