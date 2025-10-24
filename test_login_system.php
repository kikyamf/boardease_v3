<?php
// Test the login system to verify user data is returned correctly
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Test Login System</h2>";

try {
    // Test with a known user email
    $test_email = "mari@gmail.com"; // Change this to a valid email in your system
    $test_password = "password123"; // Change this to the correct password
    
    echo "<h3>Testing login for: $test_email</h3>";
    
    // Simulate the login process
    $sql = "SELECT r.id, r.role, r.first_name, r.last_name, r.email, r.password, r.status, u.user_id 
            FROM registrations r 
            LEFT JOIN users u ON r.id = u.reg_id 
            WHERE r.email = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $test_email);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        echo "<p><strong>❌ User not found with email: $test_email</strong></p>";
        echo "<p>Available users in system:</p>";
        
        // Show all available users
        $stmt2 = $conn->prepare("SELECT r.email, r.first_name, r.last_name, r.role, r.status, u.user_id 
                                FROM registrations r 
                                LEFT JOIN users u ON r.id = u.reg_id 
                                ORDER BY r.id");
        $stmt2->execute();
        $result2 = $stmt2->get_result();
        
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>Email</th><th>Name</th><th>Role</th><th>Status</th><th>User ID</th></tr>";
        
        while ($row = $result2->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['email'] . "</td>";
            echo "<td>" . $row['first_name'] . " " . $row['last_name'] . "</td>";
            echo "<td>" . $row['role'] . "</td>";
            echo "<td>" . $row['status'] . "</td>";
            echo "<td>" . ($row['user_id'] ? $row['user_id'] : 'Not in users table') . "</td>";
            echo "</tr>";
        }
        echo "</table>";
        
        $stmt2->close();
    } else {
        $user = $result->fetch_assoc();
        $stmt->close();
        
        echo "<p><strong>✅ User found:</strong></p>";
        echo "<ul>";
        echo "<li><strong>Registration ID:</strong> " . $user['id'] . "</li>";
        echo "<li><strong>User ID:</strong> " . ($user['user_id'] ? $user['user_id'] : 'Not in users table') . "</li>";
        echo "<li><strong>Name:</strong> " . $user['first_name'] . " " . $user['last_name'] . "</li>";
        echo "<li><strong>Email:</strong> " . $user['email'] . "</li>";
        echo "<li><strong>Role:</strong> " . $user['role'] . "</li>";
        echo "<li><strong>Status:</strong> " . $user['status'] . "</li>";
        echo "</ul>";
        
        // Test password verification
        if (strpos($user['password'], '$2y$') === 0) {
            $passwordValid = password_verify($test_password, $user['password']);
            echo "<p><strong>Password verification (hashed):</strong> " . ($passwordValid ? "✅ Valid" : "❌ Invalid") . "</p>";
        } else {
            $passwordValid = ($test_password === $user['password']);
            echo "<p><strong>Password verification (plain text):</strong> " . ($passwordValid ? "✅ Valid" : "❌ Invalid") . "</p>";
        }
        
        if ($passwordValid && $user['status'] === 'approved') {
            // Simulate the login response
            $response = array(
                "success" => true,
                "message" => "Login successful",
                "user" => array(
                    "id" => $user['user_id'] ? $user['user_id'] : $user['id'],
                    "role" => $user['role'],
                    "firstName" => $user['first_name'],
                    "lastName" => $user['last_name'],
                    "email" => $user['email']
                )
            );
            
            echo "<h4>Login Response:</h4>";
            echo "<pre>" . json_encode($response, JSON_PRETTY_PRINT) . "</pre>";
            
            echo "<p><strong>✅ Login would be successful!</strong></p>";
            echo "<p><strong>User ID that will be stored in app:</strong> " . $response['user']['id'] . "</p>";
        } else {
            echo "<p><strong>❌ Login would fail:</strong></p>";
            if (!$passwordValid) {
                echo "<p>- Invalid password</p>";
            }
            if ($user['status'] !== 'approved') {
                echo "<p>- Account status: " . $user['status'] . " (needs to be 'approved')</p>";
            }
        }
    }
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>
