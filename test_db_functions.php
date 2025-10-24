<?php
// Test to see what functions are available in dbConfig.php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');

try {
    // Check if dbConfig.php exists
    if (!file_exists('dbConfig.php')) {
        throw new Exception('dbConfig.php not found in main folder');
    }
    
    // Include the database connection
    require_once 'dbConfig.php';
    
    // Get all defined functions
    $functions = get_defined_functions();
    $user_functions = $functions['user'];
    
    // Look for database-related functions
    $db_functions = array_filter($user_functions, function($func) {
        return strpos(strtolower($func), 'db') !== false || 
               strpos(strtolower($func), 'connect') !== false ||
               strpos(strtolower($func), 'pdo') !== false;
    });
    
    $response = [
        'success' => true,
        'message' => 'dbConfig.php loaded successfully',
        'data' => [
            'all_user_functions' => $user_functions,
            'db_related_functions' => array_values($db_functions),
            'total_functions' => count($user_functions)
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error loading dbConfig.php',
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>























