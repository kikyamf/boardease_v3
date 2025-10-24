<?php
// Create Sample Payment Data
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== CREATING SAMPLE PAYMENT DATA ===\n\n";

try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "✅ Database connection successful\n\n";
} catch (Exception $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "\n";
    exit;
}

// Start transaction
$db->beginTransaction();

try {
    // First, check if we have users and boarding houses
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
    $stmt->execute();
    $user_count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM boarding_houses");
    $stmt->execute();
    $bh_count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    
    echo "📊 Current data:\n";
    echo "   - Users: $user_count\n";
    echo "   - Boarding houses: $bh_count\n\n";
    
    if ($user_count == 0) {
        echo "⚠️  No users found. Creating sample users...\n";
        
        // Create sample registrations records
        $stmt = $db->prepare("
            INSERT INTO registrations (f_name, l_name, email, phone_number, birth_date, gender, address, university, student_id, emergency_contact_name, emergency_contact_phone, emergency_contact_relationship) 
            VALUES 
            ('John', 'Doe', 'john.doe@example.com', '09123456789', '1995-05-15', 'Male', '123 Main St, City', 'University of Example', 'STU001', 'Jane Doe', '09123456788', 'Mother'),
            ('Jane', 'Smith', 'jane.smith@example.com', '09123456790', '1996-08-20', 'Female', '456 Oak Ave, City', 'University of Example', 'STU002', 'Bob Smith', '09123456791', 'Father'),
            ('Mike', 'Johnson', 'mike.johnson@example.com', '09123456792', '1994-12-10', 'Male', '789 Pine Rd, City', 'University of Example', 'STU003', 'Sarah Johnson', '09123456793', 'Sister')
        ");
        $stmt->execute();
        
        // Create sample users
        $stmt = $db->prepare("
            INSERT INTO users (reg_id, profile_picture) 
            VALUES 
            (1, 'profile1.jpg'),
            (2, 'profile2.jpg'),
            (3, 'profile3.jpg')
        ");
        $stmt->execute();
        
        echo "✅ Created 3 sample users\n";
    }
    
    if ($bh_count == 0) {
        echo "⚠️  No boarding houses found. Creating sample boarding houses...\n";
        
        // Create sample boarding houses
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_contact) 
            VALUES 
            (1, 'Sunset Boarding House', '123 Sunset Blvd, City', '09123456789'),
            (2, 'Mountain View Dormitory', '456 Mountain St, City', '09123456790')
        ");
        $stmt->execute();
        
        echo "✅ Created 2 sample boarding houses\n";
    }
    
    // Create sample room categories
    $stmt = $db->prepare("
        INSERT IGNORE INTO boarding_house_rooms (bh_id, room_category, room_price, room_description, room_capacity, room_amenities) 
        VALUES 
        (1, 'Single Room', 5000.00, 'Private single room with basic amenities', 1, 'Bed, Desk, Chair, Wardrobe'),
        (1, 'Double Room', 3500.00, 'Shared double room with basic amenities', 2, '2 Beds, 2 Desks, 2 Chairs, Wardrobe'),
        (2, 'Triple Room', 2500.00, 'Shared triple room with basic amenities', 3, '3 Beds, 3 Desks, 3 Chairs, Wardrobe')
    ");
    $stmt->execute();
    
    // Create sample room units
    $stmt = $db->prepare("
        INSERT IGNORE INTO room_units (room_number, bhr_id, room_status) 
        VALUES 
        ('101', 1, 'Available'),
        ('102', 1, 'Available'),
        ('201', 2, 'Available'),
        ('202', 2, 'Available'),
        ('301', 3, 'Available'),
        ('302', 3, 'Available')
    ");
    $stmt->execute();
    
    echo "✅ Created sample room categories and units\n";
    
    // Create sample active boarders
    $stmt = $db->prepare("
        INSERT IGNORE INTO active_boarders (user_id, status) 
        VALUES 
        (1, 'Active'),
        (2, 'Active'),
        (3, 'Active')
    ");
    $stmt->execute();
    
    echo "✅ Created sample active boarders\n";
    
    // Create sample bills
    $stmt = $db->prepare("
        INSERT IGNORE INTO bills (active_id, amount_due, due_date, status) 
        VALUES 
        (1, 5000.00, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'Unpaid'),
        (1, 5000.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'Overdue'),
        (2, 3500.00, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'Unpaid'),
        (2, 3500.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'Paid'),
        (3, 2500.00, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Unpaid'),
        (3, 2500.00, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 'Overdue')
    ");
    $stmt->execute();
    
    echo "✅ Created sample bills\n";
    
    // Commit transaction
    $db->commit();
    
    echo "\n🎉 Sample payment data created successfully!\n\n";
    
    // Show summary
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM bills");
    $stmt->execute();
    $bill_count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM active_boarders");
    $stmt->execute();
    $active_count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    
    echo "📊 Final data summary:\n";
    echo "   - Bills: $bill_count\n";
    echo "   - Active boarders: $active_count\n";
    echo "   - Users: 3\n";
    echo "   - Boarding houses: 2\n";
    echo "   - Room units: 6\n\n";
    
    echo "✅ Payment system is now ready for testing!\n";
    
} catch (Exception $e) {
    // Rollback transaction on error
    $db->rollback();
    echo "❌ Error creating sample data: " . $e->getMessage() . "\n";
}
?>




