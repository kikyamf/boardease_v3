<?php
/**
 * get_payment_proof.php
 * 
 * This script serves as a proxy to serve payment proof images.
 * It handles both 'path' (from Android app) and 'booking_id' (for fallback) parameters.
 * It returns the actual image bytes instead of JSON, allowing libraries like Glide to load it directly.
 */

// Error reporting off for production to avoid breaking binary output
error_reporting(0);
ini_set('display_errors', 0);

// Debug logging function
function debug_log($message) {
    file_put_contents(__DIR__ . '/payment_proof_debug.log', date('[Y-m-d H:i:s] ') . $message . "\n", FILE_APPEND);
}

debug_log("Request received: " . $_SERVER['REQUEST_URI']);
debug_log("GET params: " . json_encode($_GET));

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

header('Access-Control-Allow-Origin: *');

// Database configuration (from user provided snippet)
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

$filePath = '';

// Check for 'path' parameter (primary use case for Android)
if (isset($_GET['path']) && !empty($_GET['path'])) {
    $requestedPath = $_GET['path'];
    
    // Security: Prevent directory traversal
    // We expect paths like 'uploads/payment_proofs/file.jpg'
    $filename = basename($requestedPath);
    // User specified path is outside public_html: /home/u223444398/domains/boardease.calapebohol.com/uploads/payment_proofs/
    // If this file is in public_html, we need to go one level up
    $filePath = dirname(__DIR__) . '/uploads/payment_proofs/' . $filename;
} 
// Check for 'booking_id' parameter (fallback use case)
elseif (isset($_GET['booking_id']) && intval($_GET['booking_id']) > 0) {
    $bookingId = intval($_GET['booking_id']);
    
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        // Fetch the latest payment proof path for this booking
        $sql = "SELECT payment_proof FROM payments 
                WHERE booking_id = :booking_id 
                ORDER BY payment_id DESC LIMIT 1";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':booking_id' => $bookingId]);
        $payment = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($payment && !empty($payment['payment_proof'])) {
            $filename = basename($payment['payment_proof']);
            $filePath = dirname(__DIR__) . '/uploads/payment_proofs/' . $filename;
        }
    } catch (PDOException $e) {
        // Silently fail as we are serving an image
    }
}

// Serve the file if found
if (!empty($filePath)) {
    debug_log("Final calculated filePath: " . $filePath);
    if (file_exists($filePath) && is_file($filePath)) {
        debug_log("File EXISTS. Serving bytes.");
        // Detect MIME type
        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        $mimeType = finfo_file($finfo, $filePath);
        finfo_close($finfo);

        // If MIME type detection failed, fallback to JPEG
        if (!$mimeType) {
            $mimeType = 'image/jpeg';
        }

        header('Content-Type: ' . $mimeType);
        header('Content-Length: ' . filesize($filePath));
        
        // Clear any output buffer before sending binary data
        if (ob_get_length()) ob_clean();
        flush();
        
        readfile($filePath);
        exit;
    } else {
        debug_log("File NOT FOUND at: " . $filePath);
    }
}

// Return 404 if file not found
debug_log("Returning 404 Not Found");
header("HTTP/1.0 404 Not Found");
exit;
