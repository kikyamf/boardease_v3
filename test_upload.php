<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Test endpoint to check if upload is working
$user_id = $_POST["user_id"] ?? null;
$test_data = $_POST["test_data"] ?? null;

echo json_encode([
    "success" => true,
    "message" => "Test endpoint working",
    "user_id" => $user_id,
    "test_data_length" => $test_data ? strlen($test_data) : 0,
    "post_size" => strlen(file_get_contents('php://input')),
    "memory_usage" => memory_get_usage(true) / 1024 / 1024 . " MB"
]);
?>
































