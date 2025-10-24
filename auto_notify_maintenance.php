<?php
// Auto notification for maintenance activities
require_once 'notification_helper.php';

class AutoNotifyMaintenance {
    
    public static function maintenanceScheduled($user_id, $maintenance_details) {
        $title = "Maintenance Scheduled";
        $message = "Maintenance for " . $maintenance_details['area'] . " is scheduled on " . $maintenance_details['scheduled_date'];
        
        return self::createNotification($user_id, $title, $message, 'maintenance', $maintenance_details);
    }
    
    public static function maintenanceCompleted($user_id, $maintenance_details) {
        $title = "Maintenance Completed";
        $message = "Maintenance for " . $maintenance_details['area'] . " has been completed.";
        
        return self::createNotification($user_id, $title, $message, 'maintenance', $maintenance_details);
    }
    
    public static function maintenanceCancelled($user_id, $maintenance_details) {
        $title = "Maintenance Cancelled";
        $message = "Maintenance for " . $maintenance_details['area'] . " scheduled on " . $maintenance_details['scheduled_date'] . " has been cancelled.";
        
        return self::createNotification($user_id, $title, $message, 'maintenance', $maintenance_details);
    }
    
    public static function maintenanceReminder($user_id, $maintenance_details) {
        $title = "Maintenance Reminder";
        $message = "Reminder: Maintenance for " . $maintenance_details['area'] . " is scheduled tomorrow.";
        
        return self::createNotification($user_id, $title, $message, 'maintenance', $maintenance_details);
    }
    
    private static function createNotification($user_id, $title, $message, $type, $details = []) {
        return NotificationHelper::createNotification($user_id, $title, $message, $type, true);
    }
}
?>
