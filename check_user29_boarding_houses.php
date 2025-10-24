<?php
// Check if user 29 has boarding houses
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Check User 29 Boarding Houses</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    echo "<p>1. Checking if user 29 exists...</p>";
    $stmt = $db->prepare("SELECT user_id, reg_id, status FROM users WHERE user_id = ?");
    $stmt->execute([29]);
    $user = $stmt->fetch();
    
    if ($user) {
        echo "<p>✅ User 29 exists - Status: " . $user['status'] . ", Reg ID: " . $user['reg_id'] . "</p>";
    } else {
        echo "<p>❌ User 29 does not exist</p>";
        exit;
    }
    
    echo "<p>2. Checking boarding houses for user 29...</p>";
    $stmt = $db->prepare("SELECT bh_id, bh_name, bh_address, status FROM boarding_houses WHERE user_id = ?");
    $stmt->execute([29]);
    $boarding_houses = $stmt->fetchAll();
    
    if (count($boarding_houses) > 0) {
        echo "<p>✅ Found " . count($boarding_houses) . " boarding house(s) for user 29:</p>";
        echo "<ul>";
        foreach ($boarding_houses as $bh) {
            echo "<li>ID: " . $bh['bh_id'] . ", Name: " . $bh['bh_name'] . ", Status: " . $bh['status'] . "</li>";
        }
        echo "</ul>";
    } else {
        echo "<p>❌ No boarding houses found for user 29</p>";
        echo "<p>3. Creating a default boarding house for user 29...</p>";
        
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
            VALUES (?, 'Default BH for User 29', 'Default Address', 'Default description', 'Default rules', 1, 100.00, 2024, 'Active', NOW())
        ");
        $result = $stmt->execute([29]);
        
        if ($result) {
            $bh_id = $db->lastInsertId();
            echo "<p>✅ Created default boarding house with ID: " . $bh_id . "</p>";
        } else {
            echo "<p>❌ Failed to create default boarding house</p>";
        }
    }
    
    echo "<p>4. Testing group creation with user 29...</p>";
    $stmt = $db->prepare("SELECT bh_id FROM boarding_houses WHERE user_id = ? LIMIT 1");
    $stmt->execute([29]);
    $bh_result = $stmt->fetch();
    
    if ($bh_result) {
        echo "<p>✅ Can use bh_id: " . $bh_result['bh_id'] . " for group creation</p>";
    } else {
        echo "<p>❌ No boarding house available for group creation</p>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
    echo "<p>Stack trace: " . $e->getTraceAsString() . "</p>";
}
?>




