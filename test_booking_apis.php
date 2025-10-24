<?php
// Test Booking APIs
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING BOOKING API ENDPOINTS ===\n\n";

// Test configuration
$base_url = "http://192.168.101.6/BoardEase2";
$test_user_id = 1; // Replace with actual user ID
$test_booking_id = 1; // Replace with actual booking ID

echo "🔧 API ENDPOINTS CREATED:\n\n";

echo "1. GET BOOKINGS:\n";
echo "   URL: {$base_url}/get_bookings.php?user_id={$test_user_id}&user_type=owner\n";
echo "   Purpose: Get all bookings for a user (owner or boarder)\n";
echo "   Parameters: user_id, user_type (owner/boarder)\n\n";

echo "2. GET APPROVED BOOKINGS:\n";
echo "   URL: {$base_url}/get_approved_bookings.php?user_id={$test_user_id}&user_type=owner\n";
echo "   Purpose: Get approved bookings only\n";
echo "   Parameters: user_id, user_type (owner/boarder)\n\n";

echo "3. GET PENDING BOOKINGS:\n";
echo "   URL: {$base_url}/get_pending_bookings.php?user_id={$test_user_id}&user_type=owner\n";
echo "   Purpose: Get pending bookings only\n";
echo "   Parameters: user_id, user_type (owner/boarder)\n\n";

echo "4. GET BOOKING HISTORY:\n";
echo "   URL: {$base_url}/get_booking_history.php?user_id={$test_user_id}&user_type=owner\n";
echo "   Purpose: Get completed/expired/cancelled bookings\n";
echo "   Parameters: user_id, user_type (owner/boarder)\n\n";

echo "5. GET BOOKING DETAILS:\n";
echo "   URL: {$base_url}/get_booking_details.php?booking_id={$test_booking_id}\n";
echo "   Purpose: Get detailed information for a specific booking\n";
echo "   Parameters: booking_id\n\n";

echo "6. APPROVE BOOKING:\n";
echo "   URL: {$base_url}/approve_booking.php\n";
echo "   Method: POST\n";
echo "   Purpose: Approve a pending booking\n";
echo "   Parameters: {\"booking_id\": {$test_booking_id}, \"owner_id\": {$test_user_id}}\n\n";

echo "7. DECLINE BOOKING:\n";
echo "   URL: {$base_url}/decline_booking.php\n";
echo "   Method: POST\n";
echo "   Purpose: Decline a pending booking\n";
echo "   Parameters: {\"booking_id\": {$test_booking_id}, \"owner_id\": {$test_user_id}, \"reason\": \"Room not available\"}\n\n";

echo "📊 RESPONSE FORMAT:\n\n";

echo "SUCCESS RESPONSE:\n";
echo "{\n";
echo "  \"success\": true,\n";
echo "  \"data\": {\n";
echo "    \"bookings\": [...],\n";
echo "    \"total_count\": 5\n";
echo "  }\n";
echo "}\n\n";

echo "ERROR RESPONSE:\n";
echo "{\n";
echo "  \"success\": false,\n";
echo "  \"error\": \"Error message\"\n";
echo "}\n\n";

echo "📱 BOOKING DATA STRUCTURE:\n";
echo "{\n";
echo "  \"booking_id\": 1,\n";
echo "  \"boarder_id\": 2,\n";
echo "  \"boarder_name\": \"John Doe\",\n";
echo "  \"boarder_email\": \"john@email.com\",\n";
echo "  \"boarder_phone\": \"09123456789\",\n";
echo "  \"room_name\": \"Room 101\",\n";
echo "  \"boarding_house_name\": \"Sunset Boarding House\",\n";
echo "  \"boarding_house_address\": \"123 Main St\",\n";
echo "  \"start_date\": \"2025-01-15\",\n";
echo "  \"end_date\": \"2025-04-15\",\n";
echo "  \"booking_date\": \"2025-01-10\",\n";
echo "  \"status\": \"Pending\",\n";
echo "  \"payment_status\": \"Pending\",\n";
echo "  \"notes\": \"Prefer ground floor\",\n";
echo "  \"profile_image\": \"profile.jpg\",\n";
echo "  \"room_id\": 1,\n";
echo "  \"boarding_house_id\": 1,\n";
echo "  \"rent_type\": \"Long-term\",\n";
echo "  \"amount\": \"P3,000.00\"\n";
echo "}\n\n";

echo "🔧 FEATURES INCLUDED:\n";
echo "✅ CORS headers for cross-origin requests\n";
echo "✅ JSON response format\n";
echo "✅ Error handling with try-catch\n";
echo "✅ Input validation\n";
echo "✅ Database transactions for approve/decline\n";
echo "✅ Automatic notifications via AutoNotifyBooking\n";
echo "✅ Room status updates\n";
echo "✅ Date formatting\n";
echo "✅ Amount formatting with currency\n";
echo "✅ Support for both owner and boarder views\n";
echo "✅ Detailed booking information\n";
echo "✅ Booking history filtering\n\n";

echo "🚀 INTEGRATION WITH ANDROID:\n";
echo "1. Replace sample data in booking fragments\n";
echo "2. Update API URLs in Android code\n";
echo "3. Handle JSON responses\n";
echo "4. Implement error handling\n";
echo "5. Add loading states\n";
echo "6. Update booking details activity\n\n";

echo "📋 DATABASE REQUIREMENTS:\n";
echo "The following tables are expected:\n";
echo "- bookings (booking_id, user_id, room_id, start_date, end_date, booking_date, booking_status, payment_status, notes)\n";
echo "- users (user_id, reg_id, profile_picture)\n";
echo "- registrations (reg_id, f_name, l_name, email, phone_number)\n";
echo "- room_units (room_id, room_number, bhr_id)\n";
echo "- boarding_house_rooms (bhr_id, bh_id, room_category, room_price, room_description, room_capacity, room_amenities)\n";
echo "- boarding_houses (bh_id, user_id, bh_name, bh_address, bh_contact)\n\n";

echo "🧪 TESTING INSTRUCTIONS:\n";
echo "1. Test each endpoint with valid user_id and booking_id\n";
echo "2. Test error cases (invalid IDs, missing parameters)\n";
echo "3. Test approve/decline functionality\n";
echo "4. Verify notifications are sent\n";
echo "5. Check database updates\n";
echo "6. Test with different user types (owner/boarder)\n\n";

echo "=== BOOKING API ENDPOINTS READY ===\n";
echo "All booking API endpoints have been created and are ready for integration! 🎉\n";
?>





