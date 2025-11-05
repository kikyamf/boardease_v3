<?php
// Simple test script to verify get_boarder_info.php is accessible
header('Content-Type: application/json');

$testUrl = 'https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarder_info.php';

// Test with cURL
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $testUrl);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query(['user_id' => 1]));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'ngrok-skip-browser-warning: any'
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

echo json_encode([
    'test_url' => $testUrl,
    'http_code' => $httpCode,
    'response' => $response,
    'error' => $error ?: 'None'
]);
?>

