# Bookings SQL Queries

## Database Structure
- Table: `bookings`
  - `booking_id` (PK)
  - `room_id` (FK to `room_units.room_id`)
  - `user_id` (FK to `users.user_id`)
  - `start_date` (date)
  - `end_date` (date)
  - `booking_status` (enum: 'Pending','Confirmed','Cancelled','Completed')
  - `booking_date` (timestamp)

## SQL Queries for Each Section

### 1. Current Boarding House Booked
**Criteria:**
- `booking_status = 'Confirmed'`
- Current date is between `start_date` and `end_date`
- User is currently staying

```sql
SELECT 
    b.booking_id,
    b.room_id,
    b.user_id,
    b.start_date,
    b.end_date,
    b.booking_status,
    b.booking_date,
    ru.room_number,
    bhr.room_category,
    bhr.price,
    bhr.bh_id,
    bh.bh_name,
    bh.bh_address,
    bh.bh_description,
    (SELECT bhi.image_path 
     FROM boarding_house_images AS bhi 
     WHERE bhi.bh_id = bh.bh_id 
     ORDER BY bhi.image_id ASC 
     LIMIT 1) as image_path,
    COALESCE(SUM(p.payment_amount), 0) as total_paid,
    COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as confirmed_paid,
    bhr.price - COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as balance_due
FROM bookings b
INNER JOIN room_units ru ON b.room_id = ru.room_id
INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
LEFT JOIN payments p ON b.booking_id = p.booking_id
WHERE b.user_id = :user_id
    AND b.booking_status = 'Confirmed'
    AND CURDATE() >= b.start_date
    AND CURDATE() <= b.end_date
GROUP BY b.booking_id, b.room_id, b.user_id, b.start_date, b.end_date, 
         b.booking_status, b.booking_date, ru.room_number, bhr.room_category, 
         bhr.price, bhr.bh_id, bh.bh_name, bh.bh_address, bh.bh_description
ORDER BY b.booking_date DESC;
```

### 2. Pending Booking
**Criteria:**
- `booking_status = 'Pending'`
- Not yet approved by owner

```sql
SELECT 
    b.booking_id,
    b.room_id,
    b.user_id,
    b.start_date,
    b.end_date,
    b.booking_status,
    b.booking_date,
    ru.room_number,
    bhr.room_category,
    bhr.price,
    bhr.bh_id,
    bh.bh_name,
    bh.bh_address,
    bh.bh_description,
    (SELECT bhi.image_path 
     FROM boarding_house_images AS bhi 
     WHERE bhi.bh_id = bh.bh_id 
     ORDER BY bhi.image_id ASC 
     LIMIT 1) as image_path,
    COALESCE(SUM(p.payment_amount), 0) as total_paid,
    COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as confirmed_paid,
    bhr.price - COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as balance_due
FROM bookings b
INNER JOIN room_units ru ON b.room_id = ru.room_id
INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
LEFT JOIN payments p ON b.booking_id = p.booking_id
WHERE b.user_id = :user_id
    AND b.booking_status = 'Pending'
GROUP BY b.booking_id, b.room_id, b.user_id, b.start_date, b.end_date, 
         b.booking_status, b.booking_date, ru.room_number, bhr.room_category, 
         bhr.price, bhr.bh_id, bh.bh_name, bh.bh_address, bh.bh_description
ORDER BY b.booking_date DESC;
```

### 3. Booking History
**Criteria:**
- `booking_status = 'Completed'`
- Past bookings that have ended

```sql
SELECT 
    b.booking_id,
    b.room_id,
    b.user_id,
    b.start_date,
    b.end_date,
    b.booking_status,
    b.booking_date,
    ru.room_number,
    bhr.room_category,
    bhr.price,
    bhr.bh_id,
    bh.bh_name,
    bh.bh_address,
    bh.bh_description,
    (SELECT bhi.image_path 
     FROM boarding_house_images AS bhi 
     WHERE bhi.bh_id = bh.bh_id 
     ORDER BY bhi.image_id ASC 
     LIMIT 1) as image_path,
    COALESCE(SUM(p.payment_amount), 0) as total_paid,
    COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as confirmed_paid,
    bhr.price - COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as balance_due
FROM bookings b
INNER JOIN room_units ru ON b.room_id = ru.room_id
INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
LEFT JOIN payments p ON b.booking_id = p.booking_id
WHERE b.user_id = :user_id
    AND b.booking_status = 'Completed'
GROUP BY b.booking_id, b.room_id, b.user_id, b.start_date, b.end_date, 
         b.booking_status, b.booking_date, ru.room_number, bhr.room_category, 
         bhr.price, bhr.bh_id, bh.bh_name, bh.bh_address, bh.bh_description
ORDER BY b.end_date DESC;
```

## Combined Query (All Three Sections)
This query returns all bookings with a `section` field indicating which section they belong to:

```sql
SELECT 
    b.booking_id,
    b.room_id,
    b.user_id,
    b.start_date,
    b.end_date,
    b.booking_status,
    b.booking_date,
    ru.room_number,
    bhr.room_category,
    bhr.price,
    bhr.bh_id,
    bh.bh_name,
    bh.bh_address,
    bh.bh_description,
    (SELECT bhi.image_path 
     FROM boarding_house_images AS bhi 
     WHERE bhi.bh_id = bh.bh_id 
     ORDER BY bhi.image_id ASC 
     LIMIT 1) as image_path,
    COALESCE(SUM(p.payment_amount), 0) as total_paid,
    COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as confirmed_paid,
    bhr.price - COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as balance_due,
    CASE 
        WHEN b.booking_status = 'Confirmed' AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date THEN 'current'
        WHEN b.booking_status = 'Pending' THEN 'pending'
        WHEN b.booking_status = 'Completed' THEN 'history'
        ELSE 'other'
    END as section
FROM bookings b
INNER JOIN room_units ru ON b.room_id = ru.room_id
INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
LEFT JOIN payments p ON b.booking_id = p.booking_id
WHERE b.user_id = :user_id
    AND b.booking_status IN ('Pending', 'Confirmed', 'Completed')
GROUP BY b.booking_id, b.room_id, b.user_id, b.start_date, b.end_date, 
         b.booking_status, b.booking_date, ru.room_number, bhr.room_category, 
         bhr.price, bhr.bh_id, bh.bh_name, bh.bh_address, bh.bh_description
ORDER BY 
    CASE 
        WHEN b.booking_status = 'Confirmed' AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date THEN 1
        WHEN b.booking_status = 'Pending' THEN 2
        WHEN b.booking_status = 'Completed' THEN 3
        ELSE 4
    END,
    b.booking_date DESC;
```

## Notes
- All queries use `user_id` which references `users.user_id` (not `registrations.id`)
- Payment calculations include total paid and balance due
- Image path is the first image from `boarding_house_images`
- Queries exclude 'Cancelled' bookings
- Current bookings must have current date between start and end dates

