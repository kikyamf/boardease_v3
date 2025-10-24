<?php
require_once 'db_connection.php';

// Get user_id from POST request
$userId = $_POST['user_id'] ?? null;

if (!$userId) {
    sendResponse(false, 'User ID is required');
}

// Validate user
$user = validateUser($pdo, $userId);
if (!$user) {
    sendResponse(false, 'Invalid user');
}

try {
    // Update last seen timestamp
    $updateQuery = "
        UPDATE users 
        SET last_seen = NOW() 
        WHERE user_id = :user_id
    ";

    $stmt = $pdo->prepare($updateQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();

    sendResponse(true, 'Last seen updated successfully');

} catch (Exception $e) {
    sendResponse(false, 'Error updating last seen: ' . $e->getMessage());
}
?>

























