<?php
require_once 'db_helper.php';

echo "<h1>Testing Payment APIs with Bills Table</h1>";

// Test data
$owner_id = 1; // Your owner ID from the database

echo "<h2>1. Testing get_payment_status.php</h2>";

// Test getting all payments (GET method)
$url = "http://localhost/BoardEase2/get_payment_status.php?owner_id=$owner_id&status=all";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "<h3>All Payments Response (HTTP $httpCode):</h3>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

// Test getting pending payments (GET method)
$url = "http://localhost/BoardEase2/get_payment_status.php?owner_id=$owner_id&status=unpaid";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
curl_close($ch);

echo "<h3>Pending Payments Response:</h3>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

echo "<h2>2. Testing get_payment_summary.php</h2>";

// Test payment summary (GET method)
$url = "http://localhost/BoardEase2/get_payment_summary.php?owner_id=$owner_id&period=month";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
curl_close($ch);

echo "<h3>Payment Summary Response:</h3>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

echo "<h2>3. Testing mark_payments_overdue.php</h2>";

$testData = [
    'owner_id' => $owner_id
];

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, 'http://localhost/BoardEase2/mark_payments_overdue.php');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($testData));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
curl_close($ch);

echo "<h3>Mark Overdue Response:</h3>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

echo "<h2>4. Database Schema Check</h2>";

try {
    $db = new Database();
    $conn = $db->getConnection();
    
    // Check if bills table exists and has data
    $stmt = $conn->query("SELECT COUNT(*) as count FROM bills");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Bills table has {$result['count']} records</p>";
    
    // Check bills structure
    $stmt = $conn->query("DESCRIBE bills");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "<h3>Bills Table Structure:</h3>";
    echo "<table border='1'>";
    echo "<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>";
    foreach ($columns as $column) {
        echo "<tr>";
        echo "<td>{$column['Field']}</td>";
        echo "<td>{$column['Type']}</td>";
        echo "<td>{$column['Null']}</td>";
        echo "<td>{$column['Key']}</td>";
        echo "<td>{$column['Default']}</td>";
        echo "<td>{$column['Extra']}</td>";
        echo "</tr>";
    }
    echo "</table>";
    
    // Check active_boarders table
    $stmt = $conn->query("SELECT COUNT(*) as count FROM active_boarders");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Active boarders table has {$result['count']} records</p>";
    
    // Check users table
    $stmt = $conn->query("SELECT COUNT(*) as count FROM users");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Users table has {$result['count']} records</p>";
    
    // Check boarding_houses table
    $stmt = $conn->query("SELECT COUNT(*) as count FROM boarding_houses WHERE user_id = $owner_id");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Owner $owner_id has {$result['count']} boarding houses</p>";
    
} catch (Exception $e) {
    echo "<p style='color: red;'>Database Error: " . $e->getMessage() . "</p>";
}

echo "<h2>5. Sample Data Insertion (if needed)</h2>";

try {
    $db = new Database();
    $conn = $db->getConnection();
    
    // Check if we have any bills
    $stmt = $conn->query("SELECT COUNT(*) as count FROM bills");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result['count'] == 0) {
        echo "<p>No bills found. Creating sample data...</p>";
        
        // First, let's check if we have active boarders
        $stmt = $conn->query("SELECT COUNT(*) as count FROM active_boarders");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($result['count'] == 0) {
            echo "<p>No active boarders found. Creating sample active boarder...</p>";
            
            // Get a boarder user
            $stmt = $conn->query("SELECT user_id FROM users WHERE user_id != $owner_id LIMIT 1");
            $boarder = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($boarder) {
                $stmt = $conn->prepare("INSERT INTO active_boarders (user_id, status) VALUES (?, 'Active')");
                $stmt->execute([$boarder['user_id']]);
                $active_id = $conn->lastInsertId();
                
                echo "<p>Created active boarder with ID: $active_id</p>";
                
                // Create sample bills
                $stmt = $conn->prepare("
                    INSERT INTO bills (active_id, amount_due, due_date, status) 
                    VALUES (?, ?, ?, ?)
                ");
                
                $bills = [
                    [$active_id, 5000.00, date('Y-m-d', strtotime('+5 days')), 'Unpaid'],
                    [$active_id, 3000.00, date('Y-m-d', strtotime('-2 days')), 'Unpaid'],
                    [$active_id, 4000.00, date('Y-m-d', strtotime('-10 days')), 'Paid']
                ];
                
                foreach ($bills as $bill) {
                    $stmt->execute($bill);
                }
                
                echo "<p>Created 3 sample bills</p>";
            } else {
                echo "<p>No boarder users found. Please create some users first.</p>";
            }
        } else {
            echo "<p>Active boarders exist. Creating sample bills...</p>";
            
            // Get first active boarder
            $stmt = $conn->query("SELECT active_id FROM active_boarders LIMIT 1");
            $active_boarder = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($active_boarder) {
                $stmt = $conn->prepare("
                    INSERT INTO bills (active_id, amount_due, due_date, status) 
                    VALUES (?, ?, ?, ?)
                ");
                
                $bills = [
                    [$active_boarder['active_id'], 5000.00, date('Y-m-d', strtotime('+5 days')), 'Unpaid'],
                    [$active_boarder['active_id'], 3000.00, date('Y-m-d', strtotime('-2 days')), 'Unpaid'],
                    [$active_boarder['active_id'], 4000.00, date('Y-m-d', strtotime('-10 days')), 'Paid']
                ];
                
                foreach ($bills as $bill) {
                    $stmt->execute($bill);
                }
                
                echo "<p>Created 3 sample bills</p>";
            }
        }
    } else {
        echo "<p>Bills table already has data.</p>";
    }
    
} catch (Exception $e) {
    echo "<p style='color: red;'>Error creating sample data: " . $e->getMessage() . "</p>";
}

echo "<h2>6. Final Test - Get All Payments Again</h2>";

// Final test (GET method)
$url = "http://localhost/BoardEase2/get_payment_status.php?owner_id=$owner_id&status=all";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
curl_close($ch);

echo "<h3>Final All Payments Response:</h3>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

echo "<h2>Test Complete!</h2>";
echo "<p>If you see payment data above, the APIs are working correctly with your bills table.</p>";
?>
