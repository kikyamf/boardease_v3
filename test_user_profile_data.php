<?php
// Test script to check user profile data
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Test User Profile Data</h2>";

try {
    // Test different user IDs to see their profile data
    $test_user_ids = [1, 2, 3, 4, 5];
    
    foreach ($test_user_ids as $user_id) {
        echo "<h3>Testing User ID: $user_id</h3>";
        
        // Check if user exists in users table
        $stmt = $conn->prepare("
            SELECT u.user_id, u.reg_id, u.status, r.first_name, r.last_name, r.email, r.role, r.status as reg_status
            FROM users u 
            JOIN registrations r ON u.reg_id = r.id 
            WHERE u.user_id = ?
        ");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $user_data = $result->fetch_assoc();
        $stmt->close();
        
        if ($user_data) {
            echo "<p><strong>✅ User found:</strong></p>";
            echo "<ul>";
            echo "<li><strong>User ID:</strong> " . $user_data['user_id'] . "</li>";
            echo "<li><strong>Registration ID:</strong> " . $user_data['reg_id'] . "</li>";
            echo "<li><strong>Name:</strong> " . $user_data['first_name'] . " " . $user_data['last_name'] . "</li>";
            echo "<li><strong>Email:</strong> " . $user_data['email'] . "</li>";
            echo "<li><strong>Role:</strong> " . $user_data['role'] . "</li>";
            echo "<li><strong>User Status:</strong> " . $user_data['status'] . "</li>";
            echo "<li><strong>Registration Status:</strong> " . $user_data['reg_status'] . "</li>";
            echo "</ul>";
            
            // Test profile picture
            $stmt = $conn->prepare("SELECT profile_picture FROM users WHERE user_id = ?");
            $stmt->bind_param("i", $user_id);
            $stmt->execute();
            $result = $stmt->get_result();
            $profile_data = $result->fetch_assoc();
            $stmt->close();
            
            if ($profile_data && $profile_data['profile_picture']) {
                echo "<li><strong>Profile Picture:</strong> " . $profile_data['profile_picture'] . "</li>";
            } else {
                echo "<li><strong>Profile Picture:</strong> None</li>";
            }
            
        } else {
            echo "<p><strong>❌ User not found</strong></p>";
        }
        
        echo "<hr>";
    }
    
    // Show all users in the system
    echo "<h3>All Users in System</h3>";
    $stmt = $conn->prepare("
        SELECT u.user_id, r.first_name, r.last_name, r.email, r.role, r.status as reg_status
        FROM users u 
        JOIN registrations r ON u.reg_id = r.id 
        ORDER BY u.user_id
    ");
    $stmt->execute();
    $result = $stmt->get_result();
    
    echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
    echo "<tr><th>User ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr>";
    
    while ($row = $result->fetch_assoc()) {
        echo "<tr>";
        echo "<td>" . $row['user_id'] . "</td>";
        echo "<td>" . $row['first_name'] . " " . $row['last_name'] . "</td>";
        echo "<td>" . $row['email'] . "</td>";
        echo "<td>" . $row['role'] . "</td>";
        echo "<td>" . $row['reg_status'] . "</td>";
        echo "</tr>";
    }
    echo "</table>";
    
    $stmt->close();
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>
