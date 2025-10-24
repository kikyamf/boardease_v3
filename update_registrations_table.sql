-- Update registrations table to support admin approval workflow
-- Run this SQL script on your database

-- Add status column to track registration status
ALTER TABLE registrations 
ADD COLUMN status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending';

-- Add timestamps for approval tracking
ALTER TABLE registrations 
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE registrations 
ADD COLUMN approved_at TIMESTAMP NULL;

ALTER TABLE registrations 
ADD COLUMN rejected_at TIMESTAMP NULL;

-- Add index for better performance on status queries
CREATE INDEX idx_registrations_status ON registrations(status);
CREATE INDEX idx_registrations_created_at ON registrations(created_at);

-- Update existing registrations to have 'approved' status (assuming they were already processed)
UPDATE registrations SET status = 'approved' WHERE status = 'pending' AND created_at < NOW();

-- Optional: Add unique constraint on email if not already exists
-- ALTER TABLE registrations ADD CONSTRAINT unique_email UNIQUE (email);










