-- Update payment_breakdowns table to add 'For Approval' status
-- This allows tracking payments that have been submitted but are waiting for BH Owner approval

ALTER TABLE `payment_breakdowns` 
MODIFY COLUMN `payment_status` enum('Pending','For Approval','Paid','Overdue','Cancelled') NOT NULL DEFAULT 'Pending';

-- Update any existing records that have a payment_id but are not yet paid to 'For Approval'
-- This handles cases where payments were submitted but status wasn't updated
UPDATE `payment_breakdowns` 
SET `payment_status` = 'For Approval' 
WHERE `payment_id` IS NOT NULL 
  AND `is_paid` = 0 
  AND `payment_status` = 'Pending';

