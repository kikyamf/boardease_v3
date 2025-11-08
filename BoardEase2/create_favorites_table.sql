-- Create boarder_favorites table with registrations.id reference
-- This makes it easier since login already returns registrations.id

-- Drop existing table if it exists (BE CAREFUL - this will delete all favorites!)
-- DROP TABLE IF EXISTS `boarder_favorites`;

-- Create the table with registrations.id reference
CREATE TABLE IF NOT EXISTS `boarder_favorites` (
  `fav_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT 'References registrations.id (not users.user_id)',
  `bh_id` int(11) NOT NULL COMMENT 'References boarding_houses.bh_id',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`fav_id`),
  UNIQUE KEY `unique_favorite` (`user_id`, `bh_id`),
  KEY `fk_user_reg` (`user_id`),
  KEY `fk_bh` (`bh_id`),
  CONSTRAINT `fk_bh_favorites` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_reg_favorites` FOREIGN KEY (`user_id`) REFERENCES `registrations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Alternative: If you want to keep using users.user_id, use this version instead:
-- CREATE TABLE IF NOT EXISTS `boarder_favorites` (
--   `fav_id` int(11) NOT NULL AUTO_INCREMENT,
--   `user_id` int(11) NOT NULL COMMENT 'References users.user_id',
--   `bh_id` int(11) NOT NULL COMMENT 'References boarding_houses.bh_id',
--   `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
--   PRIMARY KEY (`fav_id`),
--   UNIQUE KEY `unique_favorite` (`user_id`, `bh_id`),
--   KEY `fk_user` (`user_id`),
--   KEY `fk_bh` (`bh_id`),
--   CONSTRAINT `fk_bh_favorites` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE ON UPDATE CASCADE,
--   CONSTRAINT `fk_user_favorites` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

