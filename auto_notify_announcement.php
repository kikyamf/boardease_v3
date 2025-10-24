<?php
// Auto notification for announcements
require_once 'notification_helper.php';

class AutoNotifyAnnouncement {
    
    public static function newAnnouncement($user_id, $announcement_details) {
        $title = "New Announcement";
        $message = $announcement_details['title'] . ": " . substr($announcement_details['content'], 0, 100) . "...";
        
        return self::createNotification($user_id, $title, $message, 'announcement', $announcement_details);
    }
    
    public static function urgentAnnouncement($user_id, $announcement_details) {
        $title = "🚨 URGENT: " . $announcement_details['title'];
        $message = $announcement_details['content'];
        
        return self::createNotification($user_id, $title, $message, 'announcement', $announcement_details);
    }
    
    public static function eventReminder($user_id, $event_details) {
        $title = "Event Reminder";
        $message = "Reminder: " . $event_details['event_name'] . " is happening on " . $event_details['event_date'];
        
        return self::createNotification($user_id, $title, $message, 'announcement', $event_details);
    }
    
    private static function createNotification($user_id, $title, $message, $type, $details = []) {
        return NotificationHelper::createNotification($user_id, $title, $message, $type, true);
    }
}
?>
