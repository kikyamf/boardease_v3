-- Fix any payment_breakdowns that have NULL status but have a payment_id
-- This happens when the enum 'For Approval' didn't exist yet
-- Run this after adding 'For Approval' to the enum

UPDATE payment_breakdowns 
SET payment_status = 'For Approval'
WHERE payment_id IS NOT NULL 
  AND payment_status IS NULL
  AND is_paid = 0;

-- Verify the update
SELECT 
    breakdown_id,
    booking_id,
    payment_id,
    payment_status,
    is_paid
FROM payment_breakdowns
WHERE payment_id IS NOT NULL
ORDER BY breakdown_id DESC
LIMIT 10;

