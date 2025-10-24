<?php
// Simple test script for boarding houses
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Test Boarding Houses</h2>";

try {
    // Show all boarding houses in the database
    echo "<h3>All Boarding Houses in Database</h3>";
    
    $sql = "SELECT bh_id, user_id, bh_name, bh_address, bh_description, bh_rules, 
                   number_of_bathroom, area, build_year, status, bh_created_at
            FROM boarding_houses 
            ORDER BY bh_id DESC";
    
    $result = $conn->query($sql);
    
    if ($result && $result->num_rows > 0) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>BH ID</th><th>User ID</th><th>Name</th><th>Address</th><th>Description</th><th>Rules</th><th>Bathrooms</th><th>Area</th><th>Build Year</th><th>Status</th><th>Created</th></tr>";
        
        while ($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['bh_id'] . "</td>";
            echo "<td>" . $row['user_id'] . "</td>";
            echo "<td>" . $row['bh_name'] . "</td>";
            echo "<td>" . $row['bh_address'] . "</td>";
            echo "<td>" . (strlen($row['bh_description']) > 50 ? substr($row['bh_description'], 0, 50) . "..." : $row['bh_description']) . "</td>";
            echo "<td>" . (strlen($row['bh_rules']) > 50 ? substr($row['bh_rules'], 0, 50) . "..." : $row['bh_rules']) . "</td>";
            echo "<td>" . $row['number_of_bathroom'] . "</td>";
            echo "<td>" . $row['area'] . "</td>";
            echo "<td>" . $row['build_year'] . "</td>";
            echo "<td>" . $row['status'] . "</td>";
            echo "<td>" . $row['bh_created_at'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
        
        // Test the get_boarding_houses.php endpoint
        echo "<h3>Testing get_boarding_houses.php Endpoint</h3>";
        
        // Test with first boarding house
        $first_bh = $result->fetch_assoc();
        if ($first_bh) {
            $test_bh_id = $first_bh['bh_id'];
            echo "<p>Testing with bh_id = $test_bh_id</p>";
            
            // Simulate the API call
            $_POST['bh_id'] = $test_bh_id;
            
            ob_start();
            include 'get_boarding_houses.php';
            $response = ob_get_clean();
            
            echo "<h4>API Response:</h4>";
            echo "<pre>" . htmlspecialchars($response) . "</pre>";
            
            // Parse and display nicely
            $data = json_decode($response, true);
            if ($data) {
                echo "<h4>Parsed Response:</h4>";
                echo "<ul>";
                echo "<li><strong>BH ID:</strong> " . $data['bh_id'] . "</li>";
                echo "<li><strong>Name:</strong> " . $data['bh_name'] . "</li>";
                echo "<li><strong>Address:</strong> " . $data['bh_address'] . "</li>";
                echo "<li><strong>Description:</strong> " . $data['bh_description'] . "</li>";
                echo "<li><strong>Rules:</strong> " . $data['bh_rules'] . "</li>";
                echo "<li><strong>Bathrooms:</strong> " . $data['number_of_bathroom'] . "</li>";
                echo "<li><strong>Area:</strong> " . $data['area'] . "</li>";
                echo "<li><strong>Build Year:</strong> " . $data['build_year'] . "</li>";
                echo "<li><strong>Status:</strong> " . $data['status'] . "</li>";
                echo "<li><strong>Images:</strong> " . count($data['images']) . " images</li>";
                echo "</ul>";
            }
        }
        
    } else {
        echo "<p style='color: red;'>❌ No boarding houses found in database</p>";
        echo "<p>You need to create some boarding houses first.</p>";
    }
    
    // Check if boarding_house_images table exists and has data
    echo "<h3>Boarding House Images</h3>";
    
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
        echo "<p style='color: orange;'>⚠️ No boarding house images found</p>";
    }
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>






