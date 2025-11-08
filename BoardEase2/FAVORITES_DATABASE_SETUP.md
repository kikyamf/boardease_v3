# Favorites Database Setup Guide

## Overview
This guide explains how to set up the database for the favorites functionality. The system uses a hybrid approach:
- **Database** (primary): Stores favorites permanently, syncs across devices
- **SharedPreferences** (cache): Fast local access, syncs with database

## Database Structure

The `boarder_favorites` table should reference `registrations.id` (not `users.user_id`) because:
1. Login returns `registrations.id` in the Android app
2. Simpler - no mapping needed
3. Works for all users regardless of `users` table entry

### Recommended Table Structure

```sql
CREATE TABLE `boarder_favorites` (
  `fav_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT 'References registrations.id',
  `bh_id` int(11) NOT NULL COMMENT 'References boarding_houses.bh_id',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`fav_id`),
  UNIQUE KEY `unique_favorite` (`user_id`, `bh_id`),
  KEY `fk_user_reg` (`user_id`),
  KEY `fk_bh` (`bh_id`),
  CONSTRAINT `fk_bh_favorites` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_reg_favorites` FOREIGN KEY (`user_id`) REFERENCES `registrations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## Setup Steps

### Step 1: Check Current Table Structure

Run this in your browser:
```
http://192.168.1.9/boardease_v3/BoardEase2/setup_favorites_table.php
```

This will show:
- If the table exists
- Current structure
- What needs to be changed

### Step 2: Update Table Structure (if needed)

If the table exists but references `users.user_id`, run this SQL in phpMyAdmin or MySQL:

```sql
-- Drop existing foreign key constraints (adjust constraint names as needed)
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS boarder_favorites_ibfk_1;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS boarder_favorites_ibfk_2;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS fk_user;
ALTER TABLE boarder_favorites DROP FOREIGN KEY IF EXISTS fk_user_favorites;

-- Add new foreign key to reference registrations.id
ALTER TABLE boarder_favorites 
ADD CONSTRAINT fk_user_reg_favorites 
FOREIGN KEY (user_id) REFERENCES registrations(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Ensure foreign key for bh_id exists
ALTER TABLE boarder_favorites 
ADD CONSTRAINT fk_bh_favorites 
FOREIGN KEY (bh_id) REFERENCES boarding_houses(bh_id) 
ON DELETE CASCADE ON UPDATE CASCADE;
```

### Step 3: Create Table (if it doesn't exist)

If the table doesn't exist, run the SQL from `create_favorites_table.sql`:

```sql
CREATE TABLE IF NOT EXISTS `boarder_favorites` (
  `fav_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT 'References registrations.id',
  `bh_id` int(11) NOT NULL COMMENT 'References boarding_houses.bh_id',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`fav_id`),
  UNIQUE KEY `unique_favorite` (`user_id`, `bh_id`),
  KEY `fk_user_reg` (`user_id`),
  KEY `fk_bh` (`bh_id`),
  CONSTRAINT `fk_bh_favorites` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_reg_favorites` FOREIGN KEY (`user_id`) REFERENCES `registrations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## PHP Endpoints

Three PHP endpoints handle favorites:

1. **`get_favorites_v2.php`** - Fetch all favorites for a user
   - Accepts: `user_id` (POST or GET)
   - Returns: JSON array of favorite boarding houses with full details
   - Automatically maps `users.user_id` to `registrations.id` if needed

2. **`add_favorite_v2.php`** - Add a boarding house to favorites
   - Accepts: `user_id`, `bh_id` (POST or GET)
   - Validates: Boarding house exists and is active
   - Prevents: Duplicate favorites
   - Automatically maps `users.user_id` to `registrations.id` if needed

3. **`remove_favorite_v2.php`** - Remove a boarding house from favorites
   - Accepts: `user_id`, `bh_id` (POST or GET)
   - Automatically maps `users.user_id` to `registrations.id` if needed

## Android App Flow

### Adding Favorites
1. User clicks heart icon in `ExploreFragment`
2. App immediately updates SharedPreferences (optimistic update)
3. App calls `add_favorite_v2.php` to save to database
4. If database call fails, reverts SharedPreferences change

### Removing Favorites
1. User clicks heart icon again (or delete button in favorites)
2. App immediately updates SharedPreferences (optimistic update)
3. App calls `remove_favorite_v2.php` to remove from database
4. If database call fails, reverts SharedPreferences change

### Loading Favorites
1. `BoarderFavoriteFragment` loads on navigation
2. App calls `get_favorites_v2.php` with user_id
3. App displays favorites from database
4. App also updates SharedPreferences cache

### Checking Favorites
- `isFavorite()` checks SharedPreferences cache (fast, always in sync)

## Testing

### Test Database Setup
1. Run `setup_favorites_table.php` to check table structure
2. Verify foreign keys point to correct tables

### Test Adding Favorite
1. Login to Android app
2. Navigate to ExploreFragment
3. Click heart icon on a listing
4. Check database: `SELECT * FROM boarder_favorites WHERE user_id = YOUR_USER_ID;`
5. Navigate to BoarderFavoriteFragment - should show the favorited item

### Test Persistence
1. Add a favorite
2. Logout and login again
3. Navigate to BoarderFavoriteFragment
4. Favorite should still be there (loaded from database)

## Troubleshooting

### Error: "User ID not found in registrations or users table"
- Check that the user_id being sent matches a `registrations.id` or `users.user_id`
- Verify login is returning the correct user_id

### Error: "Boarding house with ID X does not exist"
- Check that the `bh_id` exists in `boarding_houses` table
- Verify the boarding house status is 'Active'

### Error: "Foreign key constraint fails"
- Verify table structure matches the recommended structure
- Check that `user_id` references `registrations.id`
- Check that `bh_id` references `boarding_houses.bh_id`

### Favorites not persisting after logout
- Verify database calls are successful (check Logcat)
- Check PHP error logs for database errors
- Verify user_id is being sent correctly

## Files

- `create_favorites_table.sql` - SQL to create the table
- `setup_favorites_table.php` - Diagnostic script to check table structure
- `add_favorite_v2.php` - Add favorite endpoint
- `get_favorites_v2.php` - Get favorites endpoint
- `remove_favorite_v2.php` - Remove favorite endpoint
- `BoarderFavoriteFragment.java` - Android fragment that displays favorites

