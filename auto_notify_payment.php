<?php
// Auto notification for payment activities
require_once 'notification_helper.php';

class AutoNotifyPayment {
    
    public static function paymentReceived($user_id, $payment_details) {
        $title = "Payment Received";
        $message = "Payment of ₱" . number_format($payment_details['amount'], 2) . " has been received for " . $payment_details['description'];
        
        return self::createNotification($user_id, $title, $message, 'payment', $payment_details);
    }
    
    public static function paymentOverdue($user_id, $payment_details) {
        $title = "Payment Overdue";
        $message = "Your payment of ₱" . number_format($payment_details['amount'], 2) . " for " . $payment_details['description'] . " is overdue.";
        
        return self::createNotification($user_id, $title, $message, 'payment', $payment_details);
    }
    
    public static function paymentReminder($user_id, $payment_details) {
        $title = "Payment Reminder";
        $message = "Reminder: Payment of ₱" . number_format($payment_details['amount'], 2) . " for " . $payment_details['description'] . " is due soon.";
        
        return self::createNotification($user_id, $title, $message, 'payment', $payment_details);
    }
    
    public static function paymentFailed($user_id, $payment_details) {
        $title = "Payment Failed";
        $message = "Your payment of ₱" . number_format($payment_details['amount'], 2) . " for " . $payment_details['description'] . " has failed. Please try again.";
        
        return self::createNotification($user_id, $title, $message, 'payment', $payment_details);
    }
    
    private static function createNotification($user_id, $title, $message, $type, $details = []) {
        return NotificationHelper::createNotification($user_id, $title, $message, $type, true);
    }
}
?>
