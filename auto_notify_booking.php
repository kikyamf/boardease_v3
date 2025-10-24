<?php
// Auto notification for booking activities
require_once 'notification_helper.php';

class AutoNotifyBooking {
    
    public static function newBooking($user_id, $booking_details) {
        $title = "New Booking Request";
        $message = "You have a new booking request from " . $booking_details['tenant_name'] . " for " . $booking_details['room_name'];
        
        return self::createNotification($user_id, $title, $message, 'booking', $booking_details);
    }
    
    public static function bookingApproved($user_id, $booking_details) {
        $title = "Booking Approved";
        $message = "Your booking request for " . $booking_details['room_name'] . " has been approved!";
        
        return self::createNotification($user_id, $title, $message, 'booking', $booking_details);
    }
    
    public static function bookingRejected($user_id, $booking_details) {
        $title = "Booking Rejected";
        $message = "Your booking request for " . $booking_details['room_name'] . " has been rejected.";
        
        return self::createNotification($user_id, $title, $message, 'booking', $booking_details);
    }
    
    public static function bookingCancelled($user_id, $booking_details) {
        $title = "Booking Cancelled";
        $message = "Booking for " . $booking_details['room_name'] . " has been cancelled.";
        
        return self::createNotification($user_id, $title, $message, 'booking', $booking_details);
    }
    
    private static function createNotification($user_id, $title, $message, $type, $details = []) {
        return NotificationHelper::createNotification($user_id, $title, $message, $type, true);
    }
}
?>
