<?php
// Simple test to verify get_boarder_info.php is accessible
header('Content-Type: text/plain');

echo "Testing get_boarder_info.php access...\n\n";

// Test if file exists
$filePath = __DIR__ . '/get_boarder_info.php';
echo "File path: $filePath\n";
echo "File exists: " . (file_exists($filePath) ? 'YES' : 'NO') . "\n";
echo "File readable: " . (is_readable($filePath) ? 'YES' : 'NO') . "\n\n";

// Try to include and check for syntax errors
$syntaxCheck = shell_exec('php -l ' . escapeshellarg($filePath) . ' 2>&1');
echo "PHP Syntax Check:\n$syntaxCheck\n\n";

// Test database connection
$servername = "localhost";
$username   = "boardease";
$password   = "boardease";
$dbname     = "boardease2";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    echo "Database connection: FAILED - " . $conn->connect_error . "\n";
} else {
    echo "Database connection: SUCCESS\n";
    $conn->close();
}
?>

