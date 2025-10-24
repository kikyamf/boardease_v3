# 🔔 BoardEase Notification System

A comprehensive notification system for the BoardEase boarding house management app that provides real-time notifications for various activities including bookings, payments, maintenance, and announcements.

## 📋 Features

- ✅ **Real-time FCM Notifications** - Push notifications to Android devices
- ✅ **Multiple Notification Types** - Booking, Payment, Maintenance, Announcement, General
- ✅ **Read/Unread Status** - Track notification status
- ✅ **Automatic Notifications** - Auto-generate notifications for app activities
- ✅ **Bulk Notifications** - Send to multiple users
- ✅ **Scheduled Reminders** - Automated reminder system
- ✅ **Android Integration** - Native Android notification activity
- ✅ **Web Testing Interface** - HTML test page for debugging

## 🗂️ File Structure

### PHP Backend Files
```
Main Directory/
├── create_notification.php          # Create new notifications
├── get_notifications.php            # Get user notifications
├── mark_notification_read.php       # Mark single notification as read
├── mark_all_notifications_read.php  # Mark all notifications as read
├── delete_notification.php          # Delete notification
├── get_unread_notif_count.php       # Get unread notification count
├── auto_notify_booking.php          # Auto notifications for booking activities
├── auto_notify_payment.php          # Auto notifications for payment activities
├── auto_notify_maintenance.php      # Auto notifications for maintenance activities
├── auto_notify_announcement.php     # Auto notifications for announcements
├── test_notification_system.php     # Test all notification types
├── integration_examples.php         # Integration examples for existing code

notification/
├── test_notification_system.html    # Web interface for testing
└── README.md                        # This documentation
```

### Android Files
```
app/src/main/java/com/example/mock/
├── Notification.java                # Main notification activity
├── NotificationItemModel.java       # Notification data model
├── NotificationsAdapter.java        # RecyclerView adapter
└── NotificationHelper.java          # Helper class for notifications

app/src/main/res/layout/
├── activity_notification.xml        # Main notification activity layout
├── item_notif_header.xml            # Date header layout
└── item_notif_card.xml              # Notification card layout
```

## 🗄️ Database Schema

```sql
CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `notif_title` varchar(150) NOT NULL,
  `notif_message` text NOT NULL,
  `notif_type` enum('booking','payment','announcement','maintenance','general') DEFAULT 'general',
  `notif_status` enum('unread','read') DEFAULT 'unread',
  `notif_created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## 🚀 Quick Start

### 1. Setup Database
Run the SQL schema above to create the notifications table.

### 2. Test the System
Open `notification/test_notification_system.html` in your browser to test the notification system.

### 3. Android Integration
The Android notification activity is ready to use. It will automatically:
- Load notifications from the server
- Display them with proper formatting
- Handle read/unread status
- Support swipe-to-refresh
- Show empty state when no notifications

### 4. PHP Integration
Use the helper classes in your existing PHP files:

```php
// For booking notifications
require_once 'auto_notify_booking.php';
AutoNotifyBooking::newBooking($user_id, $booking_details);

// For payment notifications
require_once 'auto_notify_payment.php';
AutoNotifyPayment::paymentReceived($user_id, $payment_details);
```

## 📱 Android Usage

### Basic Usage
```java
// In your activity
NotificationHelper helper = new NotificationHelper(this);

// Create a notification
helper.notifyBookingActivity(userId, "new_booking", "New booking request for Room 101");

// Get unread count
helper.getUnreadCount(userId, new NotificationHelper.UnreadCountCallback() {
    @Override
    public void onSuccess(int unreadCount) {
        // Update badge or UI
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

### Notification Activity
The `Notification.java` activity provides:
- List of all notifications
- Date-based grouping (Today, Yesterday, etc.)
- Read/unread status indicators
- Click handling for different notification types
- Swipe-to-refresh functionality

## 🔧 API Endpoints

### Create Notification
```
POST /create_notification.php
Parameters:
- user_id (int): Target user ID
- notif_title (string): Notification title
- notif_message (string): Notification message
- notif_type (string): Type (booking, payment, maintenance, announcement, general)
- send_fcm (bool): Whether to send FCM notification
```

### Get Notifications
```
GET /get_notifications.php?user_id={id}&limit={limit}&offset={offset}
```

### Mark as Read
```
POST /mark_notification_read.php
Parameters:
- notif_id (int): Notification ID
- user_id (int): User ID
```

### Get Unread Count
```
GET /get_unread_notif_count.php?user_id={id}
```

## 🎯 Notification Types

### Booking Notifications
- `new_booking` - New booking request
- `booking_approved` - Booking approved
- `booking_rejected` - Booking rejected
- `booking_cancelled` - Booking cancelled

### Payment Notifications
- `payment_received` - Payment received
- `payment_overdue` - Payment overdue
- `payment_reminder` - Payment reminder
- `payment_failed` - Payment failed

### Maintenance Notifications
- `maintenance_scheduled` - Maintenance scheduled
- `maintenance_completed` - Maintenance completed
- `maintenance_cancelled` - Maintenance cancelled
- `maintenance_reminder` - Maintenance reminder

### Announcement Notifications
- `new_announcement` - New announcement
- `urgent_announcement` - Urgent announcement
- `event_reminder` - Event reminder

## 🔄 Automatic Integration

### Booking System
When a booking is created, approved, or rejected, notifications are automatically sent to the relevant users.

### Payment System
When payments are received, overdue, or fail, notifications are automatically sent.

### Maintenance System
When maintenance is scheduled, completed, or cancelled, notifications are sent to affected users.

### Announcement System
When announcements are created, notifications are sent to target users.

## 📊 Testing

### Web Interface
1. Open `notification/test_notification_system.html` in your browser
2. Test creating different types of notifications
3. Check your Android device for FCM notifications
4. Test marking notifications as read

### PHP Testing
```php
// Test all notification types
$result = include 'test_notification_system.php?action=test_all&user_id=1';
```

### Android Testing
1. Open the Notification activity in your app
2. Pull down to refresh
3. Tap notifications to mark as read
4. Check different notification types

## 🔧 Configuration

### FCM Setup
Make sure your `fcm_config.php` is properly configured with:
- Firebase project ID
- Service account key file
- FCM server key

### Database Connection
Ensure your `db_helper.php` is properly configured with database credentials.

### Android Permissions
Make sure your Android app has:
- Internet permission
- FCM service configured
- Notification permissions

## 🐛 Troubleshooting

### Common Issues

1. **FCM Notifications Not Received**
   - Check FCM configuration
   - Verify device token is registered
   - Check Firebase console for errors

2. **Database Connection Issues**
   - Verify database credentials
   - Check table exists
   - Test database connection

3. **Android App Issues**
   - Check network permissions
   - Verify API endpoints
   - Check user ID in shared preferences

### Debug Mode
Enable debug mode by setting `error_reporting(1)` in PHP files to see detailed error messages.

## 📈 Future Enhancements

- [ ] Notification categories and filtering
- [ ] Push notification scheduling
- [ ] Notification templates
- [ ] Analytics and reporting
- [ ] Multi-language support
- [ ] Notification preferences
- [ ] Rich notifications with images
- [ ] Action buttons in notifications

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This notification system is part of the BoardEase project and follows the same licensing terms.

---

**Need Help?** Check the integration examples in `integration_examples.php` or test the system using `notification/test_notification_system.html`.








