<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Database configuration
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $user_id = isset($_REQUEST['user_id']) ? intval($_REQUEST['user_id']) : 0;

    if ($user_id == 0) {
        echo json_encode(['success' => false, 'message' => 'Missing user_id']);
        exit;
    }

    // Check for pending termination requests
    $termStmt = $pdo->prepare("SELECT COUNT(*) FROM termination_requests WHERE user_id = ? AND status = 'Pending'");
    $termStmt->execute([$user_id]);
    $termCount = $termStmt->fetchColumn();

    // Check for pending change room requests
    $changeStmt = $pdo->prepare("SELECT COUNT(*) FROM change_room_requests WHERE user_id = ? AND status = 'Pending'");
    $changeStmt->execute([$user_id]);
    $changeCount = $changeStmt->fetchColumn();

    $has_pending = ($termCount > 0 || $changeCount > 0);
    
    $pending_type = '';
    if ($termCount > 0 && $changeCount > 0) {
        $pending_type = 'Both';
    } elseif ($termCount > 0) {
        $pending_type = 'Termination';
    } elseif ($changeCount > 0) {
        $pending_type = 'Change Room';
    }

    echo json_encode([
        'success' => true, 
        'has_pending' => $has_pending,
        'pending_type' => $pending_type,
        'termination_count' => intval($termCount),
        'change_room_count' => intval($changeCount),
        'message' => $has_pending ? 'User has pending requests' : 'No pending requests found'
    ]);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
