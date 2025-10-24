<?php
// BoardEase Messaging System Configuration

// Database Configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'boardease2'); // Update with your database name
define('DB_USER', 'root'); // Update with your database username
define('DB_PASS', ''); // Update with your database password
define('DB_CHARSET', 'utf8mb4');

// Firebase Configuration
define('FIREBASE_PROJECT_ID', 'boardease2'); // Update with your Firebase project ID
define('SERVICE_ACCOUNT_PATH', 'path/to/your/service-account-key.json'); // Update with your service account key path

// API Configuration
define('API_BASE_URL', 'http://192.168.101.6/BoardEase2/'); // Update with your server URL
define('API_VERSION', 'v1');

// Message Configuration
define('MESSAGE_LIMIT', 50); // Default number of messages to fetch
define('MAX_MESSAGE_LENGTH', 1000); // Maximum message length
define('MAX_GROUP_MEMBERS', 50); // Maximum number of group members

// Notification Configuration
define('NOTIFICATION_SOUND', 'default');
define('NOTIFICATION_PRIORITY', 'high');
define('NOTIFICATION_VISIBILITY', 'public');

// Logging Configuration
define('LOG_MESSAGES', true);
define('LOG_NOTIFICATIONS', true);
define('LOG_DIR', __DIR__ . '/logs/');

// Security Configuration
define('ALLOWED_ORIGINS', [
    'http://localhost',
    'http://192.168.101.6',
    'https://yourdomain.com' // Add your production domain
]);

// Error Reporting (set to false in production)
define('DEBUG_MODE', true);

// CORS Headers
function setCORSHeaders() {
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    
    if (in_array($origin, ALLOWED_ORIGINS) || DEBUG_MODE) {
        header("Access-Control-Allow-Origin: $origin");
    }
    
    header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    header('Access-Control-Allow-Credentials: true');
    
    // Handle preflight requests
    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(200);
        exit();
    }
}

// Set CORS headers
setCORSHeaders();

// Error handling
if (DEBUG_MODE) {
    error_reporting(E_ALL);
    ini_set('display_errors', 1);
} else {
    error_reporting(0);
    ini_set('display_errors', 0);
}

// Timezone
date_default_timezone_set('Asia/Manila'); // Update with your timezone

// Create logs directory if it doesn't exist
if (!file_exists(LOG_DIR)) {
    mkdir(LOG_DIR, 0755, true);
}
?>











