<?php
// Create Sample Payment Data with proper relationships
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

try {
    // Start transaction
    $db->beginTransaction();
    
    echo "📋 Creating sample payment data...\n";
    
    // First, get existing data
    $stmt = $db->prepare("SELECT user_id FROM users LIMIT 3");
    $stmt->execute();
    $users = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 2");
    $stmt->execute();
    $boarding_houses = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    $stmt = $db->prepare("SELECT room_id FROM room_units LIMIT 3");
    $stmt->execute();
    $rooms = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    echo "📊 Found existing data:\n";
    echo "   - Users: " . count($users) . "\n";
    echo "   - Boarding houses: " . count($boarding_houses) . "\n";
    echo "   - Rooms: " . count($rooms) . "\n\n";
    
    if (count($users) == 0 || count($boarding_houses) == 0 || count($rooms) == 0) {
        echo "⚠️  Not enough existing data. Creating basic data first...\n";
        
        // Create sample registrations if needed
        if (count($users) == 0) {
            $stmt = $db->prepare("
                INSERT IGNORE INTO registrations (f_name, l_name, email, phone_number, birth_date, gender, address, university, student_id, emergency_contact_name, emergency_contact_phone, emergency_contact_relationship) 
                VALUES 
                ('John', 'Doe', 'john.doe@example.com', '09123456789', '1995-05-15', 'Male', '123 Main St, City', 'University of Example', 'STU001', 'Jane Doe', '09123456788', 'Mother'),
                ('Jane', 'Smith', 'jane.smith@example.com', '09123456790', '1996-08-20', 'Female', '456 Oak Ave, City', 'University of Example', 'STU002', 'Bob Smith', '09123456791', 'Father'),
                ('Mike', 'Johnson', 'mike.johnson@example.com', '09123456792', '1994-12-10', 'Male', '789 Pine Rd, City', 'University of Example', 'STU003', 'Sarah Johnson', '09123456793', 'Sister')
            ");
            $stmt->execute();
            
            $stmt = $db->prepare("
                INSERT IGNORE INTO users (reg_id, profile_picture) 
                VALUES 
                (1, 'profile1.jpg'),
                (2, 'profile2.jpg'),
                (3, 'profile3.jpg')
            ");
            $stmt->execute();
            
            $users = [1, 2, 3];
            echo "✅ Created sample users\n";
        }
        
        // Create sample boarding houses if needed
        if (count($boarding_houses) == 0) {
            $stmt = $db->prepare("
                INSERT IGNORE INTO boarding_houses (user_id, bh_name, bh_address, bh_contact) 
                VALUES 
                (1, 'Sunset Boarding House', '123 Sunset Blvd, City', '09123456789'),
                (2, 'Mountain View Dormitory', '456 Mountain St, City', '09123456790')
            ");
            $stmt->execute();
            
            $stmt = $db->prepare("SELECT bh_id FROM boarding_houses ORDER BY bh_id DESC LIMIT 2");
            $stmt->execute();
            $boarding_houses = $stmt->fetchAll(PDO::FETCH_COLUMN);
            echo "✅ Created sample boarding houses\n";
        }
        
        // Create sample room categories and units if needed
        if (count($rooms) == 0) {
            $stmt = $db->prepare("
                INSERT IGNORE INTO boarding_house_rooms (bh_id, room_category, room_price, room_description, room_capacity, room_amenities) 
                VALUES 
                ({$boarding_houses[0]}, 'Single Room', 5000.00, 'Private single room with basic amenities', 1, 'Bed, Desk, Chair, Wardrobe'),
                ({$boarding_houses[0]}, 'Double Room', 3500.00, 'Shared double room with basic amenities', 2, '2 Beds, 2 Desks, 2 Chairs, Wardrobe'),
                ({$boarding_houses[1]}, 'Triple Room', 2500.00, 'Shared triple room with basic amenities', 3, '3 Beds, 3 Desks, 3 Chairs, Wardrobe')
            ");
            $stmt->execute();
            
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
            
            $stmt = $db->prepare("SELECT room_id FROM room_units ORDER BY room_id DESC LIMIT 3");
            $stmt->execute();
            $rooms = $stmt->fetchAll(PDO::FETCH_COLUMN);
            echo "✅ Created sample room categories and units\n";
        }
    }
    
    // Create sample active boarders with proper relationships
    echo "📋 Creating active boarders with room and boarding house assignments...\n";
    
    $stmt = $db->prepare("
        INSERT IGNORE INTO active_boarders (user_id, room_id, boarding_house_id, status) 
        VALUES 
        ({$users[0]}, {$rooms[0]}, {$boarding_houses[0]}, 'Active'),
        ({$users[1]}, {$rooms[1]}, {$boarding_houses[0]}, 'Active'),
        ({$users[2]}, {$rooms[2]}, {$boarding_houses[1]}, 'Active')
    ");
    $stmt->execute();
    
    echo "✅ Created active boarders with proper relationships\n";
    
    // Create sample bills
    echo "📋 Creating sample bills...\n";
    
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
    echo "   - Users: " . count($users) . "\n";
    echo "   - Boarding houses: " . count($boarding_houses) . "\n";
    echo "   - Rooms: " . count($rooms) . "\n\n";
    
    echo "✅ Payment system is now ready for testing!\n";
    
} catch (Exception $e) {
    // Rollback transaction on error
    $db->rollback();
    echo "❌ Error creating sample data: " . $e->getMessage() . "\n";
}
?>




