<?php
// Simple test file to check if PHP is working and directory is accessible
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

echo json_encode(array(
    'success' => true,
    'message' => 'PHP is working!',
    'server_time' => date('Y-m-d H:i:s'),
    'php_version' => phpversion(),
    'method' => $_SERVER['REQUEST_METHOD'],
    'post_data' => $_POST,
    'get_data' => $_GET
));
?>

