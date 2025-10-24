<?php
// Check file paths for messaging files
header('Content-Type: application/json');

$files_to_check = [
    'db_helper.php' => 'Main folder',
    '../db_helper.php' => 'Parent folder (from message/)',
    'fcm_config.php' => 'Main folder', 
    '../fcm_config.php' => 'Parent folder (from message/)',
    'firebase-service-account.json' => 'Main folder',
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
            'message_folder_files' => [
                'db_helper_exists' => $results['../db_helper.php']['exists'],
                'fcm_config_exists' => $results['../fcm_config.php']['exists'],
                'service_account_exists' => $results['../firebase-service-account.json']['exists']
            ]
        ]
    ]
];

echo json_encode($response, JSON_PRETTY_PRINT);
?>























