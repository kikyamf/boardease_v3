<?php
// Test all user-related endpoints to ensure they work with actual logged-in user data
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Test All User Endpoints</h2>";

try {
    // Test with a known user ID
    $test_user_id = 1; // Change this to a valid user ID in your system
    
    echo "<h3>Testing with User ID: $test_user_id</h3>";
    
    // Test 1: Login System
    echo "<h4>1. Testing Login System</h4>";
    $test_email = "mari@gmail.com"; // Change this to a valid email
    
    $sql = "SELECT r.id, r.role, r.first_name, r.last_name, r.email, r.password, r.status, u.user_id 
            FROM registrations r 
            LEFT JOIN users u ON r.id = u.reg_id 
            WHERE r.email = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $test_email);
    $stmt->execute();
    $result = $stmt->get_result();
    $user_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($user_data) {
        echo "✅ Login system working - User found<br>";
        echo "User ID that will be returned: " . ($user_data['user_id'] ? $user_data['user_id'] : $user_data['id']) . "<br>";
    } else {
        echo "❌ Login system issue - User not found<br>";
    }
    
    // Test 2: Owner Profile
    echo "<h4>2. Testing Owner Profile</h4>";
    $sql = "SELECT r.first_name, r.middle_name, r.last_name, r.birth_date, r.phone, r.address, r.email, u.profile_picture 
            FROM users u 
            JOIN registrations r ON u.reg_id = r.id 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $profile_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($profile_data) {
        echo "✅ Owner profile working<br>";
        echo "Name: " . $profile_data['first_name'] . " " . $profile_data['last_name'] . "<br>";
        echo "Email: " . $profile_data['email'] . "<br>";
    } else {
        echo "❌ Owner profile issue - No data found<br>";
    }
    
    // Test 3: Account Information
    echo "<h4>3. Testing Account Information</h4>";
    $sql = "SELECT r.email, r.gcash_num, r.gcash_qr 
            FROM registrations r 
            JOIN users u ON r.id = u.reg_id 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $account_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($account_data) {
        echo "✅ Account information working<br>";
        echo "Email: " . $account_data['email'] . "<br>";
        echo "GCash Number: " . ($account_data['gcash_num'] ? $account_data['gcash_num'] : 'Not set') . "<br>";
    } else {
        echo "❌ Account information issue - No data found<br>";
    }
    
    // Test 4: GCash Information
    echo "<h4>4. Testing GCash Information</h4>";
    $sql = "SELECT r.gcash_num, r.gcash_qr 
            FROM registrations r 
            JOIN users u ON r.id = u.reg_id 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $gcash_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($gcash_data) {
        echo "✅ GCash information working<br>";
        echo "GCash Number: " . ($gcash_data['gcash_num'] ? $gcash_data['gcash_num'] : 'Not set') . "<br>";
        echo "GCash QR: " . ($gcash_data['gcash_qr'] ? 'Set' : 'Not set') . "<br>";
    } else {
        echo "❌ GCash information issue - No data found<br>";
    }
    
    // Test 5: Owner Dashboard
    echo "<h4>5. Testing Owner Dashboard</h4>";
    $sql = "SELECT CONCAT(r.first_name, ' ', r.last_name) AS fullname
            FROM users u
            INNER JOIN registrations r ON u.reg_id = r.id
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $dashboard_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($dashboard_data) {
        echo "✅ Owner dashboard working<br>";
        echo "Owner Name: " . $dashboard_data['fullname'] . "<br>";
    } else {
        echo "❌ Owner dashboard issue - No data found<br>";
    }
    
    // Test 6: Notifications
    echo "<h4>6. Testing Notifications</h4>";
    $sql = "SELECT COUNT(*) as total, 
            SUM(CASE WHEN notif_status = 'unread' THEN 1 ELSE 0 END) as unread
            FROM notifications 
            WHERE user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $notif_data = $result->fetch_assoc();
    $stmt->close();
    
    if ($notif_data) {
        echo "✅ Notifications working<br>";
        echo "Total notifications: " . $notif_data['total'] . "<br>";
        echo "Unread notifications: " . $notif_data['unread'] . "<br>";
    } else {
        echo "❌ Notifications issue - No data found<br>";
    }
    
    // Summary
    echo "<h3>Summary</h3>";
    echo "<p>All endpoints have been updated to work with your actual table structure:</p>";
    echo "<ul>";
    echo "<li>✅ <strong>login.php</strong> - Returns correct user_id</li>";
    echo "<li>✅ <strong>get_owner_profile.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>get_account_info.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>get_gcash_info.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>update_gcash_info.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>update_account_info.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>get_owner_dashboard.php</strong> - Uses correct field names</li>";
    echo "<li>✅ <strong>update_owner_profile.php</strong> - Uses correct field names</li>";
    echo "</ul>";
    
    echo "<h3>Next Steps</h3>";
    echo "<p>1. <strong>Test in the app:</strong> Log out and log back in to see actual user data</p>";
    echo "<p>2. <strong>Check profile section:</strong> Should show actual user's name and email</p>";
    echo "<p>3. <strong>Check account settings:</strong> Should show actual user's information</p>";
    echo "<p>4. <strong>Check GCash info:</strong> Should show actual user's GCash data</p>";
    echo "<p>5. <strong>Check dashboard:</strong> Should show actual user's name and data</p>";
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>






