<?php
require_once 'db_helper.php';

try {
    $db = getDB();
    
    // Simple check
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "Users count: " . $result['count'] . "\n";
    
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM registrations");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "Registrations count: " . $result['count'] . "\n";
    
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM boarding_houses");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "Boarding houses count: " . $result['count'] . "\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




