<?php
/**
 * Notification System Integration Examples
 * 
 * This file shows how to integrate automatic notifications
 * into your existing app activities (booking, payment, etc.)
 */

// Example 1: Booking System Integration
// When a new booking is created
function createBookingWithNotification($tenant_id, $owner_id, $room_id, $booking_details) {
    require_once 'db_helper.php';
    require_once 'auto_notify_booking.php';
    
    $db = getDB();
    
    // Insert booking into database
    $stmt = $db->prepare("
        INSERT INTO bookings (tenant_id, owner_id, room_id, booking_date, status) 
        VALUES (?, ?, ?, NOW(), 'pending')
    ");
    $stmt->execute([$tenant_id, $owner_id, $room_id]);
    $booking_id = $db->lastInsertId();
    
    // Send notification to owner
    $result = AutoNotifyBooking::newBooking($owner_id, [
        'tenant_name' => $booking_details['tenant_name'],
        'room_name' => $booking_details['room_name'],
        'booking_id' => $booking_id
    ]);
    
    return [
        'booking_id' => $booking_id,
        'notification_sent' => $result['success']
    ];
}

// Example 2: Payment System Integration
// When a payment is received
function processPaymentWithNotification($user_id, $payment_amount, $payment_type, $description) {
    require_once 'db_helper.php';
    require_once 'auto_notify_payment.php';
    
    $db = getDB();
    
    // Insert payment into database
    $stmt = $db->prepare("
        INSERT INTO payments (user_id, amount, payment_type, description, payment_date, status) 
        VALUES (?, ?, ?, ?, NOW(), 'completed')
    ");
    $stmt->execute([$user_id, $payment_amount, $payment_type, $description]);
    $payment_id = $db->lastInsertId();
    
    // Send notification to user
    $result = AutoNotifyPayment::paymentReceived($user_id, [
        'amount' => $payment_amount,
        'description' => $description,
        'payment_id' => $payment_id
    ]);
    
    return [
        'payment_id' => $payment_id,
        'notification_sent' => $result['success']
    ];
}

// Example 3: Maintenance System Integration
// When maintenance is scheduled
function scheduleMaintenanceWithNotification($affected_users, $maintenance_details) {
    require_once 'db_helper.php';
    require_once 'auto_notify_maintenance.php';
    
    $db = getDB();
    
    // Insert maintenance record
    $stmt = $db->prepare("
        INSERT INTO maintenance (area, description, scheduled_date, status) 
        VALUES (?, ?, ?, 'scheduled')
    ");
    $stmt->execute([
        $maintenance_details['area'],
        $maintenance_details['description'],
        $maintenance_details['scheduled_date']
    ]);
    $maintenance_id = $db->lastInsertId();
    
    // Send notifications to all affected users
    $results = [];
    foreach ($affected_users as $user_id) {
        $result = AutoNotifyMaintenance::maintenanceScheduled($user_id, [
            'area' => $maintenance_details['area'],
            'scheduled_date' => $maintenance_details['scheduled_date'],
            'maintenance_id' => $maintenance_id
        ]);
        $results[] = $result;
    }
    
    return [
        'maintenance_id' => $maintenance_id,
        'notifications_sent' => $results
    ];
}

// Example 4: Announcement System Integration
// When an announcement is created
function createAnnouncementWithNotification($target_users, $announcement_details) {
    require_once 'db_helper.php';
    require_once 'auto_notify_announcement.php';
    
    $db = getDB();
    
    // Insert announcement
    $stmt = $db->prepare("
        INSERT INTO announcements (title, content, created_by, created_at, is_urgent) 
        VALUES (?, ?, ?, NOW(), ?)
    ");
    $stmt->execute([
        $announcement_details['title'],
        $announcement_details['content'],
        $announcement_details['created_by'],
        $announcement_details['is_urgent']
    ]);
    $announcement_id = $db->lastInsertId();
    
    // Send notifications to all target users
    $results = [];
    foreach ($target_users as $user_id) {
        $result = AutoNotifyAnnouncement::newAnnouncement($user_id, [
            'title' => $announcement_details['title'],
            'content' => $announcement_details['content'],
            'announcement_id' => $announcement_id
        ]);
        $results[] = $result;
    }
    
    return [
        'announcement_id' => $announcement_id,
        'notifications_sent' => $results
    ];
}

// Example 5: Booking Status Update Integration
// When booking status changes
function updateBookingStatusWithNotification($booking_id, $new_status, $user_id) {
    require_once 'db_helper.php';
    require_once 'auto_notify_booking.php';
    
    $db = getDB();
    
    // Update booking status
    $stmt = $db->prepare("
        UPDATE bookings 
        SET status = ?, updated_at = NOW() 
        WHERE booking_id = ?
    ");
    $stmt->execute([$new_status, $booking_id]);
    
    // Get booking details
    $stmt = $db->prepare("
        SELECT b.*, r.room_name, u.first_name, u.last_name 
        FROM bookings b 
        JOIN rooms r ON b.room_id = r.room_id 
        JOIN users u ON b.tenant_id = u.user_id 
        WHERE b.booking_id = ?
    ");
    $stmt->execute([$booking_id]);
    $booking = $stmt->fetch();
    
    // Send appropriate notification
    $result = null;
    switch ($new_status) {
        case 'approved':
            $result = AutoNotifyBooking::bookingApproved($booking['tenant_id'], [
                'room_name' => $booking['room_name']
            ]);
            break;
        case 'rejected':
            $result = AutoNotifyBooking::bookingRejected($booking['tenant_id'], [
                'room_name' => $booking['room_name']
            ]);
            break;
        case 'cancelled':
            $result = AutoNotifyBooking::bookingCancelled($booking['tenant_id'], [
                'room_name' => $booking['room_name']
            ]);
            break;
    }
    
    return [
        'booking_updated' => true,
        'notification_sent' => $result ? $result['success'] : false
    ];
}

// Example 6: Payment Reminder System
// Send payment reminders
function sendPaymentReminders() {
    require_once 'db_helper.php';
    require_once 'auto_notify_payment.php';
    
    $db = getDB();
    
    // Get users with overdue payments
    $stmt = $db->prepare("
        SELECT u.user_id, p.amount, p.description, p.due_date 
        FROM payments p 
        JOIN users u ON p.user_id = u.user_id 
        WHERE p.status = 'pending' 
        AND p.due_date < NOW() 
        AND p.reminder_sent = 0
    ");
    $stmt->execute();
    $overdue_payments = $stmt->fetchAll();
    
    $results = [];
    foreach ($overdue_payments as $payment) {
        // Send overdue notification
        $result = AutoNotifyPayment::paymentOverdue($payment['user_id'], [
            'amount' => $payment['amount'],
            'description' => $payment['description']
        ]);
        
        // Mark reminder as sent
        $update_stmt = $db->prepare("
            UPDATE payments 
            SET reminder_sent = 1 
            WHERE user_id = ? AND amount = ? AND description = ?
        ");
        $update_stmt->execute([
            $payment['user_id'],
            $payment['amount'],
            $payment['description']
        ]);
        
        $results[] = $result;
    }
    
    return [
        'reminders_sent' => count($results),
        'results' => $results
    ];
}

// Example 7: Maintenance Reminder System
// Send maintenance reminders
function sendMaintenanceReminders() {
    require_once 'db_helper.php';
    require_once 'auto_notify_maintenance.php';
    
    $db = getDB();
    
    // Get upcoming maintenance (next 24 hours)
    $stmt = $db->prepare("
        SELECT m.*, GROUP_CONCAT(gm.user_id) as affected_users 
        FROM maintenance m 
        LEFT JOIN group_members gm ON m.affected_group = gm.gc_id 
        WHERE m.scheduled_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR) 
        AND m.status = 'scheduled' 
        AND m.reminder_sent = 0
        GROUP BY m.maintenance_id
    ");
    $stmt->execute();
    $upcoming_maintenance = $stmt->fetchAll();
    
    $results = [];
    foreach ($upcoming_maintenance as $maintenance) {
        $affected_users = explode(',', $maintenance['affected_users']);
        
        foreach ($affected_users as $user_id) {
            if ($user_id) {
                $result = AutoNotifyMaintenance::maintenanceReminder($user_id, [
                    'area' => $maintenance['area'],
                    'scheduled_date' => $maintenance['scheduled_date']
                ]);
                $results[] = $result;
            }
        }
        
        // Mark reminder as sent
        $update_stmt = $db->prepare("
            UPDATE maintenance 
            SET reminder_sent = 1 
            WHERE maintenance_id = ?
        ");
        $update_stmt->execute([$maintenance['maintenance_id']]);
    }
    
    return [
        'reminders_sent' => count($results),
        'results' => $results
    ];
}

// Example 8: Event Reminder System
// Send event reminders
function sendEventReminders() {
    require_once 'db_helper.php';
    require_once 'auto_notify_announcement.php';
    
    $db = getDB();
    
    // Get upcoming events (next 24 hours)
    $stmt = $db->prepare("
        SELECT e.*, GROUP_CONCAT(gm.user_id) as attendees 
        FROM events e 
        LEFT JOIN event_attendees ea ON e.event_id = ea.event_id 
        LEFT JOIN group_members gm ON ea.group_id = gm.gc_id 
        WHERE e.event_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR) 
        AND e.reminder_sent = 0
        GROUP BY e.event_id
    ");
    $stmt->execute();
    $upcoming_events = $stmt->fetchAll();
    
    $results = [];
    foreach ($upcoming_events as $event) {
        $attendees = explode(',', $event['attendees']);
        
        foreach ($attendees as $user_id) {
            if ($user_id) {
                $result = AutoNotifyAnnouncement::eventReminder($user_id, [
                    'event_name' => $event['event_name'],
                    'event_date' => $event['event_date']
                ]);
                $results[] = $result;
            }
        }
        
        // Mark reminder as sent
        $update_stmt = $db->prepare("
            UPDATE events 
            SET reminder_sent = 1 
            WHERE event_id = ?
        ");
        $update_stmt->execute([$event['event_id']]);
    }
    
    return [
        'reminders_sent' => count($results),
        'results' => $results
    ];
}

// Example 9: Bulk Notification System
// Send notifications to multiple users
function sendBulkNotification($user_ids, $title, $message, $type = 'general') {
    require_once 'create_notification.php';
    
    $results = [];
    foreach ($user_ids as $user_id) {
        // Simulate POST data for create_notification.php
        $_POST = [
            'user_id' => $user_id,
            'notif_title' => $title,
            'notif_message' => $message,
            'notif_type' => $type,
            'send_fcm' => true
        ];
        
        // Capture output
        ob_start();
        include 'create_notification.php';
        $output = ob_get_clean();
        
        $result = json_decode($output, true);
        $results[] = $result;
    }
    
    return [
        'total_sent' => count($user_ids),
        'results' => $results
    ];
}

// Example 10: Scheduled Notification System
// Create a cron job or scheduled task to run these functions
function runScheduledNotifications() {
    $results = [];
    
    // Send payment reminders
    $payment_results = sendPaymentReminders();
    $results['payment_reminders'] = $payment_results;
    
    // Send maintenance reminders
    $maintenance_results = sendMaintenanceReminders();
    $results['maintenance_reminders'] = $maintenance_results;
    
    // Send event reminders
    $event_results = sendEventReminders();
    $results['event_reminders'] = $event_results;
    
    return $results;
}

// Example usage in your existing PHP files:
/*
// In your booking creation file:
$booking_result = createBookingWithNotification(
    $tenant_id, 
    $owner_id, 
    $room_id, 
    ['tenant_name' => 'John Doe', 'room_name' => 'Room 101']
);

// In your payment processing file:
$payment_result = processPaymentWithNotification(
    $user_id, 
    5000.00, 
    'rent', 
    'Monthly Rent - January 2025'
);

// In your maintenance scheduling file:
$maintenance_result = scheduleMaintenanceWithNotification(
    [1, 2, 3, 4, 5], // User IDs
    [
        'area' => 'Common Area',
        'description' => 'Water system maintenance',
        'scheduled_date' => '2025-01-15 09:00:00'
    ]
);

// In your announcement creation file:
$announcement_result = createAnnouncementWithNotification(
    [1, 2, 3, 4, 5], // User IDs
    [
        'title' => 'Water Supply Maintenance',
        'content' => 'There will be water supply maintenance tomorrow...',
        'created_by' => 1,
        'is_urgent' => true
    ]
);
*/
?>
