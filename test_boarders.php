<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Simple test response
echo json_encode([
    "success" => true,
    "message" => "Test endpoint working",
    "boarders" => [],
    "total_count" => 0,
    "debug_info" => [
        "user_id" => $_POST["user_id"] ?? "not_provided",
        "timestamp" => date("Y-m-d H:i:s")
    ]
], JSON_UNESCAPED_SLASHES);
?>































