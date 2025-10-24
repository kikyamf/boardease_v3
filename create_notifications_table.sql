-- Create notifications table for BoardEase
CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `notif_title` varchar(150) NOT NULL,
  `notif_message` text NOT NULL,
  `notif_type` enum('booking','payment','announcement','maintenance','general') DEFAULT 'general',
  `notif_status` enum('unread','read') DEFAULT 'unread',
  `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`notif_id`),
  KEY `user_id` (`user_id`),
  KEY `notif_status` (`notif_status`),
  KEY `notif_type` (`notif_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;





















