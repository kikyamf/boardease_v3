<?php
// Simple Payment System Test
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== PAYMENT SYSTEM TEST ===\n\n";

// Test database connection
try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "✅ Database connection successful\n\n";
} catch (Exception $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "\n";
    exit;
}

// Test 1: Check if required tables exist
echo "📋 CHECKING REQUIRED TABLES:\n\n";

$required_tables = ['bills', 'active_boarders', 'users', 'registrations', 'room_units', 'boarding_house_rooms', 'boarding_houses'];

foreach ($required_tables as $table) {
    try {
        $stmt = $db->prepare("SHOW TABLES LIKE ?");
        $stmt->execute([$table]);
        if ($stmt->rowCount() > 0) {
            echo "✅ Table '$table' exists\n";
        } else {
            echo "❌ Table '$table' missing\n";
        }
    } catch (Exception $e) {
        echo "❌ Error checking table '$table': " . $e->getMessage() . "\n";
    }
}

echo "\n";

// Test 2: Check table structures
echo "📋 CHECKING TABLE STRUCTURES:\n\n";

// Check bills table structure
try {
    $stmt = $db->prepare("DESCRIBE bills");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "✅ Bills table structure:\n";
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    echo "\n";
} catch (Exception $e) {
    echo "❌ Error checking bills table: " . $e->getMessage() . "\n\n";
}

// Check active_boarders table structure
try {
    $stmt = $db->prepare("DESCRIBE active_boarders");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "✅ Active_boarders table structure:\n";
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    echo "\n";
} catch (Exception $e) {
    echo "❌ Error checking active_boarders table: " . $e->getMessage() . "\n\n";
}

// Test 3: Check sample data
echo "📋 CHECKING SAMPLE DATA:\n\n";

try {
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM bills");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "✅ Bills count: {$result['count']}\n";
} catch (Exception $e) {
    echo "❌ Error checking bills count: " . $e->getMessage() . "\n";
}

try {
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM active_boarders");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "✅ Active boarders count: {$result['count']}\n";
} catch (Exception $e) {
    echo "❌ Error checking active_boarders count: " . $e->getMessage() . "\n";
}

try {
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM boarding_houses");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "✅ Boarding houses count: {$result['count']}\n";
} catch (Exception $e) {
    echo "❌ Error checking boarding_houses count: " . $e->getMessage() . "\n";
}

echo "\n";

// Test 4: Test the payment query structure
echo "📋 TESTING PAYMENT QUERY STRUCTURE:\n\n";

try {
    $sql = "
        SELECT 
            b.bill_id,
            b.active_id,
            b.amount_due,
            b.due_date,
            b.status,
            b.created_at,
            CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
            r.email as boarder_email,
            r.phone_number as boarder_phone,
            ru.room_number as room_name,
            bhr.room_name as room_type,
            bhr.room_category as rent_type,
            bhr.price as room_price,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            ab.status as boarder_status,
            u.profile_picture
        FROM bills b
        INNER JOIN active_boarders ab ON b.active_id = ab.active_id
        INNER JOIN users u ON ab.user_id = u.user_id
        INNER JOIN registrations r ON u.reg_id = r.reg_id
        LEFT JOIN room_units ru ON ab.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE bh.user_id = 1
        ORDER BY b.created_at DESC
        LIMIT 1
    ";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([1]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result) {
        echo "✅ Payment query executed successfully\n";
        echo "   Sample result: Bill ID {$result['bill_id']}, Amount: P{$result['amount_due']}, Status: {$result['status']}\n";
    } else {
        echo "⚠️  Payment query executed but no data found (this is normal if no sample data exists)\n";
    }
} catch (Exception $e) {
    echo "❌ Payment query failed: " . $e->getMessage() . "\n";
}

echo "\n=== TEST COMPLETE ===\n";
?>




