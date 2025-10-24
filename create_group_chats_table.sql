-- Create chat_groups table if it doesn't exist
CREATE TABLE IF NOT EXISTS `chat_groups` (
  `gc_id` int(11) NOT NULL AUTO_INCREMENT,
  `bh_id` int(11) DEFAULT NULL,
  `gc_name` varchar(255) NOT NULL,
  `gc_created_by` int(11) NOT NULL,
  `gc_created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`gc_id`),
  KEY `gc_created_by` (`gc_created_by`),
  KEY `bh_id` (`bh_id`),
  CONSTRAINT `chat_groups_ibfk_1` FOREIGN KEY (`gc_created_by`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;








