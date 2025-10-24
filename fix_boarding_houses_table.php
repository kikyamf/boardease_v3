<?php
// Script to add missing fields to boarding_houses table
require_once 'dbConfig.php';

header('Content-Type: application/json');

echo "<h2>Fix Boarding Houses Table</h2>";

try {
    // Check current table structure
    echo "<h3>1. Current table structure</h3>";
    $result = $conn->query("DESCRIBE boarding_houses");
    if ($result) {
        echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>";
        
        $existing_fields = [];
        while ($row = $result->fetch_assoc()) {
            $existing_fields[] = $row['Field'];
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
        exit;
    }
    
    // Fields that the app expects
    $required_fields = [
        'bh_description' => 'TEXT',
        'bh_rules' => 'TEXT',
        'number_of_bathroom' => 'INT(11)',
        'area' => 'VARCHAR(50)',
        'build_year' => 'VARCHAR(10)'
    ];
    
    echo "<h3>2. Adding missing fields</h3>";
    
    $fields_added = 0;
    foreach ($required_fields as $field => $type) {
        if (!in_array($field, $existing_fields)) {
            $sql = "ALTER TABLE boarding_houses ADD COLUMN $field $type";
            if ($conn->query($sql)) {
                echo "<p style='color: green;'>✅ Added field: $field ($type)</p>";
                $fields_added++;
            } else {
                echo "<p style='color: red;'>❌ Failed to add field: $field - " . $conn->error . "</p>";
            }
        } else {
            echo "<p style='color: blue;'>ℹ️ Field already exists: $field</p>";
        }
    }
    
    // Create boarding_house_images table if it doesn't exist
    echo "<h3>3. Checking boarding_house_images table</h3>";
    
    $result = $conn->query("SHOW TABLES LIKE 'boarding_house_images'");
    if ($result->num_rows == 0) {
        $sql = "CREATE TABLE boarding_house_images (
            image_id INT(11) NOT NULL AUTO_INCREMENT,
            bh_id INT(11) NOT NULL,
            image_path VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (image_id),
            KEY bh_id (bh_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
        
        if ($conn->query($sql)) {
            echo "<p style='color: green;'>✅ Created boarding_house_images table</p>";
        } else {
            echo "<p style='color: red;'>❌ Failed to create boarding_house_images table: " . $conn->error . "</p>";
        }
    } else {
        echo "<p style='color: blue;'>ℹ️ boarding_house_images table already exists</p>";
    }
    
    // Show final table structure
    echo "<h3>4. Final table structure</h3>";
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
    }
    
    // Summary
    echo "<h3>5. Summary</h3>";
    if ($fields_added > 0) {
        echo "<p style='color: green;'>✅ Successfully added $fields_added fields to boarding_houses table</p>";
    } else {
        echo "<p style='color: blue;'>ℹ️ No fields needed to be added</p>";
    }
    
    echo "<p><strong>Next steps:</strong></p>";
    echo "<ul>";
    echo "<li>Test the manage listing edit button again</li>";
    echo "<li>If you still get 'boarding house not found', check if there are any boarding houses in the database</li>";
    echo "<li>You may need to create some test boarding houses first</li>";
    echo "</ul>";
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>






