<?php
require_once 'db_connection.php';

// Get parameters from POST request
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
    // Get active device tokens for user
    $tokensQuery = "
        SELECT device_token 
        FROM device_tokens 
        WHERE user_id = :user_id 
        AND is_active = 1 
        AND last_used > DATE_SUB(NOW(), INTERVAL 30 DAY)
        ORDER BY last_used DESC
    ";

    $stmt = $pdo->prepare($tokensQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);

    sendResponse(true, 'Device tokens retrieved successfully', $tokens);

} catch (Exception $e) {
    sendResponse(false, 'Error retrieving device tokens: ' . $e->getMessage());
}
?>

























