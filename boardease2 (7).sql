-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 29, 2025 at 03:45 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `boardease2`
--

-- --------------------------------------------------------

--
-- Table structure for table `active_boarders`
--

CREATE TABLE `active_boarders` (
  `active_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` enum('Active','Inactive') NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `boarding_house_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `active_boarders`
--

INSERT INTO `active_boarders` (`active_id`, `user_id`, `status`, `room_id`, `boarding_house_id`) VALUES
(5, 1, 'Active', 82, 85),
(6, 28, 'Active', 81, 85);

-- --------------------------------------------------------

--
-- Table structure for table `admin_accounts`
--

CREATE TABLE `admin_accounts` (
  `admin_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('super_admin') DEFAULT 'super_admin',
  `status` enum('active','inactive') DEFAULT 'active',
  `last_login` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin_accounts`
--

INSERT INTO `admin_accounts` (`admin_id`, `name`, `email`, `password`, `role`, `status`, `last_login`, `created_at`, `updated_at`) VALUES
(1, 'Super Admin', 'admin@boardease.com', '$2y$10$5sSPAwaECIF2WfiqJQa26uP6VM86cfEJ/52xVAdL0GaYDk60eBiuu', 'super_admin', 'active', '2025-10-29 02:42:48', '2025-10-25 07:13:20', '2025-10-29 02:42:48'),
(2, 'Your Partner', 'partner@boardease.com', '$2y$10$5sSPAwaECIF2WfiqJQa26uP6VM86cfEJ/52xVAdL0GaYDk60eBiuu', 'super_admin', 'active', NULL, '2025-10-25 07:13:20', '2025-10-25 10:57:35');

-- --------------------------------------------------------

--
-- Table structure for table `announcements`
--

CREATE TABLE `announcements` (
  `announcement_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  `an_title` varchar(150) NOT NULL,
  `an_content` text NOT NULL,
  `posted_by` int(11) NOT NULL,
  `an_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bills`
--

CREATE TABLE `bills` (
  `bill_id` int(11) NOT NULL,
  `active_id` int(11) NOT NULL,
  `amount_due` double(10,2) NOT NULL,
  `due_date` date NOT NULL,
  `status` enum('Unpaid','Paid','Overdue') NOT NULL DEFAULT 'Unpaid',
  `payment_id` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `boarding_houses`
--

CREATE TABLE `boarding_houses` (
  `bh_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `bh_name` varchar(100) NOT NULL,
  `bh_address` varchar(255) NOT NULL,
  `bh_description` text DEFAULT NULL,
  `bh_rules` text DEFAULT NULL,
  `number_of_bathroom` int(11) NOT NULL,
  `area` double(10,2) DEFAULT NULL,
  `build_year` year(4) DEFAULT NULL,
  `status` enum('Active','Inactive') NOT NULL,
  `bh_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `boarding_houses`
--

INSERT INTO `boarding_houses` (`bh_id`, `user_id`, `bh_name`, `bh_address`, `bh_description`, `bh_rules`, `number_of_bathroom`, `area`, `build_year`, `status`, `bh_created_at`) VALUES
(11, 1, 'BH CUAS', 'Tinibgan, Calape Bohol', 'ssss', 'sssss', 1, 5.00, '2024', 'Active', '2025-09-23 07:16:21'),
(12, 1, 'BH CUASS', 'Tinibgan', 'sss', 'sss', 2, 10.00, '2024', 'Active', '2025-09-23 07:16:27'),
(13, 1, 'BH CUAS', 'Tinibgan', 'sss', 'sss', 2, 10.00, '2024', 'Active', '2025-09-23 07:16:29'),
(14, 1, 'BH CUAS', 'Tinibgan', 'sss', 'sss', 2, 10.00, '2024', 'Active', '2025-09-23 07:17:42'),
(15, 1, 'BH MANTE', 'Bangi', 'bbb', 'bbb', 2, 14.00, '2025', 'Active', '2025-09-23 07:22:57'),
(16, 1, 'BH MANTE', 'Bangi Calape', 'bbb', 'bbb', 3, 14.00, '2025', 'Active', '2025-09-23 07:24:27'),
(17, 1, 'BH SKY', 'Bentig', 'bbb', 'bbb', 1, 5.00, '2025', 'Active', '2025-09-23 07:27:49'),
(18, 1, 'BH B', 'gg', 'ggg', 'ggg', 1, 5.00, '2024', 'Active', '2025-09-23 07:33:01'),
(19, 1, 'BH H', 'ggg', 'ggg', 'ggg', 1, 12.00, '2024', 'Active', '2025-09-23 07:34:57'),
(20, 1, 'BH C', 'hh', 'hh', 'hh', 1, 1.00, '2024', 'Active', '2025-09-23 07:38:07'),
(21, 1, 'BH G', 'Gg', 'gg', 'gg', 1, 1.00, '2024', 'Active', '2025-09-23 07:39:58'),
(22, 1, 'BH G', 'Gg', 'gg', 'gg', 1, 1.00, '2024', 'Active', '2025-09-23 07:40:32'),
(23, 1, 'BH J', 'jj', 'jj', 'jj', 1, 1.00, '2004', 'Active', '2025-09-23 07:42:45'),
(26, 1, 'BH K', 'kk', 'kk', 'kk', 1, 1.00, '2024', 'Active', '2025-09-23 07:56:35'),
(28, 1, 'BH K', 'kk', 'kk', 'kk', 1, 1.00, '2024', 'Active', '2025-09-23 07:56:36'),
(29, 1, 'BH K', 'kk', 'kk', 'kk', 1, 1.00, '2024', 'Active', '2025-09-23 07:56:36'),
(32, 1, 'BH K', 'kk', 'kk', 'kk', 1, 1.00, '2024', 'Active', '2025-09-23 07:57:22'),
(34, 1, 'BH L', 'yy', 'yy', 'yy', 1, 1.00, '2004', 'Active', '2025-09-23 08:02:54'),
(35, 1, 'BH L', 'yy', 'yy', 'yy', 1, 1.00, '2004', 'Active', '2025-09-23 08:03:03'),
(37, 1, 'BH L', 'yy', 'yy', 'yy', 1, 1.00, '2004', 'Active', '2025-09-23 08:03:13'),
(38, 1, 'BH L', 'yy', 'yy', 'yy', 1, 1.00, '2004', 'Active', '2025-09-23 08:03:27'),
(39, 1, 'BH L', 'yy', 'yy', 'yy', 1, 1.00, '2004', 'Active', '2025-09-23 08:05:16'),
(40, 1, 'BH L', 'kk', 'kk', 'kk', 1, 1.00, '2004', 'Active', '2025-09-23 08:08:38'),
(41, 1, 'BH L', 'kk', 'kk', 'kk', 1, 1.00, '2004', 'Active', '2025-09-23 08:08:47'),
(42, 1, 'GB', 'rr', 'rr', 'rr', 2, 2.00, '0000', 'Active', '2025-09-23 08:10:44'),
(43, 1, 'FG', 'uu', 'uu', 'uu', 1, 1.00, '2004', 'Active', '2025-09-23 08:23:31'),
(44, 1, 'BB', 'bb', 'bb', 'bb', 1, 6.00, '2023', 'Active', '2025-09-23 08:26:11'),
(45, 1, 'BB', 'bb', 'bb', 'bb', 1, 6.00, '2023', 'Active', '2025-09-23 08:31:34'),
(46, 1, 'AA', 'qq', 'qq', 'qq', 1, 23.00, '2023', 'Active', '2025-09-23 08:54:06'),
(47, 1, 'AA', 'qq', 'qq', 'qq', 1, 23.00, '2023', 'Active', '2025-09-23 08:54:52'),
(48, 1, 'AA', 'qq', 'qq', 'qq', 1, 23.00, '2023', 'Active', '2025-09-23 08:57:18'),
(49, 1, 'SS', 'ss', 'ss', 'ss', 1, 1.00, '2004', 'Active', '2025-09-23 09:01:39'),
(50, 1, 'DD', 'ee', 'ee', 'ee', 2, 20.00, '2020', 'Active', '2025-09-23 09:05:46'),
(52, 1, 'hh', 'ff', 'ff', 'ff', 2, 1.00, '2024', 'Active', '2025-09-23 09:11:38'),
(53, 1, 'DD', 'dd', 'dd', 'dd', 2, 1.00, '2022', 'Active', '2025-09-23 09:19:32'),
(54, 1, 'JJ', 'jj', 'jj', 'jj', 1, 1.00, '2001', 'Active', '2025-09-23 09:25:48'),
(55, 1, 'TODAY', 'today', 'today', 'today', 2, 4.00, '2024', 'Active', '2025-09-26 04:17:14'),
(56, 1, 'aa', 'aa', 'aa', 'aa', 2, 1.00, '2024', 'Active', '2025-09-27 13:12:29'),
(57, 1, 'qq', 'qq', 'qq', 'qq', 1, 12.00, '2024', 'Active', '2025-09-27 13:29:17'),
(58, 1, 'ww', 'ww', 'ww', 'ww', 2, 10.00, '2023', 'Active', '2025-09-28 01:16:03'),
(59, 1, 'ee', 'ee', 'uyy', 'uyy', 2, 10.00, '2024', 'Active', '2025-09-28 01:21:03'),
(60, 1, 'yy', 'yy', 'yy', 'yy', 2, 2.00, '2022', 'Active', '2025-09-28 04:59:43'),
(61, 1, 'BLENDER', 'ddd', 'ddd', 'dddd', 1, 2.00, '2023', 'Active', '2025-09-30 01:37:57'),
(63, 1, 'ggg', 'gg', 'gg', 'gg', 2, 1.00, '2004', 'Active', '2025-09-30 01:56:57'),
(64, 1, 'jjj', 'hshssh', 'hhh', 'hhh', 2, 2.00, '2023', 'Active', '2025-09-30 02:12:38'),
(65, 1, 'uu', 'gg', 'ggg', 'ggg', 2, 1.00, '2023', 'Active', '2025-09-30 02:14:13'),
(66, 1, 'p', 'o', 'o', 'o', 2, 10.00, '2024', 'Active', '2025-09-30 04:32:37'),
(67, 1, 'hays', 'hays', 'hays', 'hays', 2, 10.00, '2023', 'Active', '2025-09-30 04:46:48'),
(68, 1, 'Y', 'gg', 'bb', 'hh', 1, 2.00, '2023', 'Active', '2025-09-30 04:54:12'),
(70, 1, 'hagu', 'hh', 'hh', 'hh', 2, 1.00, '2023', 'Active', '2025-09-30 04:58:15'),
(71, 1, 'ho', 'ho', 'ho', 'ho', 2, 20.00, '2023', 'Active', '2025-09-30 05:00:08'),
(72, 1, 'BH DO', 'Calape', 'homey', 'm', 2, 10.00, '2023', 'Active', '2025-10-02 22:13:13'),
(73, 1, 'BH KIMB', 'Bangi', 'nnn', 'nnn', 2, 10.00, '2004', 'Active', '2025-10-03 01:09:28'),
(74, 1, 'Sunset Boarding House', '123 Main Street, Cebu City', 'A cozy boarding house near the university with modern amenities.', 'No smoking, No pets, Quiet hours 10PM-6AM', 3, 200.50, '2020', 'Active', '2025-10-04 12:46:17'),
(75, 4, 'Mountain View Lodge', '456 Oak Avenue, Cebu City', 'Beautiful boarding house with mountain views and fresh air.', 'Respect other residents, Keep common areas clean', 2, 150.75, '2019', 'Active', '2025-10-04 12:46:17'),
(76, 7, 'City Center Residence', '789 Pine Street, Cebu City', 'Conveniently located in the city center with easy access to everything.', 'No loud music, Clean up after yourself', 4, 300.00, '2021', 'Active', '2025-10-04 12:46:17'),
(77, 1, 'hh', 'hh', 'hh', 'hh', 2, 10.00, '2023', 'Active', '2025-10-05 03:13:35'),
(78, 1, 'bh', 'ttyyy', 'yyynnn', 'yyy', 2, 2.00, '2023', 'Active', '2025-10-08 16:48:19'),
(84, 1, 'test', 'calape', 'hg', 'hh', 2, 10.00, '2023', 'Active', '2025-10-09 02:30:04'),
(85, 29, 'BH 1', 'tinibgan,.calape', 'yy', 'yy', 2, 10.00, '2023', 'Active', '2025-10-12 03:48:57'),
(86, 29, 'Kikyam BH', 'Lucob, Calape, Bohol', 'This is a two storey building with aircon. Shalan!', 'No loud music from 9:00 PM - 6 AM', 2, 100.00, '2020', 'Active', '2025-10-28 02:25:43'),
(87, 29, 'Kikyam BH', 'Lucob, Calape, Bohol', 'This is a two storey building with aircon. Shalan!', 'No loud music from 9:00 PM - 6 AM', 2, 100.00, '2020', 'Active', '2025-10-28 02:25:46');

-- --------------------------------------------------------

--
-- Table structure for table `boarding_house_images`
--

CREATE TABLE `boarding_house_images` (
  `image_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  `image_path` varchar(255) NOT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `boarding_house_images`
--

INSERT INTO `boarding_house_images` (`image_id`, `bh_id`, `image_path`, `uploaded_at`) VALUES
(1, 23, 'uploads/boarding_house_images/bh_23_68d24f780fd2f.jpg', '2025-09-23 07:42:48'),
(2, 40, 'uploads/boarding_house_images/bh_40_68d2558daaa50.jpg', '2025-09-23 08:08:45'),
(3, 40, 'uploads/boarding_house_images/bh_40_68d25592d75ea.jpg', '2025-09-23 08:08:50'),
(4, 41, 'uploads/boarding_house_images/bh_41_68d25596a90f6.jpg', '2025-09-23 08:08:54'),
(5, 41, 'uploads/boarding_house_images/bh_41_68d2559b9c9e4.jpg', '2025-09-23 08:08:59'),
(6, 42, 'uploads/boarding_house_images/bh_42_68d256071445a.jpg', '2025-09-23 08:10:47'),
(7, 43, 'uploads/boarding_house_images/bh_43_68d259096cc4d.jpg', '2025-09-23 08:23:37'),
(8, 43, 'uploads/boarding_house_images/bh_43_68d25910057e4.jpg', '2025-09-23 08:23:44'),
(9, 44, 'uploads/boarding_house_images/bh_44_68d259aa3260b.jpg', '2025-09-23 08:26:18'),
(10, 44, 'uploads/boarding_house_images/bh_44_68d259af8b8a6.jpg', '2025-09-23 08:26:23'),
(11, 45, 'uploads/boarding_house_images/bh_45_68d25aebdf439.jpg', '2025-09-23 08:31:39'),
(12, 45, 'uploads/boarding_house_images/bh_45_68d25af178cac.jpg', '2025-09-23 08:31:45'),
(13, 46, 'uploads/boarding_house_images/bh_46_68d260349584a.jpg', '2025-09-23 08:54:12'),
(14, 46, 'uploads/boarding_house_images/bh_46_68d2603a8c884.jpg', '2025-09-23 08:54:18'),
(15, 46, 'uploads/boarding_house_images/bh_46_68d2604037c39.jpg', '2025-09-23 08:54:24'),
(16, 46, 'uploads/boarding_house_images/bh_46_68d26045d49b6.jpg', '2025-09-23 08:54:29'),
(17, 47, 'uploads/boarding_house_images/bh_47_68d26062d57a4.jpg', '2025-09-23 08:54:58'),
(18, 47, 'uploads/boarding_house_images/bh_47_68d2606820154.jpg', '2025-09-23 08:55:04'),
(19, 47, 'uploads/boarding_house_images/bh_47_68d2606ed2535.jpg', '2025-09-23 08:55:10'),
(20, 47, 'uploads/boarding_house_images/bh_47_68d2607457902.jpg', '2025-09-23 08:55:16'),
(21, 48, 'uploads/boarding_house_images/bh_48_68d260f53b0ab.jpg', '2025-09-23 08:57:25'),
(22, 48, 'uploads/boarding_house_images/bh_48_68d260fb671b6.jpg', '2025-09-23 08:57:31'),
(23, 48, 'uploads/boarding_house_images/bh_48_68d26101d45d8.jpg', '2025-09-23 08:57:37'),
(24, 48, 'uploads/boarding_house_images/bh_48_68d2610d8f72c.jpg', '2025-09-23 08:57:49'),
(25, 49, 'uploads/boarding_house_images/bh_49_68d261f95b0e5.jpg', '2025-09-23 09:01:45'),
(26, 49, 'uploads/boarding_house_images/bh_49_68d261ff47bad.jpg', '2025-09-23 09:01:51'),
(27, 53, 'uploads/boarding_house_images/bh_53_68d2662b1ba04.jpg', '2025-09-23 09:19:39'),
(28, 53, 'uploads/boarding_house_images/bh_53_68d2663361e30.jpg', '2025-09-23 09:19:47'),
(29, 54, 'uploads/boarding_house_images/bh_54_68d267a205cc3.jpg', '2025-09-23 09:25:54'),
(30, 54, 'uploads/boarding_house_images/bh_54_68d267a77adc3.jpg', '2025-09-23 09:25:59'),
(31, 55, 'uploads/boarding_house_images/bh_55_68d613cd96fbf.jpg', '2025-09-26 04:17:17'),
(32, 55, 'uploads/boarding_house_images/bh_55_68d613d046db3.jpg', '2025-09-26 04:17:20'),
(33, 56, 'uploads/boarding_house_images/bh_56_68d7e2c316bf5.jpg', '2025-09-27 13:12:35'),
(34, 56, 'uploads/boarding_house_images/bh_56_68d7e2c812370.jpg', '2025-09-27 13:12:40'),
(35, 59, 'uploads/boarding_house_images/bh_59_68d88d82ab3aa.jpg', '2025-09-28 01:21:06'),
(36, 59, 'uploads/boarding_house_images/bh_59_68d88d8503f68.jpg', '2025-09-28 01:21:09'),
(37, 59, 'uploads/boarding_house_images/bh_59_68d88d8781469.jpg', '2025-09-28 01:21:11'),
(38, 60, 'uploads/boarding_house_images/bh_60_68d8c0e6752c0.jpg', '2025-09-28 05:00:22'),
(41, 11, 'uploads/boarding_house_images/bh_11_68d8c1ed07598.jpg', '2025-09-28 05:04:45'),
(42, 11, 'uploads/boarding_house_images/bh_11_68da7ed55e253.jpg', '2025-09-29 12:43:01'),
(44, 12, 'uploads/boarding_house_images/bh_12_68da7fa24259f.jpg', '2025-09-29 12:46:26'),
(45, 12, 'uploads/boarding_house_images/bh_12_68da7fa64a9fc.jpg', '2025-09-29 12:46:30'),
(46, 12, 'uploads/boarding_house_images/bh_12_68da7facc64f8.jpg', '2025-09-29 12:46:36'),
(47, 12, 'uploads/boarding_house_images/bh_12_68da7fad6dd0f.jpg', '2025-09-29 12:46:37'),
(48, 12, 'uploads/boarding_house_images/bh_12_68da7fb054e3a.jpg', '2025-09-29 12:46:40'),
(49, 12, 'uploads/boarding_house_images/bh_12_68da7fb2b9586.jpg', '2025-09-29 12:46:42'),
(50, 13, 'uploads/boarding_house_images/bh_13_68da81d496477.jpg', '2025-09-29 12:55:48'),
(51, 13, 'uploads/boarding_house_images/bh_13_68da81d722967.jpg', '2025-09-29 12:55:51'),
(52, 13, 'uploads/boarding_house_images/bh_13_68da81d9d8b05.jpg', '2025-09-29 12:55:53'),
(53, 14, 'uploads/boarding_house_images/bh_14_68da835705d66.jpg', '2025-09-29 13:02:15'),
(54, 14, 'uploads/boarding_house_images/bh_14_68da8359e7824.jpg', '2025-09-29 13:02:17'),
(55, 12, 'uploads/boarding_house_images/bh_12_68da8624153b9.jpg', '2025-09-29 13:14:12'),
(56, 15, 'uploads/boarding_house_images/bh_15_68da872fb1706.jpg', '2025-09-29 13:18:39'),
(59, 16, 'uploads/boarding_house_images/bh_16_68da8f356d75c.jpg', '2025-09-29 13:52:53'),
(60, 16, 'uploads/boarding_house_images/bh_16_68da8f37f1d74.jpg', '2025-09-29 13:52:56'),
(61, 22, 'uploads/boarding_house_images/bh_22_68da9155827f3.jpg', '2025-09-29 14:01:57'),
(62, 18, 'uploads/boarding_house_images/bh_18_68da98871b131.jpg', '2025-09-29 14:32:39'),
(63, 61, 'uploads/boarding_house_images/bh_61_68db3478b3e34.jpg', '2025-09-30 01:38:00'),
(64, 61, 'uploads/boarding_house_images/bh_61_68db347d5d74e.jpg', '2025-09-30 01:38:05'),
(67, 61, 'uploads/boarding_house_images/bh_61_68db34c4a8539.jpg', '2025-09-30 01:39:16'),
(68, 63, 'uploads/boarding_house_images/bh_63_68db38ecd65ae.jpg', '2025-09-30 01:57:00'),
(69, 64, 'uploads/boarding_house_images/bh_64_68db3c99e7d43.jpg', '2025-09-30 02:12:41'),
(70, 65, 'uploads/boarding_house_images/bh_65_68db3cf7b3a74.jpg', '2025-09-30 02:14:15'),
(71, 65, 'uploads/boarding_house_images/bh_65_68db3d259544f.jpg', '2025-09-30 02:15:01'),
(72, 72, 'uploads/boarding_house_images/bh_72_68def8fc1263f.jpg', '2025-10-02 22:13:16'),
(73, 73, 'uploads/boarding_house_images/bh_73_68df224bdd350.jpg', '2025-10-03 01:09:31'),
(75, 77, 'uploads/boarding_house_images/bh_77_68e1e2f8c0ac6.jpg', '2025-10-05 03:16:08'),
(76, 77, 'uploads/boarding_house_images/bh_77_68e1e4231be7b.jpg', '2025-10-05 03:21:07'),
(77, 78, 'uploads/boarding_house_images/bh_78_68e695df04939.jpg', '2025-10-08 16:48:31'),
(78, 78, 'uploads/boarding_house_images/bh_78_68e695f66b119.jpg', '2025-10-08 16:48:54'),
(79, 84, 'uploads/boarding_house_images/bh_84_68e71e2e738ab.jpg', '2025-10-09 02:30:06'),
(80, 85, 'uploads/boarding_house_images/bh_85_68eb25319895f.jpg', '2025-10-12 03:49:05'),
(81, 85, 'uploads/boarding_house_images/bh_85_68eb286b32357.jpg', '2025-10-12 04:02:51'),
(82, 87, 'uploads/boarding_house_images/bh_87_690029c015372.jpg', '2025-10-28 02:26:08');

-- --------------------------------------------------------

--
-- Table structure for table `boarding_house_rooms`
--

CREATE TABLE `boarding_house_rooms` (
  `bhr_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  `room_category` enum('Private Room','Bed Spacer') NOT NULL,
  `room_name` varchar(100) NOT NULL,
  `price` double(10,2) NOT NULL,
  `capacity` int(11) NOT NULL,
  `room_description` text DEFAULT NULL,
  `total_rooms` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `boarding_house_rooms`
--

INSERT INTO `boarding_house_rooms` (`bhr_id`, `bh_id`, `room_category`, `room_name`, `price`, `capacity`, `room_description`, `total_rooms`, `created_at`) VALUES
(1, 41, 'Private Room', 'Single Room', 5000.00, 2, '0', 3, '2025-09-23 08:09:01'),
(2, 42, 'Private Room', 'Single Room', 5000.00, 3, '0', 4, '2025-09-23 08:10:49'),
(3, 43, 'Private Room', 'Single Room', 4000.00, 2, '0', 3, '2025-09-23 08:23:46'),
(4, 44, 'Private Room', 'Single Room', 4000.00, 3, '0', 3, '2025-09-23 08:26:25'),
(5, 45, 'Private Room', 'Single Room', 4000.00, 3, '0', 3, '2025-09-23 08:31:47'),
(6, 46, 'Private Room', 'Double', 10000.00, 5, '0', 4, '2025-09-23 08:54:31'),
(7, 47, 'Private Room', 'Single', 10000.00, 5, '0', 4, '2025-09-23 08:55:18'),
(8, 48, 'Private Room', 'Single', 10000.00, 5, '0', 4, '2025-09-23 08:57:51'),
(9, 49, 'Bed Spacer', 'Group A', 5000.00, 5, '0', 5, '2025-09-23 09:01:53'),
(10, 50, 'Private Room', 'Single', 5000.00, 3, '0', 1, '2025-09-23 09:05:48'),
(12, 52, 'Private Room', 'Double', 4000.00, 2, '0', 1, '2025-09-23 09:11:40'),
(13, 53, 'Private Room', 'Double', 5000.00, 4, '0', 1, '2025-09-23 09:19:49'),
(14, 54, 'Bed Spacer', 'Group B', 8000.00, 4, '0', 1, '2025-09-23 09:26:01'),
(15, 55, 'Private Room', 'Family Room', 8000.00, 5, '0', 2, '2025-09-26 04:17:22'),
(16, 56, 'Private Room', 'SINGLE', 1000.00, 1, '0', 2, '2025-09-27 13:12:42'),
(17, 57, 'Private Room', 'Single Room', 2900.00, 3, '0', 1, '2025-09-27 13:29:19'),
(18, 58, 'Private Room', 'Family', 9000.00, 5, '0', 2, '2025-09-28 01:16:05'),
(19, 59, 'Private Room', 'Family', 2000.00, 3, '0', 1, '2025-09-28 01:21:13'),
(20, 60, 'Bed Spacer', 'Group C', 2000.00, 6, '0', 1, '2025-09-28 04:59:46'),
(21, 63, 'Private Room', 'Single', 2000.00, 2, '0', 1, '2025-09-30 01:57:02'),
(22, 64, 'Private Room', 'Single', 2000.00, 2, '0', 1, '2025-09-30 02:12:44'),
(23, 65, 'Private Room', 'Single', 2999.00, 3, '0', 1, '2025-09-30 02:14:17'),
(24, 11, 'Private Room', 'Single A', 2000.00, 3, 'homey', 3, '2025-09-30 03:30:49'),
(25, 11, 'Bed Spacer', 'Group B', 1000.00, 5, 'bigg', 1, '2025-09-30 03:44:05'),
(26, 13, 'Private Room', 'Family', 10000.00, 5, '0', 1, '2025-09-30 03:48:18'),
(28, 12, 'Private Room', 'Single A', 5000.00, 2, '1', 2, '2025-09-30 04:25:25'),
(29, 66, 'Private Room', 'Single', 5000.00, 3, '0', 1, '2025-09-30 04:32:39'),
(31, 11, '', 'Test Room', 1000.00, 2, '0', 1, '2025-09-30 04:39:43'),
(33, 67, 'Private Room', 'Single', 5000.00, 2, '10', 1, '2025-09-30 04:46:50'),
(34, 68, 'Private Room', 'Single', 2000.00, 2, 'home', 1, '2025-09-30 04:54:15'),
(36, 70, 'Private Room', 'Single', 3000.00, 2, 'home', 1, '2025-09-30 04:58:17'),
(37, 71, 'Private Room', 'Single', 2000.00, 2, 'ho', 1, '2025-09-30 05:00:10'),
(38, 72, 'Private Room', 'Single Room', 5000.00, 2, 'good for', 2, '2025-10-02 22:13:18'),
(39, 72, 'Bed Spacer', 'Group', 1000.00, 5, 'good', 2, '2025-10-02 22:14:59'),
(40, 11, 'Private Room', 'Kim Hauz and Room', 900.00, 10, 'Room availability', 12, '2025-10-03 00:52:21'),
(41, 12, 'Private Room', 'Single A', 1000.00, 2, 'hhh', 1, '2025-10-03 00:58:20'),
(42, 73, 'Private Room', 'Family Room', 8000.00, 3, 'family', 2, '2025-10-03 01:09:34'),
(43, 77, 'Private Room', 'Single', 10000.00, 5, 'homeyy is the key', 1, '2025-10-05 03:13:42'),
(44, 78, 'Private Room', 'Single A', 4000.00, 2, 'homeeeeyyy', 1, '2025-10-08 16:48:33'),
(45, 84, 'Private Room', 'Single A', 4000.00, 2, 'homeee', 1, '2025-10-09 02:30:08'),
(46, 85, 'Private Room', 'single a', 2009.00, 2, 'hhhhooo', 1, '2025-10-12 03:49:07'),
(47, 85, 'Bed Spacer', 'Group A', 1000.00, 4, 'manyyy', 2, '2025-10-12 03:54:52'),
(48, 85, 'Private Room', 'Room 2', 5000.00, 2, 'Just a vibe', 1, '2025-10-24 06:47:52'),
(49, 87, 'Private Room', 'Private Room 01', 5000.00, 5, 'Can occupy 5 person', 1, '2025-10-28 02:26:10');

-- --------------------------------------------------------

--
-- Table structure for table `bookings`
--

CREATE TABLE `bookings` (
  `booking_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `booking_status` enum('Pending','Confirmed','Cancelled','Completed') NOT NULL DEFAULT 'Pending',
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `chat_groups`
--

CREATE TABLE `chat_groups` (
  `gc_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  `gc_name` varchar(100) NOT NULL,
  `gc_created_by` int(11) NOT NULL,
  `gc_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chat_groups`
--

INSERT INTO `chat_groups` (`gc_id`, `bh_id`, `gc_name`, `gc_created_by`, `gc_created_at`) VALUES
(4, 11, 'BH CUAS Chat', 1, '2025-10-04 12:50:44'),
(5, 12, 'BH CUASS Residents', 1, '2025-10-04 12:50:44'),
(6, 15, 'BH MANTE Discussion', 1, '2025-10-03 12:50:44'),
(7, 11, 'BH CUAS Chat', 1, '2025-10-04 12:56:44'),
(8, 12, 'BH CUASS Residents', 1, '2025-10-04 12:56:44'),
(9, 15, 'BH MANTE Discussion', 1, '2025-10-03 12:56:44'),
(11, 85, 'Test Group A', 29, '2025-10-14 03:58:45'),
(12, 85, 'Group b', 29, '2025-10-14 04:00:05'),
(13, 85, 'Group C', 29, '2025-10-14 07:24:42');

-- --------------------------------------------------------

--
-- Table structure for table `device_tokens`
--

CREATE TABLE `device_tokens` (
  `token_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `device_token` varchar(255) NOT NULL,
  `device_type` enum('android','ios','web') DEFAULT 'android',
  `app_version` varchar(50) DEFAULT '1.0.0',
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `device_tokens`
--

INSERT INTO `device_tokens` (`token_id`, `user_id`, `device_token`, `device_type`, `app_version`, `is_active`, `created_at`, `updated_at`) VALUES
(10, 1, 'doIZWxHNRkqo_lVUVcNn6a:APA91bGvBwcxisdLz9oNw6CJB1gKSaqz0HmNSLqgOfua9_R_X97IWRIas6HSV0CS4m1LoSMwI2bX959PyMn-vDmxy2K8yIkptrFx8nyzNyaWib5IYH3-0PM', 'android', '1.0.0', 0, '2025-10-09 02:53:46', '2025-10-09 03:02:48'),
(11, 1, 'cfE4VW8eRFeGZjIiX1nWoi:APA91bFpYILFXsXlM5oOcoDbaAPtoUsFq2ylML7OG4kOajLO72qOziZY5jscHR5VDAkpmM8FTZUhdbitQxUaYFPqdBcUQPB-slJWrrz5thBNus6J380csCQ', 'android', '1.0.0', 0, '2025-10-09 03:02:48', '2025-10-09 03:05:51'),
(12, 1, 'cvivWukjRtuy1HWtqnBvZC:APA91bG-4_hUVl1_ElHRbEthGqwOuuGMUwTveK3bYNG-GXYPxXQQeRoQ2SJxmM_coHNE7YCJXRiiLGJyaKcMwYsbxmzxbIRbblxWsOpwSdnU3oAukVHG45I', 'android', '1.0.0', 1, '2025-10-09 03:05:51', '2025-10-09 03:05:51'),
(13, 29, 'cvivWukjRtuy1HWtqnBvZC:APA91bG-4_hUVl1_ElHRbEthGqwOuuGMUwTveK3bYNG-GXYPxXQQeRoQ2SJxmM_coHNE7YCJXRiiLGJyaKcMwYsbxmzxbIRbblxWsOpwSdnU3oAukVHG45I', 'android', '1.0.0', 0, '2025-10-12 03:33:02', '2025-10-12 05:10:07'),
(14, 24, 'cvivWukjRtuy1HWtqnBvZC:APA91bG-4_hUVl1_ElHRbEthGqwOuuGMUwTveK3bYNG-GXYPxXQQeRoQ2SJxmM_coHNE7YCJXRiiLGJyaKcMwYsbxmzxbIRbblxWsOpwSdnU3oAukVHG45I', 'android', '1.0.0', 0, '2025-10-12 04:36:07', '2025-10-14 02:24:32'),
(15, 6, 'cvivWukjRtuy1HWtqnBvZC:APA91bG-4_hUVl1_ElHRbEthGqwOuuGMUwTveK3bYNG-GXYPxXQQeRoQ2SJxmM_coHNE7YCJXRiiLGJyaKcMwYsbxmzxbIRbblxWsOpwSdnU3oAukVHG45I', 'android', '1.0.0', 1, '2025-10-12 04:37:15', '2025-10-12 04:37:15'),
(16, 29, 'f4s7iqzjRtiPhdh0hIia0t:APA91bEhK5oDk51TwRrtatuoJ1kRW7yPve8zhJ-Fi1NAhFwXJfPv-uVQ76rCTe1SPUxbWdahWG6Pz1WsiOZlB1cbvAgaG4m-tmlRGmNmQGSKBSIhjPDHOiI', 'android', '1.0.0', 0, '2025-10-12 05:10:07', '2025-10-12 05:25:49'),
(17, 29, 'cLsLWCccSKKVeX-J0jNLY2:APA91bHs8noetyjaDSli4BhNW1-d6_IjUBjxg2p4sIc5yonRjsh8llOelWp50fiAo__dToRGpm6hDiTTAaGONxqi7vD3fP8qcEFiMxwpCZjtJbvhNqptlhU', 'android', '1.0.0', 0, '2025-10-12 05:25:49', '2025-10-12 05:38:13'),
(18, 29, 'dAXDgbwuQLyxAEpSsU24Am:APA91bHtj93rIkmbpb5x7f5WszdR1eM5929L-cTWkwrk_d4Qkpq8ZR939K48_ruM07BTmIhYscW6_r4xSvYi-3iOo2ehnXWcV0HBbQ9usaRwV1bbXxxS1Ak', 'android', '1.0.0', 0, '2025-10-12 05:38:13', '2025-10-27 01:36:23'),
(19, 24, 'dAXDgbwuQLyxAEpSsU24Am:APA91bHtj93rIkmbpb5x7f5WszdR1eM5929L-cTWkwrk_d4Qkpq8ZR939K48_ruM07BTmIhYscW6_r4xSvYi-3iOo2ehnXWcV0HBbQ9usaRwV1bbXxxS1Ak', 'android', '1.0.0', 1, '2025-10-14 02:24:32', '2025-10-14 02:24:32'),
(20, 29, 'f7SS5GQyRL6yFRqlf10SZ9:APA91bHDlsLELpVloaU2Dz97xSIgK2wJnUihuPhwGGCAgTSQSPXZdKOvyHmVkMbIcQj-ETALUG_cJLhiJzQ302Xf4sZFvWT_TtoOnWJQSRedsHJj0Zkl-zw', 'android', '1.0.0', 1, '2025-10-24 07:10:22', '2025-10-28 23:39:01'),
(21, 29, 'eLd7YhTVRHqp7J75n5t0y3:APA91bF4ovvMnFaHY7IeMoxWGjJRiR4tYAPL-jEDDTh2kGClJLkKH6OZISQeb5YEbtpyLAx_0mWIzpDfVfkWtLxeGUusP8ShvKkVMmaS3WBkxplNaTFSP2c', 'android', '1.0.0', 0, '2025-10-24 23:14:44', '2025-10-27 23:23:24'),
(22, 36, 'dAXDgbwuQLyxAEpSsU24Am:APA91bHtj93rIkmbpb5x7f5WszdR1eM5929L-cTWkwrk_d4Qkpq8ZR939K48_ruM07BTmIhYscW6_r4xSvYi-3iOo2ehnXWcV0HBbQ9usaRwV1bbXxxS1Ak', 'android', '1.0.0', 0, '2025-10-27 00:20:07', '2025-10-27 23:24:45'),
(23, 29, 'fH4UJ38_SG6JP_XHlTlcN1:APA91bHgQxZxSi6VSfTywAXYAn2kN_-GnMZdLjWahSMRQbO93zZ9wmdmT3ndnAekuETCZ9W4TaC8m6XS8gFOVMNJggcueUf7UiOZO4bxioHYqlkBN--RpZE', 'android', '1.0.0', 0, '2025-10-27 01:36:23', '2025-10-27 23:23:24'),
(24, 29, 'fTbyP38mRYmoXKxi-YRLlB:APA91bFDsW1PJw0G2nMo-PQHsx6pzlTYdbJQy3i6Bm25z8e5Hgim9iLnwky5bQxRB-Dvinnd4HtUuJYuJJdqVdV6tnIF1Z2NR_K4Xrjyr5BrP96Tub3ZxMk', 'android', '1.0.0', 1, '2025-10-27 23:23:24', '2025-10-28 12:49:58'),
(25, 36, 'fTbyP38mRYmoXKxi-YRLlB:APA91bFDsW1PJw0G2nMo-PQHsx6pzlTYdbJQy3i6Bm25z8e5Hgim9iLnwky5bQxRB-Dvinnd4HtUuJYuJJdqVdV6tnIF1Z2NR_K4Xrjyr5BrP96Tub3ZxMk', 'android', '1.0.0', 1, '2025-10-27 23:24:45', '2025-10-27 23:24:45'),
(26, 29, 'dlZlJq5IRaeE3Uyfp0CQfL:APA91bHxfiyKjaXaoij-dQrdBkV9_NX4t-uOQ2QzjcIwSHGiJkI_us9PTBL5JS0aNuBxbYyr5nkj4a_ACrttvyBu_rHhzv19VNdQkEoQl0E2nHoHY2BXkrU', 'android', '1.0.0', 1, '2025-10-28 00:40:50', '2025-10-28 00:40:50'),
(27, 39, 'fTbyP38mRYmoXKxi-YRLlB:APA91bFDsW1PJw0G2nMo-PQHsx6pzlTYdbJQy3i6Bm25z8e5Hgim9iLnwky5bQxRB-Dvinnd4HtUuJYuJJdqVdV6tnIF1Z2NR_K4Xrjyr5BrP96Tub3ZxMk', 'android', '1.0.0', 1, '2025-10-28 11:36:06', '2025-10-28 11:36:06'),
(28, 40, 'fTbyP38mRYmoXKxi-YRLlB:APA91bFDsW1PJw0G2nMo-PQHsx6pzlTYdbJQy3i6Bm25z8e5Hgim9iLnwky5bQxRB-Dvinnd4HtUuJYuJJdqVdV6tnIF1Z2NR_K4Xrjyr5BrP96Tub3ZxMk', 'android', '1.0.0', 1, '2025-10-28 11:50:22', '2025-10-28 11:50:22');

-- --------------------------------------------------------

--
-- Table structure for table `email_verifications`
--

CREATE TABLE `email_verifications` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `verification_code` varchar(6) NOT NULL,
  `expiry_time` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `email_verifications`
--

INSERT INTO `email_verifications` (`id`, `user_id`, `email`, `verification_code`, `expiry_time`, `created_at`) VALUES
(39, 91, 'lizacuas975@gmail.com', '204684', '2025-10-29 04:14:52', '2025-10-29 02:44:52');

-- --------------------------------------------------------

--
-- Table structure for table `group_members`
--

CREATE TABLE `group_members` (
  `gm_id` int(11) NOT NULL,
  `gc_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `gm_role` enum('Owner','Boarder','Admin') DEFAULT 'Boarder',
  `gm_joined_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `group_members`
--

INSERT INTO `group_members` (`gm_id`, `gc_id`, `user_id`, `gm_role`, `gm_joined_at`) VALUES
(1, 11, 28, '', '2025-10-14 03:58:45'),
(2, 11, 1, '', '2025-10-14 03:58:45'),
(3, 11, 29, '', '2025-10-14 03:58:45'),
(4, 12, 28, '', '2025-10-14 04:00:05'),
(5, 12, 1, '', '2025-10-14 04:00:05'),
(6, 12, 29, '', '2025-10-14 04:00:05'),
(7, 13, 28, '', '2025-10-14 07:24:42'),
(8, 13, 1, '', '2025-10-14 07:24:42');

-- --------------------------------------------------------

--
-- Table structure for table `group_messages`
--

CREATE TABLE `group_messages` (
  `groupmessage_id` int(11) NOT NULL,
  `gc_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `groupmessage_text` text NOT NULL,
  `groupmessage_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `groupmessage_status` enum('Sent','Delivered','Read') DEFAULT 'Sent'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `group_messages`
--

INSERT INTO `group_messages` (`groupmessage_id`, `gc_id`, `sender_id`, `groupmessage_text`, `groupmessage_timestamp`, `groupmessage_status`) VALUES
(1, 12, 29, 'hi guys', '2025-10-14 04:08:15', 'Read'),
(2, 12, 28, 'heyy', '2025-10-14 05:07:39', 'Read'),
(3, 12, 29, 'hiiii!!', '2025-10-14 05:08:31', 'Read'),
(4, 12, 28, 'samay', '2025-10-14 05:09:19', 'Read'),
(5, 12, 29, 'what?', '2025-10-14 05:11:44', 'Read'),
(6, 12, 28, 'yeahhh', '2025-10-14 05:13:16', 'Read'),
(7, 12, 29, 'huh', '2025-10-14 05:17:09', 'Read'),
(8, 12, 28, 'nooo', '2025-10-14 05:20:31', 'Read'),
(9, 12, 28, 'why', '2025-10-14 05:20:36', 'Read'),
(10, 12, 29, 'huh', '2025-10-14 05:30:03', 'Read'),
(11, 12, 29, 'nothing', '2025-10-14 05:32:56', 'Read'),
(12, 12, 28, 'huhuhu', '2025-10-14 05:38:07', 'Read'),
(13, 12, 28, 'wahatttt', '2025-10-14 05:41:30', 'Read'),
(14, 12, 29, 'huh', '2025-10-14 05:50:02', 'Read'),
(15, 12, 28, 'saman', '2025-10-14 05:51:01', 'Read'),
(16, 12, 29, 'wala man', '2025-10-14 05:59:53', 'Read'),
(17, 12, 28, 'huy', '2025-10-14 06:01:38', 'Read'),
(18, 12, 29, 'uy', '2025-10-14 06:06:27', 'Read'),
(19, 12, 29, 'uy', '2025-10-14 06:06:38', 'Read'),
(20, 12, 28, 'uy pud', '2025-10-14 06:07:20', 'Read'),
(21, 12, 28, 'unsa ba', '2025-10-14 06:12:32', 'Read'),
(22, 12, 29, 'wala lageh', '2025-10-14 06:14:07', 'Read'),
(23, 12, 28, 'heyyy', '2025-10-14 06:32:37', 'Read'),
(24, 12, 29, 'hiii', '2025-10-14 07:19:39', 'Read'),
(25, 12, 28, 'hey', '2025-10-14 07:47:49', 'Read'),
(26, 12, 28, 'wahta', '2025-10-14 08:14:24', 'Read'),
(27, 12, 29, 'wala', '2025-10-14 08:15:24', 'Read'),
(28, 12, 29, 'gegewg', '2025-10-14 08:51:22', 'Read'),
(29, 12, 29, 'tarung', '2025-10-14 08:58:31', 'Read'),
(30, 12, 28, 'lage', '2025-10-14 09:14:37', 'Read'),
(31, 12, 28, 'hi', '2025-10-14 09:18:34', 'Read'),
(32, 12, 29, 'hello', '2025-10-14 09:19:23', 'Read'),
(33, 12, 29, 'hi guys', '2025-10-23 13:35:35', 'Read'),
(34, 12, 29, 'yesss', '2025-10-28 12:51:24', 'Read'),
(35, 12, 28, 'hiii', '2025-10-28 13:06:13', 'Read');

-- --------------------------------------------------------

--
-- Table structure for table `maintenance_requests`
--

CREATE TABLE `maintenance_requests` (
  `request_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `mr_description` text NOT NULL,
  `mr_status` enum('Pending','In Progress','Resolved') NOT NULL DEFAULT 'Pending',
  `mr_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `message_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `msg_text` text NOT NULL,
  `msg_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `msg_status` enum('Sent','Delivered','Read') DEFAULT 'Sent'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `messages`
--

INSERT INTO `messages` (`message_id`, `sender_id`, `receiver_id`, `msg_text`, `msg_timestamp`, `msg_status`) VALUES
(1, 1, 2, 'Hello! Welcome to our boarding house.', '2025-10-04 10:57:56', ''),
(2, 2, 1, 'Thank you! I\'m excited to be here.', '2025-10-04 10:57:56', 'Read'),
(3, 1, 2, 'If you need anything, just let me know.', '2025-10-04 11:57:56', ''),
(5, 4, 6, 'Good morning! How are you settling in?', '2025-10-04 11:57:56', 'Read'),
(6, 6, 4, 'Everything is great, thank you!', '2025-10-04 12:12:56', 'Read'),
(15, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:04:54', ''),
(16, 2, 1, 'hiii', '2025-10-04 14:05:34', 'Read'),
(18, 6, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:13:48', 'Sent'),
(19, 6, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:13:53', 'Read'),
(20, 6, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:13:57', 'Read'),
(22, 6, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:23:52', 'Sent'),
(23, 6, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-04 14:23:59', 'Read'),
(32, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-05 01:36:25', ''),
(33, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-05 01:46:18', ''),
(34, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-05 01:48:19', ''),
(35, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-05 02:01:51', ''),
(36, 2, 1, '🔔 Test Message Badge - 13:36:11', '2025-10-05 05:36:11', 'Read'),
(37, 2, 1, '🔔 Test Message Badge - 13:37:20', '2025-10-05 05:37:20', 'Read'),
(38, 2, 1, '🔔 Test Message Badge - 13:40:27', '2025-10-05 05:40:27', 'Read'),
(39, 2, 1, '🔔 Test Message Badge - 14:39:57', '2025-10-05 06:39:57', 'Read'),
(40, 1, 2, 'Test message from PHP', '2025-10-05 08:13:10', ''),
(41, 1, 6, 'hi', '2025-10-05 08:14:09', 'Read'),
(42, 1, 6, 'hi', '2025-10-05 08:14:12', 'Read'),
(43, 1, 2, 'Test message from PHP', '2025-10-05 08:20:05', ''),
(44, 1, 6, 'hhiii', '2025-10-05 08:22:02', 'Read'),
(45, 1, 6, 'hhiii', '2025-10-05 08:22:05', 'Read'),
(46, 1, 2, 'Test message from PHP', '2025-10-05 08:27:01', ''),
(47, 1, 6, 'hooo', '2025-10-05 08:29:11', 'Read'),
(48, 1, 6, 'hooo', '2025-10-05 08:29:14', 'Read'),
(49, 1, 2, 'uouu', '2025-10-05 08:29:52', ''),
(50, 1, 2, 'uouu', '2025-10-05 08:29:55', ''),
(51, 1, 2, 'Test message from PHP', '2025-10-05 08:34:29', ''),
(52, 1, 2, 'bitaw', '2025-10-05 08:41:51', ''),
(53, 1, 2, 'bitaw', '2025-10-05 08:41:53', ''),
(54, 1, 6, 'how about me', '2025-10-05 08:55:01', 'Read'),
(55, 1, 6, 'how about me', '2025-10-05 08:55:03', 'Read'),
(56, 1, 6, 'huy', '2025-10-05 09:20:38', 'Read'),
(57, 1, 6, 'huy', '2025-10-05 09:20:40', 'Read'),
(58, 1, 2, 'hey', '2025-10-05 09:22:12', ''),
(59, 1, 2, 'hey', '2025-10-05 09:22:15', ''),
(60, 1, 6, 'huy pud', '2025-10-05 09:27:49', 'Read'),
(61, 1, 6, 'huy pud', '2025-10-05 09:27:51', 'Read'),
(62, 1, 6, 'huy ba', '2025-10-05 09:28:10', 'Read'),
(63, 1, 6, 'huy ba', '2025-10-05 09:28:12', 'Read'),
(64, 1, 2, 'hello', '2025-10-05 09:28:29', ''),
(65, 1, 2, 'hello', '2025-10-05 09:28:31', ''),
(66, 1, 2, 'ouhh', '2025-10-05 09:35:00', ''),
(67, 1, 2, 'ouhh', '2025-10-05 09:35:02', ''),
(68, 1, 6, 'low', '2025-10-05 09:41:58', 'Read'),
(69, 1, 6, 'low', '2025-10-05 09:42:00', 'Read'),
(70, 1, 2, 'huyy', '2025-10-05 10:40:08', ''),
(71, 1, 2, 'huyy', '2025-10-05 10:40:11', ''),
(74, 1, 6, 'lowbat', '2025-10-05 10:41:10', 'Read'),
(75, 1, 6, 'lowbat', '2025-10-05 10:41:13', 'Read'),
(77, 1, 2, 'yes', '2025-10-05 11:00:32', 'Sent'),
(78, 1, 2, 'yes', '2025-10-05 11:00:34', 'Sent'),
(82, 1, 2, 'no', '2025-10-05 12:19:52', 'Sent'),
(83, 1, 2, 'no', '2025-10-05 12:19:55', 'Sent'),
(84, 1, 2, 'favri', '2025-10-05 12:24:37', 'Sent'),
(85, 1, 2, 'favri', '2025-10-05 12:24:39', 'Sent'),
(86, 1, 2, 'dam', '2025-10-05 12:29:10', 'Sent'),
(87, 1, 2, 'dam', '2025-10-05 12:29:12', 'Sent'),
(88, 1, 2, 'waley', '2025-10-05 12:29:29', 'Sent'),
(89, 1, 2, 'waley', '2025-10-05 12:29:31', 'Sent'),
(90, 1, 6, 'bat', '2025-10-05 12:30:29', 'Read'),
(91, 1, 6, 'bat', '2025-10-05 12:30:31', 'Read'),
(92, 1, 6, 'hey', '2025-10-05 12:34:56', 'Read'),
(93, 1, 6, 'hey', '2025-10-05 12:34:59', 'Read'),
(94, 1, 6, 'woi', '2025-10-05 12:38:33', 'Read'),
(96, 4, 1, 'hays', '2025-10-05 12:44:32', 'Read'),
(97, 4, 1, 'gaba gajud ni', '2025-10-05 12:45:02', 'Read'),
(98, 1, 4, 'kims', '2025-10-05 12:45:39', 'Sent'),
(99, 4, 1, 'yes', '2025-10-05 12:45:49', 'Read'),
(100, 4, 1, 'hi', '2025-10-05 12:51:27', 'Read'),
(101, 4, 1, 'hiii', '2025-10-05 12:52:01', 'Read'),
(102, 1, 6, 'REAL-TIME TEST MESSAGE 1759668852', '2025-10-05 12:54:12', 'Read'),
(103, 1, 6, 'API TEST MESSAGE 1759668852', '2025-10-05 12:54:14', 'Read'),
(104, 4, 1, 'yy', '2025-10-05 12:54:59', 'Read'),
(105, 4, 1, 'no\r\n', '2025-10-05 12:55:27', 'Read'),
(106, 1, 4, 'yesss', '2025-10-07 01:23:58', 'Sent'),
(107, 1, 2, 'hi', '2025-10-07 01:24:26', 'Sent'),
(108, 1, 2, 'hi', '2025-10-07 01:24:56', 'Sent'),
(109, 1, 4, 'huy dapat sa babaw ka', '2025-10-07 01:25:34', 'Sent'),
(112, 1, 6, 'boboerns', '2025-10-07 01:26:18', 'Read'),
(113, 1, 2, 'haystt', '2025-10-07 01:26:33', 'Sent'),
(114, 1, 2, 'nooo', '2025-10-07 01:35:52', 'Sent'),
(115, 1, 2, 'ye', '2025-10-07 07:07:28', 'Sent'),
(116, 1, 2, 'heyy', '2025-10-08 15:08:54', 'Sent'),
(118, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:11:02', 'Sent'),
(119, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:17:38', 'Read'),
(120, 1, 2, 'okays', '2025-10-08 15:18:21', 'Sent'),
(121, 1, 2, 'huhu', '2025-10-08 15:21:57', 'Sent'),
(122, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:26:20', 'Read'),
(123, 1, 6, 'huhuhu', '2025-10-08 15:32:26', 'Read'),
(124, 1, 6, 'huyyy', '2025-10-08 15:40:41', 'Read'),
(125, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:40:54', 'Read'),
(126, 1, 6, 'huyyy', '2025-10-08 15:57:48', 'Read'),
(127, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:57:59', 'Read'),
(128, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 15:58:04', 'Read'),
(129, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 16:04:47', 'Read'),
(130, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 16:05:03', 'Read'),
(131, 2, 1, 'Test notification message - should pop up!', '2025-10-08 16:06:49', 'Read'),
(132, 1, 2, 'we', '2025-10-08 16:09:57', 'Sent'),
(133, 1, 2, 'weeeee', '2025-10-08 16:10:08', 'Sent'),
(134, 1, 4, 'bay', '2025-10-08 16:14:26', 'Sent'),
(135, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 16:15:25', 'Read'),
(136, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 16:16:11', 'Read'),
(139, 1, 6, 'hagua mn ka', '2025-10-08 16:21:00', 'Read'),
(140, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-08 16:27:48', 'Read'),
(141, 1, 6, 'uy', '2025-10-09 02:31:29', 'Read'),
(142, 1, 6, 'dina lageh ka mogana notif', '2025-10-09 02:31:37', 'Read'),
(143, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:36:35', 'Read'),
(144, 1, 6, 'woyyy', '2025-10-09 02:54:52', 'Read'),
(145, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:55:19', 'Read'),
(146, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:56:32', 'Read'),
(147, 4, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:59:06', 'Sent'),
(148, 4, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:59:23', 'Read'),
(149, 4, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 02:59:33', 'Read'),
(150, 1, 2, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:01:30', 'Sent'),
(151, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:01:44', 'Read'),
(152, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:03:22', 'Read'),
(153, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:03:30', 'Read'),
(154, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:03:36', 'Read'),
(156, 5, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:03:50', 'Read'),
(157, 2, 1, 'Hello! This is a test message from the real messaging system.', '2025-10-09 03:04:44', 'Read'),
(158, 2, 1, 'we\r\n', '2025-10-09 03:04:56', 'Read'),
(159, 2, 1, 'we\r\n', '2025-10-09 03:06:05', 'Read'),
(160, 2, 1, 'wala ', '2025-10-09 03:06:11', 'Read'),
(161, 2, 29, 'hi', '2025-10-12 04:07:20', 'Read'),
(162, 1, 6, 'woyyy', '2025-10-12 04:08:27', 'Read'),
(163, 2, 29, 'hello', '2025-10-12 04:24:07', 'Read'),
(164, 8, 29, 'hello', '2025-10-12 04:24:38', 'Read'),
(165, 29, 8, 'hi', '2025-10-12 04:25:01', 'Sent'),
(166, 29, 2, 'huy', '2025-10-12 04:25:29', 'Sent'),
(167, 29, 6, 'hi', '2025-10-12 04:25:34', 'Read'),
(168, 29, 6, 'hello po', '2025-10-12 04:30:13', 'Read'),
(169, 6, 29, 'hello', '2025-10-12 04:30:47', 'Read'),
(170, 24, 29, 'hello', '2025-10-12 04:31:36', 'Read'),
(171, 24, 29, 'hoo', '2025-10-12 04:32:01', 'Read'),
(172, 24, 29, 'hoo', '2025-10-12 05:10:43', 'Read'),
(173, 24, 29, 'hupay', '2025-10-12 05:39:12', 'Read'),
(174, 24, 29, 'huhuhuhu\r\n\r\n', '2025-10-12 05:39:32', 'Read'),
(175, 27, 29, 'https://open.spotify.com/playlist/37i9dQZF1E36NC4j9YSysy\r\n\r\n', '2025-10-12 05:40:09', 'Read'),
(176, 27, 29, 'huhuhu', '2025-10-12 05:43:29', 'Read'),
(177, 27, 29, 'huhuhu', '2025-10-12 05:43:42', 'Read'),
(178, 29, 28, 'hi', '2025-10-14 05:38:53', 'Read'),
(179, 28, 29, 'yes?', '2025-10-14 05:41:19', 'Read'),
(180, 28, 29, 'hays', '2025-10-14 05:50:52', 'Read'),
(181, 29, 28, 'yes?', '2025-10-14 05:59:59', 'Read'),
(182, 28, 29, 'aw wala raman', '2025-10-14 06:01:09', 'Read'),
(183, 28, 29, 'huy', '2025-10-14 06:01:33', 'Read'),
(184, 29, 28, 'uy', '2025-10-14 06:02:19', 'Read'),
(185, 28, 29, 'huh', '2025-10-14 06:07:35', 'Read'),
(186, 28, 29, 'unsa', '2025-10-14 06:12:19', 'Read'),
(187, 29, 28, 'wala lagrh', '2025-10-14 06:14:27', 'Read'),
(188, 28, 29, 'noo', '2025-10-14 06:32:48', 'Read'),
(189, 29, 28, 'hey', '2025-10-14 06:53:55', 'Read'),
(190, 29, 28, 'okay', '2025-10-14 07:19:58', 'Read'),
(191, 29, 28, 'huyyyy', '2025-10-14 07:29:00', 'Read'),
(192, 29, 28, 'ha', '2025-10-14 07:44:32', 'Read'),
(193, 28, 29, 'wala', '2025-10-14 07:47:57', 'Read'),
(194, 29, 28, 'hays', '2025-10-14 07:53:56', 'Read'),
(195, 29, 28, 'haysh', '2025-10-14 07:53:59', 'Read'),
(196, 29, 28, 'hays', '2025-10-14 07:54:03', 'Read'),
(197, 28, 29, 'what happen', '2025-10-14 08:14:33', 'Read'),
(198, 29, 28, 'wala mannnn', '2025-10-14 08:15:35', 'Read'),
(199, 28, 29, 'sure ka?', '2025-10-14 08:40:31', 'Read'),
(200, 28, 29, 'sure ba', '2025-10-14 08:56:48', 'Read'),
(201, 29, 28, 'lagehhh', '2025-10-14 08:58:21', 'Read'),
(202, 28, 29, 'huy', '2025-10-14 09:03:01', 'Read'),
(203, 28, 29, 'jjj', '2025-10-14 09:04:41', 'Read'),
(204, 28, 29, 'jjjjjjjjj', '2025-10-14 09:06:28', 'Read'),
(205, 28, 29, 'hakdog', '2025-10-14 09:06:43', 'Read'),
(206, 28, 29, 'kk', '2025-10-14 09:11:51', 'Read'),
(207, 28, 29, 'hi', '2025-10-14 09:18:43', 'Read'),
(208, 29, 28, 'hello', '2025-10-14 09:19:14', 'Read'),
(209, 28, 29, 'yes?', '2025-10-23 13:37:42', 'Read'),
(210, 29, 28, 'b**o', '2025-10-25 03:46:58', 'Read'),
(211, 29, 28, 't***a', '2025-10-25 03:47:07', 'Read'),
(212, 29, 28, 'f**k', '2025-10-25 03:47:14', 'Read'),
(213, 29, 28, 's****d', '2025-10-25 03:47:25', 'Read'),
(214, 29, 28, 't*****a', '2025-10-25 03:47:46', 'Read'),
(215, 29, 28, 'hi', '2025-10-25 03:47:48', 'Read'),
(216, 29, 28, 'boboha nimo', '2025-10-25 03:49:40', 'Read'),
(217, 29, 28, 'b**o', '2025-10-25 03:49:45', 'Read'),
(218, 29, 28, 'fucking s****d', '2025-10-25 03:51:07', 'Read'),
(219, 29, 28, 'your so f*****g s****d', '2025-10-25 03:53:20', 'Read'),
(220, 29, 28, 's**t', '2025-10-25 03:53:28', 'Read'),
(221, 29, 28, 'f*****g', '2025-10-25 04:51:40', 'Read'),
(222, 28, 29, 's**t', '2025-10-25 04:52:54', 'Read'),
(223, 29, 28, 'b******t', '2025-10-25 08:06:12', 'Read'),
(224, 29, 28, 's**t', '2025-10-27 01:29:41', 'Read'),
(225, 29, 28, 'b******t', '2025-10-27 01:29:51', 'Read'),
(226, 28, 29, 'namz', '2025-10-27 01:31:46', 'Read'),
(227, 29, 28, 'kim your so s****d', '2025-10-27 01:32:36', 'Read'),
(228, 28, 29, 'i don\'t care', '2025-10-27 01:33:14', 'Read'),
(229, 29, 28, 'okay', '2025-10-27 01:33:33', 'Read'),
(230, 29, 28, 's**t', '2025-10-27 01:39:10', 'Read'),
(231, 29, 28, 'b**o ka', '2025-10-27 01:39:24', 'Read'),
(232, 29, 28, 'hiii', '2025-10-28 12:50:41', 'Read'),
(233, 29, 28, 'hu', '2025-10-28 12:51:13', 'Read'),
(234, 29, 28, 'hiii', '2025-10-28 12:52:03', 'Read'),
(235, 28, 29, 'yes?', '2025-10-28 12:53:32', 'Read'),
(236, 28, 29, 'huyyy', '2025-10-28 12:59:38', 'Read'),
(237, 29, 28, 'hiiii', '2025-10-28 13:19:32', 'Sent');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `notif_title` varchar(150) NOT NULL,
  `notif_message` text NOT NULL,
  `notif_type` enum('booking','payment','announcement','maintenance','general') DEFAULT 'general',
  `notif_status` enum('unread','read') DEFAULT 'unread',
  `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`notif_id`, `user_id`, `notif_title`, `notif_message`, `notif_type`, `notif_status`, `notif_created_at`) VALUES
(1, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 03:32:22'),
(2, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 03:33:01'),
(3, 1, 'New Booking Request', 'You have a new booking request from Jane Smith for Room 205', 'booking', 'read', '2025-10-05 03:33:20'),
(4, 1, 'Payment Overdue', 'Your payment of ₱3,000.00 for Monthly Rent - December 2024 is overdue.', 'payment', 'read', '2025-10-05 03:33:21'),
(5, 1, 'Maintenance Completed', 'Maintenance for Elevator has been completed.', 'maintenance', 'read', '2025-10-05 03:33:21'),
(6, 1, '🚨 URGENT: Fire Drill', 'Fire drill will be conducted tomorrow at 2:00 PM. Please evacuate the building when the alarm sounds.', 'announcement', 'read', '2025-10-05 03:33:22'),
(7, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 03:59:21'),
(8, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:05:59'),
(9, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:06:46'),
(10, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:11:39'),
(11, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:20:49'),
(12, 1, 'New Test Notification', 'This is a new test notification to verify the badge system', 'general', 'read', '2025-10-05 04:24:21'),
(13, 1, 'New Booking Request', 'John Doe wants to book Room 101 for next month', 'booking', 'read', '2025-10-05 04:26:26'),
(14, 1, 'Payment Received', 'Payment of ₱3,500 received from Jane Smith', 'payment', 'read', '2025-10-05 04:26:26'),
(15, 1, 'Maintenance Alert', 'Elevator maintenance scheduled for tomorrow', 'maintenance', 'read', '2025-10-05 04:26:26'),
(16, 1, 'Important Announcement', 'Fire drill will be conducted next week', 'announcement', 'read', '2025-10-05 04:26:26'),
(17, 1, 'Welcome Message', 'Welcome to BoardEase! Your account is ready', 'general', 'read', '2025-10-05 04:26:26'),
(18, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:28:08'),
(19, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:28:24'),
(20, 1, 'New Booking Request', 'John Doe wants to book Room 101 for next month', 'booking', 'read', '2025-10-05 04:33:23'),
(21, 1, 'Payment Received', 'Payment of ₱3,500 received from Jane Smith', 'payment', 'read', '2025-10-05 04:33:23'),
(22, 1, 'Maintenance Alert', 'Elevator maintenance scheduled for tomorrow', 'maintenance', 'read', '2025-10-05 04:33:23'),
(23, 1, 'Important Announcement', 'Fire drill will be conducted next week', 'announcement', 'read', '2025-10-05 04:33:23'),
(24, 1, 'Welcome Message', 'Welcome to BoardEase! Your account is ready', 'general', 'read', '2025-10-05 04:33:23'),
(25, 1, 'Test Notification 1', 'This is a test notification', 'general', 'read', '2025-10-05 04:34:21'),
(26, 1, 'Test Notification 2', 'Another test notification', 'booking', 'read', '2025-10-05 04:34:21'),
(27, 1, 'Test Notification 3', 'Third test notification', 'payment', 'read', '2025-10-05 04:34:21'),
(28, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:35:21'),
(29, 1, 'New Booking Request', 'John Doe wants to book Room 101 for next month', 'booking', 'read', '2025-10-05 04:40:42'),
(30, 1, 'Payment Received', 'Payment of ₱3,500 received from Jane Smith', 'payment', 'read', '2025-10-05 04:40:42'),
(31, 1, 'Maintenance Alert', 'Elevator maintenance scheduled for tomorrow', 'maintenance', 'read', '2025-10-05 04:40:42'),
(32, 1, 'Important Announcement', 'Fire drill will be conducted next week', 'announcement', 'read', '2025-10-05 04:40:42'),
(33, 1, 'Welcome Message', 'Welcome to BoardEase! Your account is ready', 'general', 'read', '2025-10-05 04:40:42'),
(34, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 04:41:32'),
(35, 1, 'New Booking Request', 'John Doe wants to book Room 101 for next month', 'booking', 'read', '2025-10-05 04:42:29'),
(36, 1, 'Payment Received', 'Payment of ₱3,500 received from Jane Smith', 'payment', 'read', '2025-10-05 04:42:29'),
(37, 1, 'Maintenance Alert', 'Elevator maintenance scheduled for tomorrow', 'maintenance', 'read', '2025-10-05 04:42:29'),
(38, 1, 'Important Announcement', 'Fire drill will be conducted next week', 'announcement', 'read', '2025-10-05 04:42:29'),
(39, 1, 'Welcome Message', 'Welcome to BoardEase! Your account is ready', 'general', 'read', '2025-10-05 04:42:29'),
(40, 1, '🔔 REAL-TIME TEST', 'This notification was just created to test real-time badge updates!', 'general', 'read', '2025-10-05 04:48:37'),
(41, 1, 'New Booking', 'Someone wants to book your room', 'booking', 'read', '2025-10-05 04:49:10'),
(42, 1, 'Payment Alert', 'Payment received from tenant', 'payment', 'read', '2025-10-05 04:49:10'),
(43, 1, 'Maintenance', 'Elevator needs repair', 'maintenance', 'read', '2025-10-05 04:49:10'),
(44, 1, '🚨 URGENT: Fire Drill', 'Fire drill scheduled for tomorrow at 2:00 PM', 'announcement', 'read', '2025-10-05 04:50:16'),
(45, 1, '💰 Payment Received', 'Payment of ₱5,000 received from John Doe', 'payment', 'read', '2025-10-05 04:50:16'),
(46, 1, '🔧 Maintenance Alert', 'Elevator maintenance completed', 'maintenance', 'read', '2025-10-05 04:50:17'),
(47, 1, '📅 New Booking', 'Jane Smith wants to book Room 201', 'booking', 'read', '2025-10-05 04:50:17'),
(48, 1, '📢 General Notice', 'Water supply will be interrupted tomorrow', 'general', 'read', '2025-10-05 04:50:18'),
(49, 1, '🚨 URGENT: Fire Drill', 'Fire drill scheduled for tomorrow at 2:00 PM', 'announcement', 'read', '2025-10-05 04:53:21'),
(50, 1, '💰 Payment Received', 'Payment of ₱5,000 received from John Doe', 'payment', 'read', '2025-10-05 04:53:21'),
(51, 1, '🔧 Maintenance Alert', 'Elevator maintenance completed', 'maintenance', 'read', '2025-10-05 04:53:22'),
(52, 1, '📅 New Booking', 'Jane Smith wants to book Room 201', 'booking', 'read', '2025-10-05 04:53:22'),
(53, 1, '📢 General Notice', 'Water supply will be interrupted tomorrow', 'general', 'read', '2025-10-05 04:53:23'),
(54, 1, '🔔 New Message', 'You have a new message from tenant', 'general', 'read', '2025-10-05 04:56:04'),
(55, 1, '💰 Payment Alert', 'Rent payment received', 'payment', 'read', '2025-10-05 04:56:04'),
(56, 1, '📅 Booking Request', 'New booking request received', 'booking', 'read', '2025-10-05 04:56:04'),
(57, 1, '✅ Maintenance Complete', 'Elevator maintenance completed', 'maintenance', 'read', '2025-10-05 04:56:04'),
(58, 1, '📢 Announcement', 'Monthly meeting scheduled', 'announcement', 'read', '2025-10-05 04:56:04'),
(59, 1, '💳 Payment Processed', 'Utility bill payment processed', 'payment', 'read', '2025-10-05 04:56:04'),
(60, 1, '🔔 Test Notification 1', 'This is a test notification for debugging', 'general', 'read', '2025-10-05 05:00:37'),
(61, 1, '💰 Test Payment', 'Test payment notification', 'payment', 'read', '2025-10-05 05:00:37'),
(62, 1, '📅 Test Booking', 'Test booking notification', 'booking', 'read', '2025-10-05 05:00:37'),
(63, 1, '🔧 Test Maintenance', 'Test maintenance notification', 'maintenance', 'read', '2025-10-05 05:00:37'),
(64, 1, '📢 Test Announcement', 'Test announcement notification', 'announcement', 'read', '2025-10-05 05:00:37'),
(65, 1, '🔔 Test Notification 1', 'This is a test notification for debugging', 'general', 'read', '2025-10-05 05:03:49'),
(66, 1, '💰 Test Payment', 'Test payment notification', 'payment', 'read', '2025-10-05 05:03:49'),
(67, 1, '📅 Test Booking', 'Test booking notification', 'booking', 'read', '2025-10-05 05:03:49'),
(68, 1, '🔧 Test Maintenance', 'Test maintenance notification', 'maintenance', 'read', '2025-10-05 05:03:49'),
(69, 1, '📢 Test Announcement', 'Test announcement notification', 'announcement', 'read', '2025-10-05 05:03:49'),
(70, 1, '🔔 Test Notification 1', 'This is a test notification for debugging', 'general', 'read', '2025-10-05 05:06:51'),
(71, 1, '💰 Test Payment', 'Test payment notification', 'payment', 'read', '2025-10-05 05:06:51'),
(72, 1, '📅 Test Booking', 'Test booking notification', 'booking', 'read', '2025-10-05 05:06:51'),
(73, 1, '🔧 Test Maintenance', 'Test maintenance notification', 'maintenance', 'read', '2025-10-05 05:06:51'),
(74, 1, '📢 Test Announcement', 'Test announcement notification', 'announcement', 'read', '2025-10-05 05:06:51'),
(75, 1, '🔔 Badge Test Notification', 'This notification should show a badge count on your app! Check the notification icon.', 'general', 'read', '2025-10-05 05:09:43'),
(76, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:12:05'),
(77, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:12:06'),
(78, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:12:06'),
(79, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:12:06'),
(80, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:12:07'),
(81, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:14:53'),
(82, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:14:53'),
(83, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:14:54'),
(84, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:14:54'),
(85, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:14:54'),
(86, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:16:41'),
(87, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:16:41'),
(88, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:16:42'),
(89, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:16:42'),
(90, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:16:42'),
(91, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:16:45'),
(92, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:16:45'),
(93, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:16:46'),
(94, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:16:46'),
(95, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:16:46'),
(96, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:17:24'),
(97, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:17:25'),
(98, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:17:25'),
(99, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:17:25'),
(100, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:17:26'),
(101, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:19:01'),
(102, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:19:02'),
(103, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:19:02'),
(104, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:19:03'),
(105, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:19:03'),
(106, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:19:59'),
(107, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:22:10'),
(108, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:22:52'),
(109, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:23:17'),
(110, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:23:49'),
(111, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:27:33'),
(112, 1, '🔔 Test Badge Display', 'This notification is to test if the badge displays correctly in real-time.', 'general', 'read', '2025-10-05 05:28:44'),
(113, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:29:05'),
(114, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:29:05'),
(115, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:29:05'),
(116, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:29:06'),
(117, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:29:06'),
(118, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 05:29:36'),
(119, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 05:29:37'),
(120, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 05:29:37'),
(121, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 05:29:37'),
(122, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 05:29:38'),
(123, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 05:30:33'),
(124, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 05:31:07'),
(125, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 10:19:20'),
(126, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 10:19:25'),
(127, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 10:19:35'),
(128, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 10:19:47'),
(129, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 10:19:59'),
(130, 1, '🔔 NOTIFICATION WITH SOUND', 'This notification includes sound and FCM push notification! Check your device.', 'general', 'read', '2025-10-05 10:20:14'),
(131, 1, '💰 Payment Alert with Sound', 'Payment received! This notification has sound enabled.', 'payment', 'read', '2025-10-05 10:20:21'),
(132, 1, '📅 Booking Request with Sound', 'New booking request received with sound notification.', 'booking', 'read', '2025-10-05 10:20:34'),
(133, 1, '🔧 Maintenance Alert with Sound', 'Maintenance completed with sound notification.', 'maintenance', 'read', '2025-10-05 10:20:49'),
(134, 1, '📢 Announcement with Sound', 'Important announcement with sound notification.', 'announcement', 'read', '2025-10-05 10:21:01'),
(135, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 12:48:45'),
(136, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 12:49:15'),
(137, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-05 12:56:41'),
(138, 1, 'New Booking Request', 'You have a new booking request from Jane Smith for Room 205', 'booking', 'read', '2025-10-08 15:20:47'),
(139, 1, 'Payment Overdue', 'Your payment of ₱3,000.00 for Monthly Rent - December 2024 is overdue.', 'payment', 'read', '2025-10-08 15:20:47'),
(140, 1, 'Maintenance Completed', 'Maintenance for Elevator has been completed.', 'maintenance', 'read', '2025-10-08 15:20:47'),
(141, 1, '🚨 URGENT: Fire Drill', 'Fire drill will be conducted tomorrow at 2:00 PM. Please evacuate the building when the alarm sounds.', 'announcement', 'read', '2025-10-08 15:20:47'),
(142, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-08 15:21:01'),
(143, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-08 15:22:22'),
(144, 1, 'Test Notification', 'This is a test notification to verify the system is working.', 'general', 'read', '2025-10-08 15:22:30'),
(145, 1, 'System Test', 'Testing notification system after fix', 'general', 'read', '2025-10-08 15:22:55'),
(146, 1, 'System Fixed! 🎉', 'Your notification system is now working properly. This is a test notification to confirm everything is working.', 'general', 'read', '2025-10-08 15:23:37'),
(147, 1, 'System Fixed! 🎉', 'Your notification system is now working properly.', 'general', 'read', '2025-10-08 15:24:03'),
(148, 1, 'System Fixed! 🎉', 'Your notification system is now working properly.', 'general', 'read', '2025-10-08 15:25:05'),
(149, 1, 'System Status Check', 'Notification system is working properly', 'general', 'read', '2025-10-08 15:25:42'),
(150, 1, '🔔 Test Popup Notification', 'This notification should appear as a popup on your device. If you can see this, the system is working!', 'general', 'read', '2025-10-08 15:27:00'),
(151, 1, '🔔 Test Notification', 'This is a test notification with sound and popup. Check your device!', 'general', 'read', '2025-10-08 15:46:34'),
(152, 1, '🔔 Notification Test', 'This notification should appear with sound and popup!', 'general', 'read', '2025-10-08 15:52:00'),
(153, 1, '🔔 POPUP TEST', 'This should pop up on your screen with sound!', 'general', 'read', '2025-10-08 16:04:19'),
(154, 1, 'Welcome to BoardEase!', 'Your account has been successfully set up. Start exploring our features!', 'general', 'read', '2025-10-09 03:10:08'),
(155, 1, 'New Booking Request', 'You have received a new booking request for \"Cozy Studio Apartment\" from Mike Johnson.', 'booking', 'read', '2025-10-09 03:10:10'),
(156, 1, 'Payment Reminder', 'Your monthly payment of ₱3,500 is due in 3 days. Please make your payment to avoid late fees.', 'payment', 'read', '2025-10-09 03:10:11'),
(157, 1, 'Maintenance Update', 'Your maintenance request for \"Broken faucet in bathroom\" has been completed. Please check and confirm.', 'maintenance', 'read', '2025-10-09 03:10:15'),
(158, 1, 'System Announcement', 'BoardEase will be undergoing scheduled maintenance on Sunday, 10:00 PM - 11:00 PM. Some features may be temporarily unavailable.', 'announcement', 'read', '2025-10-09 03:10:17'),
(159, 29, 'Welcome to BoardEase!', 'Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.', '', 'read', '2025-10-12 05:24:32'),
(160, 29, 'Welcome to BoardEase!', 'Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.', '', 'read', '2025-10-12 05:26:07'),
(161, 29, 'Welcome to BoardEase!', 'Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.', '', 'read', '2025-10-12 05:28:14'),
(162, 29, 'Welcome to BoardEase!', 'Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.', '', 'read', '2025-10-12 05:38:59'),
(163, 29, 'Welcome to BoardEase!', 'Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.', '', 'read', '2025-10-12 05:39:04');

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `payment_id` int(11) NOT NULL,
  `booking_id` int(11) DEFAULT NULL,
  `bill_id` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `payment_amount` decimal(10,2) NOT NULL,
  `payment_method` enum('Cash','GCash','Bank Transfer','Check') NOT NULL DEFAULT 'Cash',
  `payment_proof` text DEFAULT NULL,
  `payment_status` enum('Pending','Completed','Failed','Refunded') NOT NULL DEFAULT 'Pending',
  `payment_date` datetime NOT NULL DEFAULT current_timestamp(),
  `receipt_url` varchar(500) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `payment_month` varchar(7) NOT NULL,
  `payment_year` int(4) NOT NULL,
  `payment_month_number` int(2) NOT NULL,
  `is_monthly_payment` tinyint(1) NOT NULL DEFAULT 1,
  `total_months_required` int(3) DEFAULT NULL,
  `months_paid` int(3) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `registration`
--

CREATE TABLE `registration` (
  `reg_id` int(11) NOT NULL,
  `role` enum('Boarder','Owner') NOT NULL,
  `f_name` varchar(50) NOT NULL,
  `m_name` varchar(50) DEFAULT NULL,
  `l_name` varchar(50) NOT NULL,
  `birthdate` date NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `p_address` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `valid_id` varchar(50) NOT NULL,
  `front_id` varchar(255) DEFAULT NULL,
  `back_id` varchar(255) DEFAULT NULL,
  `id_number` varchar(50) NOT NULL,
  `gcash_qr` varchar(255) DEFAULT NULL,
  `gcash_number` varchar(15) NOT NULL,
  `status` enum('Approved','Pending','Declined') NOT NULL DEFAULT 'Pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `registration`
--

INSERT INTO `registration` (`reg_id`, `role`, `f_name`, `m_name`, `l_name`, `birthdate`, `phone_number`, `p_address`, `email`, `password`, `valid_id`, `front_id`, `back_id`, `id_number`, `gcash_qr`, `gcash_number`, `status`) VALUES
(1, 'Owner', 'John', 'Michael', 'Doe', '1985-03-15', '09123456789', '123 Main Street, Cebu City', 'john.doe@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456789', NULL, '09123456789', 'Approved'),
(2, 'Owner', 'Namz', 'Mm', 'Baer', '2004-09-10', '09171234568', 'Calape, Bohol', 'namzbaer@gmail.com', '$2y$10$Q.RNHpk7eHhoTHZTm2.11.RsRLhF/NbGeFVqUjI02MSTjLe9v9HTO', 'Passport', 'front_passport.jpg', 'back_passport.jpg', 'ID987654321', 'uploads/gcash_qr/gcash_qr_1_1759443376.jpg', '09925311409', 'Approved'),
(3, 'Boarder', 'Mike', 'James', 'Johnson', '1998-11-08', '09123456791', '789 Pine Street, Cebu City', 'mike.johnson@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456790', NULL, '09123456791', 'Approved'),
(4, 'Owner', 'Sarah', 'Elizabeth', 'Wilson', '1982-05-12', '09123456792', '321 Elm Street, Cebu City', 'sarah.wilson@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456791', NULL, '09123456792', 'Approved'),
(5, 'Boarder', 'David', 'Robert', 'Brown', '1996-09-30', '09123456793', '654 Maple Avenue, Cebu City', 'david.brown@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456792', NULL, '09123456793', 'Approved'),
(6, 'Boarder', 'Lisa', 'Ann', 'Davis', '1997-12-18', '09123456794', '987 Cedar Lane, Cebu City', 'lisa.davis@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456793', NULL, '09123456794', 'Approved'),
(7, 'Owner', 'Tom', 'William', 'Miller', '1980-01-25', '09123456795', '147 Birch Road, Cebu City', 'tom.miller@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456792', NULL, '09123456795', 'Approved'),
(8, 'Boarder', 'Emma', 'Grace', 'Garcia', '1999-04-03', '09123456796', '258 Spruce Drive, Cebu City', 'emma.garcia@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456794', NULL, '09123456796', 'Approved'),
(65, 'Owner', 'John', 'Michael', 'Doe', '1985-03-15', '09123456789', '123 Main Street, Cebu City', 'mae.sam@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456789', NULL, '09123456789', 'Approved'),
(66, 'Boarder', 'Jane', 'Marie', 'Smith', '1995-07-22', '09123456790', '456 Oak Avenue, Cebu City', 'jane.smith@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456789', NULL, '09123456790', 'Approved'),
(67, 'Boarder', 'Mike', 'James', 'Johnson', '1998-11-08', '09123456791', '789 Pine Street, Cebu City', 'ru.john@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456790', NULL, '09123456791', 'Approved'),
(69, 'Boarder', 'David', 'Robert', 'Brown', '1996-09-30', '09123456793', '654 Maple Avenue, Cebu City', 'hash.mon@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456792', NULL, '09123456793', 'Approved'),
(70, 'Boarder', 'Lisa', 'Ann', 'Davis', '1997-12-18', '09123456794', '987 Cedar Lane, Cebu City', 'am.ko@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456793', NULL, '09123456794', 'Approved'),
(71, 'Owner', 'Tom', 'William', 'Miller', '1980-01-25', '09123456795', '147 Birch Road, Cebu City', 'ho.lo@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456792', NULL, '09123456795', 'Approved'),
(72, 'Boarder', 'Emma', 'Grace', 'Garcia', '1999-04-03', '09123456796', '258 Spruce Drive, Cebu City', 'wo.uy@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456794', NULL, '09123456796', 'Approved'),
(137, 'Owner', 'John', 'Michael', 'Doe', '1985-03-15', '09123456789', '123 Main Street, Cebu City', 'chris.cuas@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456789', NULL, '09123456789', 'Approved'),
(138, 'Boarder', 'Jane', 'Marie', 'Smith', '1995-07-22', '09123456790', '456 Oak Avenue, Cebu City', 'cam.phpr@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456789', NULL, '09123456790', 'Approved'),
(139, 'Boarder', 'Mike', 'James', 'Johnson', '1998-11-08', '09123456791', '789 Pine Street, Cebu City', 'ruel.john@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456790', NULL, '09123456791', 'Approved'),
(140, 'Owner', 'Sarah', 'Elizabeth', 'Wilson', '1982-05-12', '09123456792', '321 Elm Street, Cebu City', 'willy.lon@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456791', NULL, '09123456792', 'Approved'),
(142, 'Boarder', 'Lisa', 'Ann', 'Davis', '1997-12-18', '09123456794', '987 Cedar Lane, Cebu City', 'amber.ko@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456793', NULL, '09123456794', 'Approved'),
(143, 'Owner', 'Tom', 'William', 'Miller', '1980-01-25', '09123456795', '147 Birch Road, Cebu City', 'hole.lo@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Driver License', NULL, NULL, 'DL123456792', NULL, '09123456795', 'Approved'),
(144, 'Boarder', 'Emma', 'Grace', 'Garcia', '1999-04-03', '09123456796', '258 Spruce Drive, Cebu City', 'wolo.uy@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Student ID', NULL, NULL, 'ST123456794', NULL, '09123456796', 'Approved');

-- --------------------------------------------------------

--
-- Table structure for table `registrations`
--

CREATE TABLE `registrations` (
  `id` int(11) NOT NULL,
  `role` varchar(50) NOT NULL COMMENT 'Boarder or BH Owner',
  `first_name` varchar(100) NOT NULL,
  `middle_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) NOT NULL,
  `birth_date` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `gcash_num` varchar(20) DEFAULT NULL,
  `valid_id_type` varchar(100) DEFAULT NULL COMMENT 'Type of valid ID',
  `id_number` varchar(50) DEFAULT NULL COMMENT 'ID Number',
  `cb_agreed` tinyint(1) DEFAULT 0 COMMENT 'Terms and conditions agreed',
  `idFrontFile` varchar(255) DEFAULT NULL COMMENT 'Path to front ID image',
  `idBackFile` varchar(255) DEFAULT NULL COMMENT 'Path to back ID image',
  `gcash_qr` varchar(255) DEFAULT NULL COMMENT 'Path to GCash QR image',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `status` enum('unverified','pending','approved','rejected') DEFAULT 'unverified',
  `email_verified` tinyint(1) DEFAULT 0,
  `suffix` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `registrations`
--

INSERT INTO `registrations` (`id`, `role`, `first_name`, `middle_name`, `last_name`, `birth_date`, `phone`, `address`, `email`, `password`, `gcash_num`, `valid_id_type`, `id_number`, `cb_agreed`, `idFrontFile`, `idBackFile`, `gcash_qr`, `created_at`, `updated_at`, `status`, `email_verified`, `suffix`) VALUES
(1, 'Boarder', 'Test', NULL, 'User', NULL, NULL, NULL, 'test@example.com', 'test123', NULL, NULL, NULL, 0, NULL, NULL, NULL, '2025-10-05 22:08:09', '2025-10-26 05:25:18', 'approved', 1, NULL),
(2, 'Boarder', 'Test', NULL, 'User', NULL, NULL, NULL, 'test2@example.com', 'test123', NULL, NULL, NULL, 0, NULL, NULL, NULL, '2025-10-05 22:16:52', '2025-10-26 05:25:18', 'approved', 1, NULL),
(3, 'Boarder', 'Kimberly Jul', 'Binag', 'Mante', '2025-10-06', '09925311463', 'Lucob', 'kimjul@gmail.con', 'dhdjdkdk', '2134546', 'Driver\'s License', '123456789', 0, 'uploads/68e2eed214c01_front.jpg', 'uploads/68e2eed215235_back.jpg', 'uploads/68e2eed2153c1_qr.jpg', '2025-10-05 22:18:58', '2025-10-26 05:25:18', 'approved', 1, NULL),
(5, 'BH Owner', 'Christe Hanna', 'Dalugdog', 'Cuas', '2003-10-07', '09123456789', 'Tinibgan, Calape, Bohol', 'christehanna@gmail.com', 'namie', '09925311463', 'GSIS e-card', '123456789', 0, 'uploads/68e4f3b4e49ab_front.jpg', 'uploads/68e4f3b4e66be_back.jpg', 'uploads/68e4f3b4e86af_qr.jpg', '2025-10-07 11:04:20', '2025-10-26 05:25:18', 'approved', 1, NULL),
(8, 'Boarder', 'Flora', 'Oracion', 'Mante', '2004-09-07', '09925311463', 'Lucob, Calape, Bohol', 'floramante@gmail.com', 'flora', '123456789', 'SSS ID', '123456789', 0, 'uploads/68e4f92302869_front.jpg', 'uploads/68e4f92304024_back.jpg', 'uploads/68e4f92305704_qr.jpg', '2025-10-07 11:27:31', '2025-10-26 05:25:18', 'approved', 1, NULL),
(31, 'BH Owner', 'Hanna', 'Dalu', 'Baer', '0000-00-00', '09925311409', 'tini', 'hanna@gmail.com', '$2y$10$PGaMA3PAWMCB8zizQL9GNuML9moOOTo0W2FGHJ/MFeGUvhvn9DrnW', '09925311409', 'PhilID (National ID)', '12345678', 0, 'uploads/registrations/68e671d0356d0_front.jpg', 'uploads/registrations/68e671d035d67_back.jpg', 'uploads/registrations/68e671d037dbd_qr.jpg', '2025-10-08 14:14:40', '2025-10-26 05:25:18', 'approved', 1, NULL),
(35, 'BH Owner', 'Mari', 'Dalu', 'Baer', '0000-00-00', '09925311409', 'tini', 'mari@gmail.com', '$2y$10$00.1846IMH5PJixoF53O4u2B4lhsoG2gzqqVN0YraZayL/ywf4AB2', '09925311409', 'PhilID (National ID)', '12345678', 0, 'uploads/registrations/68e6722a65d31_front.jpg', 'uploads/registrations/68e6722a664ab_back.jpg', 'uploads/registrations/68e6722a68582_qr.jpg', '2025-10-08 14:16:10', '2025-10-26 05:25:18', 'approved', 1, NULL),
(42, 'Boarder', 'Mama', 'Mo', 'Ko', '2025-10-08', '9929769150', 'tinibgan', 'mama@gmail.com', '$2y$10$70UDp1ckqdUDq7imWw04u.XX8wYwOgbM3xT7OPaMDxuSwOOtmAfc6', '09353549141', 'PhilID (National ID)', '235689', 0, 'uploads/registrations/68e675f4de651_front.jpg', 'uploads/registrations/68e675f4dedde_back.jpg', 'uploads/registrations/68e675f4df3f8_qr.jpg', '2025-10-08 14:32:20', '2025-10-26 05:25:18', 'approved', 1, NULL),
(51, 'Boarder', 'Liz', '', 'Uy', '2025-10-09', '9929769150', 'calaoe', 'hannacuas536@gmail.com', '$2y$10$eM50WpC0TRIMpS28fpc7O.QnaScJXcf1vQejdDFRDmPYqdT3u8.Dm', '09925314096', 'PhilID (National ID)', '2356890', 0, 'uploads/registrations/68e709409683a_front.jpg', 'uploads/registrations/68e70940980cc_back.jpg', 'uploads/registrations/68e709409a367_qr.jpg', '2025-10-09 01:00:48', '2025-10-26 05:25:18', 'approved', 1, NULL),
(53, 'BH Owner', 'Namz', 'Dalu', 'Baer', '2025-10-09', '09925311409', 'calape', 'namzbaer@gmail.com', '$2y$10$D1L2DMM4L1LNrYYmuMS7huUlDifWQF3jU.7bfYXmqyIthGffluzD6', '09925311409', 'PhilID (National ID)', '2356890', 0, 'uploads/registrations/68e70b7a1a08c_front.jpg', 'uploads/registrations/68e70b7a1bcd8_back.jpg', 'uploads/gcash_qr/gcash_qr_29_1761528473.jpg', '2025-10-09 01:10:18', '2025-10-27 01:27:53', 'approved', 1, NULL),
(79, 'Boarder', 'Ruel', 'Dalugdog', 'Cuas', '2025-10-26', '09925311409', 'jskska', 'cuasruel028@gmail.com', '$2y$10$BsUh2CK1ipyyQ8kpE2p7Oei05ezywa3L9UQCrdPVTY5hyoK3smYqm', '09925311409', 'PhilID (National ID)', '123456789', 0, 'uploads/registrations/68fdb9b3ad6f2_front.jpg', 'uploads/registrations/68fdb9b3adcbe_back.jpg', 'uploads/registrations/68fdb9b3ae3e5_qr.jpg', '2025-10-26 06:03:31', '2025-10-26 06:04:50', 'approved', 1, NULL),
(84, 'BH Owner', 'Kimberly', 'Binag', 'Mante', '2025-10-27', '9925311409', 'lucob', 'kimjulmante@gmail.com', '$2y$10$nibA1zDk6rc1YA0qRGqWjOFZT158iHkTz0hYjcB6nimatAqqCBLEa', '09925311409', 'PhilID (National ID)', '123456789', 0, 'uploads/registrations/68feb9ecdee8d_front.jpg', 'uploads/registrations/68feb9ecdf784_back.jpg', 'uploads/registrations/68feb9ecdfe7e_qr.jpg', '2025-10-27 00:16:44', '2025-10-28 12:17:55', 'approved', 1, NULL),
(85, 'BH Owner', 'Shevic', 'Rulona', 'Tacatane', '2025-10-27', '09925311463', 'Bentig', 'mayettacatane@gmail.com', '$2y$10$gnziH/TxdrRG8EEcC15Nvu1/QFmI5eAgGekP3KUTzW63MXVA4.g/q', '09925311463', 'Driver\'s License', '123456789', 0, 'uploads/registrations/68fecdda9e8de_front.jpg', 'uploads/registrations/68fecdda9ec38_back.jpg', 'uploads/registrations/68fecdda9ee9f_qr.jpg', '2025-10-27 01:41:46', '2025-10-27 01:45:40', 'approved', 1, NULL),
(86, 'Boarder', 'John Mark', 'Marimon', 'Sagetarios', '2025-10-27', '9929769150', 'ubayon', 'johnmark.sagetarios@bisu.edu.ph', '$2y$10$as8INj1J.ZXQdZYnR.jvPu7vuzASFr0KMpfLlyE8OqUxPA2ewHYRm', '09925311409', 'PhilID (National ID)', '123456789', 0, 'uploads/registrations/68fecfb7dcae8_front.jpg', 'uploads/registrations/68fecfb7dce3f_back.jpg', 'uploads/registrations/68fecfb7dd119_qr.jpg', '2025-10-27 01:49:43', '2025-10-27 01:53:48', 'approved', 1, NULL),
(87, 'BH Owner', 'Mark', 'Marimon', 'Sagetarios', '2025-10-27', '9929769150', 'jajam', 'johnmarksagetarios114@gmail.com', '$2y$10$nzhujroKPQk59b3RPZA2puauGkllZmhIpMyYuYRwd2/gF7ZHFs6Me', '09925311409', 'PhilID (National ID)', '123456789', 0, 'uploads/registrations/68fed10aee7a3_front.jpg', 'uploads/registrations/68fed10aeec72_back.jpg', 'uploads/registrations/68fed10aef1aa_qr.jpg', '2025-10-27 01:55:22', '2025-10-27 01:58:47', 'pending', 1, NULL),
(89, 'BH Owner', 'Christe Hanna Mae', 'Dalugdog', 'Cuas', '2025-10-28', '9929769150', 'Tinibgan, Calape, Bohol', 'christehannamae.cuas@bisu.edu.ph', '$2y$10$WwiHOgMY3bBNjSc9s1wHdOsmxM026kr3HGRmaieym1nMeP8bBN5Ji', '09925311409', 'PhilID (National ID)', '2938-6034-9840-8726', 0, 'uploads/registrations/6900ad91f3f8c_front.jpg', 'uploads/registrations/6900ad92038d4_back.jpg', 'uploads/registrations/6900ad92072ff_qr.jpg', '2025-10-28 11:48:40', '2025-10-28 11:54:40', 'approved', 1, NULL),
(91, 'Boarder', 'Luca', 'Dakug', 'Cuas', '2025-10-29', '09925311409', 'Patag, Tinibgan, Calape, Bohol', 'lizacuas975@gmail.com', '$2y$10$AwR7zJ6Qm/gQCE6CXeKaAeMDnAZFeouswMISBAN2HpWvXJ0KW/f/W', '09925311409', 'Driver\'s License', 'G03-20-000299', 0, 'uploads/registrations/69017f9e51045_front.jpg', 'uploads/registrations/69017f9e53477_back.jpg', 'uploads/registrations/69017f9e55a44_qr.jpg', '2025-10-29 02:44:52', '2025-10-29 02:44:52', 'unverified', 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `reviews`
--

CREATE TABLE `reviews` (
  `review_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `bh_id` int(11) NOT NULL,
  `rating` int(11) NOT NULL CHECK (`rating` between 1 and 5),
  `comment` text DEFAULT NULL,
  `review_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `room_images`
--

CREATE TABLE `room_images` (
  `image_id` int(11) NOT NULL,
  `bhr_id` int(11) NOT NULL,
  `image_path` varchar(255) NOT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `room_images`
--

INSERT INTO `room_images` (`image_id`, `bhr_id`, `image_path`, `uploaded_at`) VALUES
(1, 10, 'uploads/room_images/bhr_10_68d262f445462.jpg', '2025-09-23 09:05:56'),
(2, 10, 'uploads/room_images/bhr_10_68d262fa15cca.jpg', '2025-09-23 09:06:02'),
(5, 12, 'uploads/room_images/bhr_12_68d264500d2e7.jpg', '2025-09-23 09:11:44'),
(6, 12, 'uploads/room_images/bhr_12_68d2645213f54.jpg', '2025-09-23 09:11:46'),
(7, 13, 'uploads/room_images/bhr_13_68d2663baa88a.jpg', '2025-09-23 09:19:55'),
(8, 13, 'uploads/room_images/bhr_13_68d26641199f1.jpg', '2025-09-23 09:20:01'),
(9, 14, 'uploads/room_images/bhr_14_68d267b01e555.jpg', '2025-09-23 09:26:08'),
(10, 14, 'uploads/room_images/bhr_14_68d267b584fc2.jpg', '2025-09-23 09:26:13'),
(11, 15, 'uploads/room_images/bhr_15_68d613d60c007.jpg', '2025-09-26 04:17:26'),
(12, 15, 'uploads/room_images/bhr_15_68d613d9984a3.jpg', '2025-09-26 04:17:29'),
(13, 16, 'uploads/room_images/bhr_16_68d7e2cf8821a.jpg', '2025-09-27 13:12:47'),
(14, 16, 'uploads/room_images/bhr_16_68d7e2d424728.jpg', '2025-09-27 13:12:52'),
(15, 17, 'uploads/room_images/bhr_17_68d7e6b19bf68.jpg', '2025-09-27 13:29:21'),
(16, 18, 'uploads/room_images/bhr_18_68d88c5857f0a.jpg', '2025-09-28 01:16:08'),
(17, 18, 'uploads/room_images/bhr_18_68d88c5a94ade.jpg', '2025-09-28 01:16:10'),
(18, 19, 'uploads/room_images/bhr_19_68d88d8c4c62d.jpg', '2025-09-28 01:21:16'),
(19, 20, 'uploads/room_images/bhr_20_68d8c0c487e68.jpg', '2025-09-28 04:59:48'),
(20, 21, 'uploads/room_images/bhr_21_68db38f23eced.jpg', '2025-09-30 01:57:06'),
(21, 24, 'uploads/room_images/bhr_24_68db4eebdb7b1.jpg', '2025-09-30 03:30:51'),
(22, 26, 'uploads/room_images/bhr_26_68db53067ef57.jpg', '2025-09-30 03:48:22'),
(23, 24, 'uploads/room_images/bhr_24_68db58a501697.jpg', '2025-09-30 04:12:21'),
(25, 25, 'uploads/room_images/bhr_25_68db58e79bcc0.jpg', '2025-09-30 04:13:27'),
(26, 28, 'uploads/room_images/bhr_28_68db5bb8a14a3.jpg', '2025-09-30 04:25:28'),
(27, 36, 'uploads/room_images/bhr_36_68db6395ce2b3.jpg', '2025-09-30 04:59:01'),
(28, 37, 'uploads/room_images/bhr_37_68db63dcb314b.jpg', '2025-09-30 05:00:12'),
(29, 38, 'uploads/room_images/bhr_38_68def900cbf5a.jpg', '2025-10-02 22:13:20'),
(30, 39, 'uploads/room_images/bhr_39_68def9665ec5e.jpg', '2025-10-02 22:15:02'),
(31, 40, 'uploads/room_images/bhr_40_68df1e48ad236.jpg', '2025-10-03 00:52:24'),
(32, 40, 'uploads/room_images/bhr_40_68df1e7dacc4c.jpg', '2025-10-03 00:53:17'),
(33, 41, 'uploads/room_images/bhr_41_68df1fb133f47.jpg', '2025-10-03 00:58:25'),
(34, 42, 'uploads/room_images/bhr_42_68df225230698.jpg', '2025-10-03 01:09:38'),
(35, 42, 'uploads/room_images/bhr_42_68df2255d4045.jpg', '2025-10-03 01:09:41'),
(36, 42, 'uploads/room_images/bhr_42_68df22590d022.jpg', '2025-10-03 01:09:45'),
(37, 24, 'uploads/room_images/bhr_24_68e0c3f4a1f17.jpg', '2025-10-04 06:51:33'),
(38, 43, 'uploads/room_images/bhr_43_68e1e2693b73e.jpg', '2025-10-05 03:13:45'),
(39, 43, 'uploads/room_images/bhr_43_68e1e348e5635.jpg', '2025-10-05 03:17:28'),
(40, 44, 'uploads/room_images/bhr_44_68e695f80e080.jpg', '2025-10-08 16:48:56'),
(41, 45, 'uploads/room_images/bhr_45_68e71e33d82fa.jpg', '2025-10-09 02:30:11'),
(42, 46, 'uploads/room_images/bhr_46_68eb253cb2a48.jpg', '2025-10-12 03:49:16'),
(43, 47, 'uploads/room_images/bhr_47_68eb268fd47c6.jpg', '2025-10-12 03:54:55'),
(44, 48, 'uploads/room_images/bhr_48_68fb212184fb8.jpg', '2025-10-24 06:48:01'),
(45, 48, 'uploads/room_images/bhr_48_68fb212431eec.jpg', '2025-10-24 06:48:04'),
(46, 49, 'uploads/room_images/bhr_49_690029cf04c1d.jpg', '2025-10-28 02:26:23');

-- --------------------------------------------------------

--
-- Table structure for table `room_units`
--

CREATE TABLE `room_units` (
  `room_id` int(11) NOT NULL,
  `bhr_id` int(11) NOT NULL,
  `room_number` varchar(50) NOT NULL,
  `status` enum('Available','Occupied','Unavailable') NOT NULL DEFAULT 'Available'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `room_units`
--

INSERT INTO `room_units` (`room_id`, `bhr_id`, `room_number`, `status`) VALUES
(1, 4, 'SR-1', 'Available'),
(2, 4, 'SR-2', 'Available'),
(3, 4, 'SR-3', 'Available'),
(4, 5, 'SR-1', 'Available'),
(5, 5, 'SR-2', 'Available'),
(6, 5, 'SR-3', 'Available'),
(7, 6, 'D-1', 'Available'),
(8, 6, 'D-2', 'Available'),
(9, 6, 'D-3', 'Available'),
(10, 6, 'D-4', 'Available'),
(11, 7, 'S-1', 'Available'),
(12, 7, 'S-2', 'Available'),
(13, 7, 'S-3', 'Available'),
(14, 7, 'S-4', 'Available'),
(15, 8, 'S-1', 'Available'),
(16, 8, 'S-2', 'Available'),
(17, 8, 'S-3', 'Available'),
(18, 8, 'S-4', 'Available'),
(19, 9, 'GA-1', 'Available'),
(20, 9, 'GA-2', 'Available'),
(21, 9, 'GA-3', 'Available'),
(22, 9, 'GA-4', 'Available'),
(23, 9, 'GA-5', 'Available'),
(24, 10, 'S-1', 'Available'),
(26, 12, 'D-1', 'Available'),
(27, 13, 'D-1', 'Available'),
(28, 14, 'GB-1', 'Available'),
(29, 15, 'FR-1', 'Available'),
(30, 15, 'FR-2', 'Available'),
(31, 16, 'S-1', 'Available'),
(32, 16, 'S-2', 'Available'),
(33, 17, 'SR-1', 'Available'),
(34, 18, 'F-1', 'Available'),
(35, 18, 'F-2', 'Available'),
(36, 19, 'F-1', 'Available'),
(37, 20, 'GC-1', 'Available'),
(38, 21, 'S-1', 'Available'),
(39, 22, 'S-1', 'Available'),
(40, 23, 'S-1', 'Available'),
(41, 24, 'S-1', 'Available'),
(42, 25, 'GB-1', 'Available'),
(43, 26, 'F-1', 'Available'),
(45, 28, 'SA-1', 'Available'),
(46, 29, 'S-1', 'Available'),
(47, 33, 'S-1', 'Available'),
(48, 34, 'S-1', 'Available'),
(50, 36, 'S-1', 'Available'),
(51, 37, 'S-1', 'Available'),
(52, 28, 'SA-2', 'Available'),
(53, 24, 'SA-2', 'Available'),
(54, 24, 'SA-3', 'Available'),
(59, 38, 'SR-1', 'Available'),
(60, 38, 'SR-2', 'Available'),
(61, 39, 'G-1', 'Available'),
(62, 39, 'G-2', 'Available'),
(63, 40, 'KHAR-1', 'Available'),
(64, 40, 'KHAR-2', 'Available'),
(65, 40, 'KHAR-3', 'Available'),
(66, 40, 'KHAR-4', 'Available'),
(67, 40, 'KHAR-5', 'Available'),
(68, 40, 'KHAR-6', 'Available'),
(69, 40, 'KHAR-7', 'Available'),
(70, 40, 'KHAR-8', 'Available'),
(71, 40, 'KHAR-9', 'Available'),
(72, 40, 'KHAR-10', 'Available'),
(73, 40, 'KHAR-11', 'Available'),
(74, 40, 'KHAR-12', 'Available'),
(75, 41, 'SA-1', 'Available'),
(76, 42, 'FR-1', 'Available'),
(77, 42, 'FR-2', 'Available'),
(78, 43, 'S-1', 'Available'),
(79, 44, 'SA-1', 'Available'),
(80, 45, 'SA-1', 'Available'),
(81, 46, 'SA-1', 'Available'),
(82, 47, 'GA-1', 'Occupied'),
(83, 48, 'R2-1', 'Available'),
(84, 47, 'GA-2', 'Available'),
(85, 49, 'PR0-1', 'Available');

-- --------------------------------------------------------

--
-- Table structure for table `support_tickets`
--

CREATE TABLE `support_tickets` (
  `ticket_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `st_subject` varchar(150) NOT NULL,
  `st_description` text NOT NULL,
  `st_status` enum('Pending','In Progress','Resolved','Closed') NOT NULL DEFAULT 'Pending',
  `st_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `reg_id` int(11) NOT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `status` enum('Active','Inactive') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `reg_id`, `profile_picture`, `status`) VALUES
(1, 2, 'uploads/profile_pictures/owner_1_68df20de76361.jpg', 'Active'),
(2, 1, 'profile_john.jpg', 'Active'),
(4, 3, 'profile_mike.jpg', 'Active'),
(5, 4, 'profile_sarah.jpg', 'Active'),
(6, 5, 'profile_david.jpg', 'Active'),
(7, 6, 'profile_lisa.jpg', 'Active'),
(8, 7, 'profile_tom.jpg', 'Active'),
(23, 42, NULL, 'Active'),
(24, 35, NULL, 'Active'),
(25, 10, NULL, 'Active'),
(27, 31, NULL, 'Active'),
(28, 51, NULL, 'Active'),
(29, 53, 'uploads/profile_pictures/user_29_68eb24c9b4c44.jpg', 'Active'),
(30, 74, NULL, 'Active'),
(31, 75, NULL, 'Active'),
(32, 76, NULL, 'Active'),
(33, 77, NULL, 'Active'),
(34, 78, NULL, 'Active'),
(35, 79, NULL, 'Active'),
(36, 84, NULL, 'Active'),
(37, 85, NULL, 'Active'),
(38, 86, NULL, 'Active'),
(39, 88, NULL, 'Active'),
(40, 89, NULL, 'Active');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `active_boarders`
--
ALTER TABLE `active_boarders`
  ADD PRIMARY KEY (`active_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `room_id` (`room_id`),
  ADD KEY `boarding_house_id` (`boarding_house_id`);

--
-- Indexes for table `admin_accounts`
--
ALTER TABLE `admin_accounts`
  ADD PRIMARY KEY (`admin_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `announcements`
--
ALTER TABLE `announcements`
  ADD PRIMARY KEY (`announcement_id`),
  ADD KEY `bh_id` (`bh_id`),
  ADD KEY `posted_by` (`posted_by`);

--
-- Indexes for table `bills`
--
ALTER TABLE `bills`
  ADD PRIMARY KEY (`bill_id`),
  ADD KEY `active_id` (`active_id`),
  ADD KEY `payment_id` (`payment_id`);

--
-- Indexes for table `boarding_houses`
--
ALTER TABLE `boarding_houses`
  ADD PRIMARY KEY (`bh_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `boarding_house_images`
--
ALTER TABLE `boarding_house_images`
  ADD PRIMARY KEY (`image_id`),
  ADD KEY `bh_id` (`bh_id`);

--
-- Indexes for table `boarding_house_rooms`
--
ALTER TABLE `boarding_house_rooms`
  ADD PRIMARY KEY (`bhr_id`),
  ADD KEY `bh_id` (`bh_id`);

--
-- Indexes for table `bookings`
--
ALTER TABLE `bookings`
  ADD PRIMARY KEY (`booking_id`),
  ADD KEY `room_id` (`room_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `chat_groups`
--
ALTER TABLE `chat_groups`
  ADD PRIMARY KEY (`gc_id`),
  ADD KEY `bh_id` (`bh_id`),
  ADD KEY `gc_created_by` (`gc_created_by`);

--
-- Indexes for table `device_tokens`
--
ALTER TABLE `device_tokens`
  ADD PRIMARY KEY (`token_id`),
  ADD UNIQUE KEY `unique_user_token` (`user_id`,`device_token`),
  ADD KEY `idx_user_active` (`user_id`,`is_active`),
  ADD KEY `idx_token` (`device_token`);

--
-- Indexes for table `email_verifications`
--
ALTER TABLE `email_verifications`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_verification` (`user_id`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_expiry` (`expiry_time`);

--
-- Indexes for table `group_members`
--
ALTER TABLE `group_members`
  ADD PRIMARY KEY (`gm_id`),
  ADD KEY `gc_id` (`gc_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `group_messages`
--
ALTER TABLE `group_messages`
  ADD PRIMARY KEY (`groupmessage_id`),
  ADD KEY `gc_id` (`gc_id`),
  ADD KEY `sender_id` (`sender_id`);

--
-- Indexes for table `maintenance_requests`
--
ALTER TABLE `maintenance_requests`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`message_id`),
  ADD KEY `sender_id` (`sender_id`),
  ADD KEY `receiver_id` (`receiver_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notif_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`payment_id`),
  ADD KEY `booking_id` (`booking_id`),
  ADD KEY `bill_id` (`bill_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `owner_id` (`owner_id`),
  ADD KEY `payment_status` (`payment_status`),
  ADD KEY `payment_date` (`payment_date`),
  ADD KEY `payment_month` (`payment_month`),
  ADD KEY `payment_year` (`payment_year`),
  ADD KEY `payment_month_number` (`payment_month_number`),
  ADD KEY `idx_payments_user_owner` (`user_id`,`owner_id`),
  ADD KEY `idx_payments_status_date` (`payment_status`,`payment_date`),
  ADD KEY `idx_payments_method` (`payment_method`),
  ADD KEY `idx_payments_monthly_tracking` (`user_id`,`payment_month`,`payment_status`),
  ADD KEY `idx_payments_owner_month` (`owner_id`,`payment_month`,`payment_status`);

--
-- Indexes for table `registration`
--
ALTER TABLE `registration`
  ADD PRIMARY KEY (`reg_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `registrations`
--
ALTER TABLE `registrations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_email` (`email`),
  ADD KEY `idx_role` (`role`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_created_at` (`created_at`),
  ADD KEY `idx_status_created` (`status`,`created_at`),
  ADD KEY `idx_suffix` (`suffix`);

--
-- Indexes for table `reviews`
--
ALTER TABLE `reviews`
  ADD PRIMARY KEY (`review_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `bh_id` (`bh_id`);

--
-- Indexes for table `room_images`
--
ALTER TABLE `room_images`
  ADD PRIMARY KEY (`image_id`),
  ADD KEY `bhr_id` (`bhr_id`);

--
-- Indexes for table `room_units`
--
ALTER TABLE `room_units`
  ADD PRIMARY KEY (`room_id`),
  ADD KEY `bhr_id` (`bhr_id`);

--
-- Indexes for table `support_tickets`
--
ALTER TABLE `support_tickets`
  ADD PRIMARY KEY (`ticket_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD KEY `reg_id` (`reg_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `active_boarders`
--
ALTER TABLE `active_boarders`
  MODIFY `active_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `admin_accounts`
--
ALTER TABLE `admin_accounts`
  MODIFY `admin_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `announcements`
--
ALTER TABLE `announcements`
  MODIFY `announcement_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `bills`
--
ALTER TABLE `bills`
  MODIFY `bill_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `boarding_houses`
--
ALTER TABLE `boarding_houses`
  MODIFY `bh_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=88;

--
-- AUTO_INCREMENT for table `boarding_house_images`
--
ALTER TABLE `boarding_house_images`
  MODIFY `image_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=83;

--
-- AUTO_INCREMENT for table `boarding_house_rooms`
--
ALTER TABLE `boarding_house_rooms`
  MODIFY `bhr_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=50;

--
-- AUTO_INCREMENT for table `bookings`
--
ALTER TABLE `bookings`
  MODIFY `booking_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `chat_groups`
--
ALTER TABLE `chat_groups`
  MODIFY `gc_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `device_tokens`
--
ALTER TABLE `device_tokens`
  MODIFY `token_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=29;

--
-- AUTO_INCREMENT for table `email_verifications`
--
ALTER TABLE `email_verifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

--
-- AUTO_INCREMENT for table `group_members`
--
ALTER TABLE `group_members`
  MODIFY `gm_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `group_messages`
--
ALTER TABLE `group_messages`
  MODIFY `groupmessage_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- AUTO_INCREMENT for table `maintenance_requests`
--
ALTER TABLE `maintenance_requests`
  MODIFY `request_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `message_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=238;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `notif_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=164;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `payment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `registration`
--
ALTER TABLE `registration`
  MODIFY `reg_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=146;

--
-- AUTO_INCREMENT for table `registrations`
--
ALTER TABLE `registrations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=92;

--
-- AUTO_INCREMENT for table `reviews`
--
ALTER TABLE `reviews`
  MODIFY `review_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `room_images`
--
ALTER TABLE `room_images`
  MODIFY `image_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;

--
-- AUTO_INCREMENT for table `room_units`
--
ALTER TABLE `room_units`
  MODIFY `room_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=86;

--
-- AUTO_INCREMENT for table `support_tickets`
--
ALTER TABLE `support_tickets`
  MODIFY `ticket_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=41;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `active_boarders`
--
ALTER TABLE `active_boarders`
  ADD CONSTRAINT `active_boarders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `active_boarders_ibfk_2` FOREIGN KEY (`room_id`) REFERENCES `room_units` (`room_id`),
  ADD CONSTRAINT `active_boarders_ibfk_3` FOREIGN KEY (`boarding_house_id`) REFERENCES `boarding_houses` (`bh_id`);

--
-- Constraints for table `announcements`
--
ALTER TABLE `announcements`
  ADD CONSTRAINT `announcements_ibfk_1` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `announcements_ibfk_2` FOREIGN KEY (`posted_by`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `bills`
--
ALTER TABLE `bills`
  ADD CONSTRAINT `bills_ibfk_1` FOREIGN KEY (`active_id`) REFERENCES `active_boarders` (`active_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `bills_ibfk_2` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`) ON DELETE SET NULL;

--
-- Constraints for table `boarding_houses`
--
ALTER TABLE `boarding_houses`
  ADD CONSTRAINT `boarding_houses_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `boarding_house_images`
--
ALTER TABLE `boarding_house_images`
  ADD CONSTRAINT `boarding_house_images_ibfk_1` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE;

--
-- Constraints for table `boarding_house_rooms`
--
ALTER TABLE `boarding_house_rooms`
  ADD CONSTRAINT `boarding_house_rooms_ibfk_1` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE;

--
-- Constraints for table `bookings`
--
ALTER TABLE `bookings`
  ADD CONSTRAINT `bookings_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `room_units` (`room_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `bookings_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `chat_groups`
--
ALTER TABLE `chat_groups`
  ADD CONSTRAINT `chat_groups_ibfk_1` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `chat_groups_ibfk_2` FOREIGN KEY (`gc_created_by`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `email_verifications`
--
ALTER TABLE `email_verifications`
  ADD CONSTRAINT `email_verifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `registrations` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `group_members`
--
ALTER TABLE `group_members`
  ADD CONSTRAINT `group_members_ibfk_1` FOREIGN KEY (`gc_id`) REFERENCES `chat_groups` (`gc_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `group_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `group_messages`
--
ALTER TABLE `group_messages`
  ADD CONSTRAINT `group_messages_ibfk_1` FOREIGN KEY (`gc_id`) REFERENCES `chat_groups` (`gc_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `group_messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `maintenance_requests`
--
ALTER TABLE `maintenance_requests`
  ADD CONSTRAINT `maintenance_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `payments_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `payments_ibfk_4` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `reviews`
--
ALTER TABLE `reviews`
  ADD CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `reviews_ibfk_2` FOREIGN KEY (`bh_id`) REFERENCES `boarding_houses` (`bh_id`) ON DELETE CASCADE;

--
-- Constraints for table `room_images`
--
ALTER TABLE `room_images`
  ADD CONSTRAINT `room_images_ibfk_1` FOREIGN KEY (`bhr_id`) REFERENCES `boarding_house_rooms` (`bhr_id`) ON DELETE CASCADE;

--
-- Constraints for table `room_units`
--
ALTER TABLE `room_units`
  ADD CONSTRAINT `room_units_ibfk_1` FOREIGN KEY (`bhr_id`) REFERENCES `boarding_house_rooms` (`bhr_id`) ON DELETE CASCADE;

--
-- Constraints for table `support_tickets`
--
ALTER TABLE `support_tickets`
  ADD CONSTRAINT `support_tickets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
