<?php
// Test the API endpoint via HTTP request
$url = "http://192.168.101.6/BoardEase2/get_unread_notif_count.php?user_id=1";

echo "Testing API endpoint: $url\n\n";

$context = stream_context_create([
    'http' => [
        'method' => 'GET',
        'header' => 'Content-Type: application/json',
        'timeout' => 10
    ]
]);

$response = file_get_contents($url, false, $context);

if ($response === FALSE) {
    echo "Error: Failed to fetch API response\n";
    echo "HTTP error: " . $http_response_header[0] . "\n";
} else {
    echo "API Response:\n";
    echo $response . "\n";
    
    // Try to decode JSON
    $data = json_decode($response, true);
    if ($data) {
        echo "\nDecoded JSON:\n";
        print_r($data);
    } else {
        echo "\nError: Invalid JSON response\n";
    }
}
?>





















