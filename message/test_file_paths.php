<?php
// Test file paths from message folder perspective
header('Content-Type: application/json');

$files_to_check = [
    '../db_helper.php' => 'Parent folder (from message/)',
    '../fcm_config.php' => 'Parent folder (from message/)',
    '../firebase-service-account.json' => 'Parent folder (from message/)'
];

$results = [];

foreach ($files_to_check as $file => $description) {
    $results[$file] = [
        'description' => $description,
        'exists' => file_exists($file),
        'path' => realpath($file) ?: 'Not found'
    ];
}

$response = [
    'success' => true,
    'data' => [
        'current_directory' => getcwd(),
        'file_checks' => $results,
        'analysis' => [
            'all_files_found' => !in_array(false, array_column($results, 'exists'))
        ]
    ]
];

echo json_encode($response, JSON_PRETTY_PRINT);
?>























