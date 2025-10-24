<?php
// Ultra simple test with proper response format
header('Content-Type: application/json');
echo json_encode([
    'success' => true, 
    'message' => 'Ultra simple test works',
    'data' => [
        'group_id' => 999,
        'group_name' => 'Test Group',
        'created_by' => 29,
        'member_count' => 3,
        'members' => [6, 27, 24]
    ]
]);
exit;
?>
