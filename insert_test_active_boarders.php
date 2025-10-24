<?php
// Insert test data into active_boarders table
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Inserting test data into active_boarders table...\n\n";

try {
    $db = getDB();
    
    // First, check if we have any boarding houses
    $stmt = $db->prepare("SELECT bh_id, user_id FROM boarding_houses LIMIT 1");
    $stmt->execute();
    $bh = $stmt->fetch();
    
    if (!$bh) {
        echo "❌ No boarding houses found. Creating one...\n";
        // Create a test boarding house
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
            VALUES (1, 'Test Boarding House', '123 Test St', 'Test description', 'Test rules', 2, 100, 2024, 'active', NOW())
        ");
        $stmt->execute();
        $bh_id = $db->lastInsertId();
        echo "✅ Created boarding house with ID: " . $bh_id . "\n";
    } else {
        $bh_id = $bh['bh_id'];
        echo "✅ Using existing boarding house ID: " . $bh_id . " (Owner: " . $bh['user_id'] . ")\n";
    }
    
    // Clear existing test data
    $stmt = $db->prepare("DELETE FROM active_boarders WHERE user_id IN (2, 3)");
    $stmt->execute();
    echo "✅ Cleared existing test data\n";
    
    // Insert test active boarders
    $test_data = [
        ['user_id' => 2, 'boarding_house_id' => $bh_id, 'status' => 'Active'],
        ['user_id' => 3, 'boarding_house_id' => $bh_id, 'status' => 'Active']
    ];
    
    $stmt = $db->prepare("
        INSERT INTO active_boarders (user_id, boarding_house_id, status, created_at, updated_at) 
        VALUES (?, ?, ?, NOW(), NOW())
    ");
    
    foreach ($test_data as $data) {
        $stmt->execute([$data['user_id'], $data['boarding_house_id'], $data['status']]);
        echo "✅ Inserted user " . $data['user_id'] . " into boarding house " . $data['boarding_house_id'] . "\n";
    }
    
    // Verify the data
    $stmt = $db->prepare("SELECT * FROM active_boarders WHERE user_id IN (2, 3)");
    $stmt->execute();
    $results = $stmt->fetchAll();
    
    echo "\n✅ Test data inserted successfully:\n";
    foreach ($results as $result) {
        echo "- User ID: " . $result['user_id'] . ", Boarding House ID: " . $result['boarding_house_id'] . ", Status: " . $result['status'] . "\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




