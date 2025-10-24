<?php
// Direct test without database helper
$servername = "localhost";
$username = "boardease";
$password = "boardease";
$database = "boardease2";

try {
    $conn = new PDO("mysql:host=$servername;dbname=$database", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Database connection successful\n";
    
    $current_user_id = 6;
    echo "Testing with user_id: " . $current_user_id . "\n";
    
    // Get current user's role
    $stmt = $conn->prepare("
        SELECT 
            r.role as user_role,
            r.first_name,
            r.last_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$current_user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        echo "Current user not found\n";
        exit;
    }
    
    echo "Current user role: " . $current_user['user_role'] . "\n";
    
    if ($current_user['user_role'] === 'BH Owner') {
        echo "User is BH Owner - checking for active boarders\n";
        
        // Test the query
        $stmt = $conn->prepare("
            SELECT COUNT(*) as count
            FROM active_boarders ab
            JOIN users u ON ab.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            WHERE ab.boarding_house_id IN (
                SELECT bh_id FROM boarding_houses WHERE user_id = ?
            )
            AND ab.user_id != ? 
            AND ab.status = 'active'
            AND r.role = 'Boarder' 
            AND r.status = 'approved'
        ");
        
        $stmt->execute([$current_user_id, $current_user_id]);
        $result = $stmt->fetch();
        echo "Active boarders count: " . $result['count'] . "\n";
        
    } else {
        echo "User is not BH Owner\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

echo "Test completed.\n";
?>




