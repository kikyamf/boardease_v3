<?php
header('Content-Type: text/plain');

$dir = __DIR__ . '/uploads/payment_proofs/';
echo "Checking directory: $dir\n";

if (file_exists($dir)) {
    echo "Directory EXISTS.\n";
    $files = scandir($dir);
    echo "Files found: " . count($files) . "\n";
    foreach ($files as $file) {
        if ($file !== '.' && $file !== '..') {
            echo " - $file\n";
        }
    }
} else {
    echo "Directory NOT FOUND.\n";
    
    // Check root uploads
    $rootUploads = __DIR__ . '/uploads/';
    echo "Checking parent: $rootUploads\n";
    if (file_exists($rootUploads)) {
        echo "Root uploads EXISTS.\n";
        print_r(scandir($rootUploads));
    } else {
        echo "Root uploads NOT FOUND.\n";
    }
}

// Check DB content for a recent booking if possible
// (Using u223444398_boardease as requested in previous session)
try {
    $host = 'localhost';
    $dbname = 'u223444398_boardease';
    $username = 'u223444398_userboardease';
    $password = '!Boardease2026';
    
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $sql = "SELECT booking_id, payment_proof FROM payments WHERE payment_proof IS NOT NULL AND payment_proof != '' ORDER BY payment_id DESC LIMIT 5";
    $stmt = $pdo->query($sql);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "\nLatest entries in payments table:\n";
    foreach ($rows as $row) {
        echo "Booking ID: " . $row['booking_id'] . " - Path: " . $row['payment_proof'] . "\n";
        $fullPath = __DIR__ . '/' . $row['payment_proof'];
        echo "  File exists: " . (file_exists($fullPath) ? "YES" : "NO") . " at $fullPath\n";
    }
} catch (Exception $e) {
    echo "\nDB Check failed: " . $e->getMessage() . "\n";
}
