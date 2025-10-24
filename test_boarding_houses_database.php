<?php
// Test script to check boarding houses database structure and data
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Test Boarding Houses Database</h2>";

try {
    // Check if boarding_houses table exists and show its structure
    echo "<h3>1. Checking boarding_houses table structure</h3>";
    
    $result = $conn->query("DESCRIBE boarding_houses");
    if ($result) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>";
        
        while ($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['Field'] . "</td>";
            echo "<td>" . $row['Type'] . "</td>";
            echo "<td>" . $row['Null'] . "</td>";
            echo "<td>" . $row['Key'] . "</td>";
            echo "<td>" . $row['Default'] . "</td>";
            echo "<td>" . $row['Extra'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<p style='color: red;'>❌ boarding_houses table does not exist!</p>";
    }
    
    // Check if boarding_house_images table exists
    echo "<h3>2. Checking boarding_house_images table structure</h3>";
    
    $result = $conn->query("DESCRIBE boarding_house_images");
    if ($result) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>";
        
        while ($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['Field'] . "</td>";
            echo "<td>" . $row['Type'] . "</td>";
            echo "<td>" . $row['Null'] . "</td>";
            echo "<td>" . $row['Key'] . "</td>";
            echo "<td>" . $row['Default'] . "</td>";
            echo "<td>" . $row['Extra'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<p style='color: red;'>❌ boarding_house_images table does not exist!</p>";
    }
    
    // Show all boarding houses data
    echo "<h3>3. All boarding houses in database</h3>";
    
    $result = $conn->query("SELECT * FROM boarding_houses ORDER BY bh_id");
    if ($result && $result->num_rows > 0) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>BH ID</th><th>User ID</th><th>Name</th><th>Address</th><th>Contact</th><th>Created</th></tr>";
        
        while ($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['bh_id'] . "</td>";
            echo "<td>" . $row['user_id'] . "</td>";
            echo "<td>" . $row['bh_name'] . "</td>";
            echo "<td>" . $row['bh_address'] . "</td>";
            echo "<td>" . ($row['bh_contact'] ?? 'N/A') . "</td>";
            echo "<td>" . $row['created_at'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<p style='color: orange;'>⚠️ No boarding houses found in database</p>";
    }
    
    // Show all boarding house images
    echo "<h3>4. All boarding house images</h3>";
    
    $result = $conn->query("SELECT * FROM boarding_house_images ORDER BY bh_id");
    if ($result && $result->num_rows > 0) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>Image ID</th><th>BH ID</th><th>Image Path</th><th>Created</th></tr>";
        
        while ($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['image_id'] . "</td>";
            echo "<td>" . $row['bh_id'] . "</td>";
            echo "<td>" . $row['image_path'] . "</td>";
            echo "<td>" . $row['created_at'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<p style='color: orange;'>⚠️ No boarding house images found in database</p>";
    }
    
    // Test the get_boarding_houses.php endpoint
    echo "<h3>5. Testing get_boarding_houses.php endpoint</h3>";
    
    // Test with a specific bh_id
    $test_bh_id = 1;
    echo "<p>Testing with bh_id = $test_bh_id</p>";
    
    $sql = "SELECT bh.bh_id, bh.bh_name, bh.bh_address, bh.bh_contact, bh.user_id,
                   bh.bh_description, bh.bh_rules, bh.number_of_bathroom, bh.area, bh.build_year,
                   GROUP_CONCAT(bhi.image_path) as images
            FROM boarding_houses bh
            LEFT JOIN boarding_house_images bhi ON bh.bh_id = bhi.bh_id
            WHERE bh.bh_id = ?
            GROUP BY bh.bh_id";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_bh_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        echo "<p style='color: green;'>✅ Boarding house found:</p>";
        echo "<pre>" . json_encode($row, JSON_PRETTY_PRINT) . "</pre>";
    } else {
        echo "<p style='color: red;'>❌ Boarding house with ID $test_bh_id not found</p>";
    }
    
    $stmt->close();
    
    // Show recommendations
    echo "<h3>6. Recommendations</h3>";
    echo "<ul>";
    echo "<li>If boarding_houses table is missing fields, you need to add them</li>";
    echo "<li>If boarding_house_images table doesn't exist, you need to create it</li>";
    echo "<li>If no boarding houses exist, you need to create some test data</li>";
    echo "</ul>";
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>






