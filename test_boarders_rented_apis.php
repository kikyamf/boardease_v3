<?php
// Test script for boarders rented APIs
include 'dbConfig.php';

echo "<h2>Testing Boarders Rented APIs</h2>\n";

// Test user_id (replace with actual owner user_id from your database)
$test_user_id = 1;

echo "<h3>1. Testing get_current_boarders.php</h3>\n";

// Test get_current_boarders.php
$url = "http://localhost/BoardEase2/get_current_boarders.php";
$data = array('user_id' => $test_user_id);

$options = array(
    'http' => array(
        'header'  => "Content-type: application/x-www-form-urlencoded\r\n",
        'method'  => 'POST',
        'content' => http_build_query($data)
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);

if ($result === FALSE) {
    echo "Error: Could not connect to get_current_boarders.php\n";
} else {
    echo "Response: " . $result . "\n";
    $response = json_decode($result, true);
    if ($response && $response['success']) {
        echo "✅ Current boarders API working - Found " . count($response['boarders']) . " current boarders\n";
    } else {
        echo "❌ Current boarders API error: " . ($response['error'] ?? 'Unknown error') . "\n";
    }
}

echo "<h3>2. Testing get_boarders_history.php</h3>\n";

// Test get_boarders_history.php
$url = "http://localhost/BoardEase2/get_boarders_history.php";
$data = array('user_id' => $test_user_id);

$options = array(
    'http' => array(
        'header'  => "Content-type: application/x-www-form-urlencoded\r\n",
        'method'  => 'POST',
        'content' => http_build_query($data)
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);

if ($result === FALSE) {
    echo "Error: Could not connect to get_boarders_history.php\n";
} else {
    echo "Response: " . $result . "\n";
    $response = json_decode($result, true);
    if ($response && $response['success']) {
        echo "✅ Boarders history API working - Found " . count($response['boarders_history']) . " historical boarders\n";
    } else {
        echo "❌ Boarders history API error: " . ($response['error'] ?? 'Unknown error') . "\n";
    }
}

echo "<h3>3. Testing get_owner_boarders.php (existing API)</h3>\n";

// Test get_owner_boarders.php
$url = "http://localhost/BoardEase2/get_owner_boarders.php";
$data = array('user_id' => $test_user_id);

$options = array(
    'http' => array(
        'header'  => "Content-type: application/x-www-form-urlencoded\r\n",
        'method'  => 'POST',
        'content' => http_build_query($data)
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);

if ($result === FALSE) {
    echo "Error: Could not connect to get_owner_boarders.php\n";
} else {
    echo "Response: " . $result . "\n";
    $response = json_decode($result, true);
    if ($response && $response['success']) {
        echo "✅ All boarders API working - Found " . count($response['boarders']) . " total boarders\n";
    } else {
        echo "❌ All boarders API error: " . ($response['error'] ?? 'Unknown error') . "\n";
    }
}

echo "<h3>4. Database Query Test</h3>\n";

// Test the database queries directly
try {
    // Test current boarders query (from active_boarders table)
    $sql = "SELECT 
                ab.active_boarder_id as boarder_id,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                reg.email as boarder_email,
                reg.phone_number as boarder_phone,
                bh.bh_name as boarding_house_name,
                ru.room_number,
                bhr.room_category as rent_type,
                ab.start_date,
                ab.end_date,
                ab.status,
                u.profile_picture
            FROM active_boarders ab
            JOIN room_units ru ON ab.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            JOIN users u ON ab.user_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            WHERE bh.user_id = ? 
            AND ab.status = 'Active'
            ORDER BY ab.start_date DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $current_count = 0;
    while ($row = $result->fetch_assoc()) {
        $current_count++;
    }

    echo "✅ Current boarders query (active_boarders): Found $current_count current boarders\n";

    // Test history query (from bookings table)
    $sql = "SELECT 
                b.booking_id as boarder_id,
                CONCAT(reg.f_name, ' ', reg.l_name) as boarder_name,
                reg.email as boarder_email,
                reg.phone_number as boarder_phone,
                bh.bh_name as boarding_house_name,
                ru.room_number,
                bhr.room_category as rent_type,
                b.start_date,
                b.end_date,
                b.booking_status as status,
                u.profile_picture
            FROM bookings b
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            JOIN users u ON b.user_id = u.user_id
            JOIN registrations reg ON u.reg_id = reg.reg_id
            WHERE bh.user_id = ? 
            AND b.booking_status = 'Completed'
            ORDER BY b.end_date DESC, b.start_date DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $test_user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $history_count = 0;
    while ($row = $result->fetch_assoc()) {
        $history_count++;
    }

    echo "✅ Boarders history query (bookings): Found $history_count historical boarders\n";

} catch (Exception $e) {
    echo "❌ Database query error: " . $e->getMessage() . "\n";
}

echo "<h3>5. Testing Active Boarders Management</h3>\n";

// Test manage_active_boarders.php
$url = "http://localhost/BoardEase2/manage_active_boarders.php";
$data = array(
    'action' => 'add_active_boarder',
    'user_id' => $test_user_id,
    'booking_id' => 1  // Replace with actual booking_id
);

$options = array(
    'http' => array(
        'header'  => "Content-type: application/x-www-form-urlencoded\r\n",
        'method'  => 'POST',
        'content' => http_build_query($data)
    )
);

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);

if ($result === FALSE) {
    echo "Error: Could not connect to manage_active_boarders.php\n";
} else {
    echo "Add Active Boarder Response: " . $result . "\n";
    $response = json_decode($result, true);
    if ($response && $response['success']) {
        echo "✅ Active boarder management API working\n";
    } else {
        echo "❌ Active boarder management API error: " . ($response['error'] ?? 'Unknown error') . "\n";
    }
}

echo "<h3>6. Summary</h3>\n";
echo "Test completed. Check the results above to verify that the boarders rented functionality is working correctly.\n";
echo "Make sure to update the test_user_id variable with an actual owner user_id from your database.\n";
echo "\n<b>New Logic:</b>\n";
echo "- <b>Current Boarders:</b> Retrieved from 'active_boarders' table (status = 'Active')\n";
echo "- <b>Boarders History:</b> Retrieved from 'bookings' table (status = 'Completed')\n";
echo "- <b>When rental starts:</b> Add to 'active_boarders' table\n";
echo "- <b>When rental ends:</b> Remove from 'active_boarders' table and update booking status to 'Completed'\n";
?>