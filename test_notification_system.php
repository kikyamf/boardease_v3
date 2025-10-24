<?php
// Test notification system
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'notification_helper.php';
require_once 'auto_notify_booking.php';
require_once 'auto_notify_payment.php';
require_once 'auto_notify_maintenance.php';
require_once 'auto_notify_announcement.php';
header('Content-Type: application/json');

try {
    $action = $_GET['action'] ?? 'test_all';
    $user_id = $_GET['user_id'] ?? 1; // Default to user ID 1 for testing
    
    $results = [];
    
    switch ($action) {
        case 'test_booking':
            $results[] = AutoNotifyBooking::newBooking($user_id, [
                'tenant_name' => 'John Doe',
                'room_name' => 'Room 101'
            ]);
            break;
            
        case 'test_payment':
            $results[] = AutoNotifyPayment::paymentReceived($user_id, [
                'amount' => 5000.00,
                'description' => 'Monthly Rent - January 2025'
            ]);
            break;
            
        case 'test_maintenance':
            $results[] = AutoNotifyMaintenance::maintenanceScheduled($user_id, [
                'area' => 'Common Area',
                'scheduled_date' => '2025-01-15 09:00 AM'
            ]);
            break;
            
        case 'test_announcement':
            $results[] = AutoNotifyAnnouncement::newAnnouncement($user_id, [
                'title' => 'Water Supply Maintenance',
                'content' => 'There will be water supply maintenance on January 20, 2025 from 8:00 AM to 12:00 PM. Please store water in advance.'
            ]);
            break;
            
        case 'test_all':
        default:
            // Test all notification types
            $results[] = AutoNotifyBooking::newBooking($user_id, [
                'tenant_name' => 'Jane Smith',
                'room_name' => 'Room 205'
            ]);
            
            $results[] = AutoNotifyPayment::paymentOverdue($user_id, [
                'amount' => 3000.00,
                'description' => 'Monthly Rent - December 2024'
            ]);
            
            $results[] = AutoNotifyMaintenance::maintenanceCompleted($user_id, [
                'area' => 'Elevator'
            ]);
            
            $results[] = AutoNotifyAnnouncement::urgentAnnouncement($user_id, [
                'title' => 'Fire Drill',
                'content' => 'Fire drill will be conducted tomorrow at 2:00 PM. Please evacuate the building when the alarm sounds.'
            ]);
            break;
    }
    
    $response = [
        'success' => true,
        'message' => 'Notification test completed',
        'data' => [
            'action' => $action,
            'user_id' => $user_id,
            'results' => $results
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>
