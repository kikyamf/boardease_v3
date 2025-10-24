<?php
// Clear old device tokens to force refresh
require_once 'db_helper.php';

try {
    $db = getDB();
    
    // Clear all old device tokens
    $stmt = $db->prepare("DELETE FROM device_tokens");
    $stmt->execute();
    
    $deletedCount = $stmt->rowCount();
    
    echo json_encode([
        'success' => true,
        'message' => "Cleared $deletedCount old device tokens. Users need to open the app to register new tokens.",
        'deleted_count' => $deletedCount
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>








