<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Database configuration
$servername = "localhost";
$username = "boardease";
$password = "boardease";
$database = "boardease2";

$conn = new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
    die(json_encode(["success" => false, "error" => "Connection failed: " . $conn->connect_error]));
}

// Get user_id from POST request
$user_id = $_POST["user_id"] ?? null;

if (!$user_id) {
    echo json_encode(["success" => false, "error" => "User ID is required"]);
    exit;
}

try {
    // Query to get owner profile data from registrations and users tables
    $sql = "SELECT r.first_name, r.middle_name, r.last_name, r.birth_date, r.phone, r.address, r.email, u.profile_picture 
            FROM users u 
            JOIN registrations r ON u.reg_id = r.id 
            WHERE u.user_id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        echo json_encode([
            "success" => true,
            "f_name" => $row["first_name"] ?? "",
            "m_name" => $row["middle_name"] ?? "",
            "l_name" => $row["last_name"] ?? "",
            "birthdate" => $row["birth_date"] ?? "",
            "phone_number" => $row["phone"] ?? "",
            "p_address" => $row["address"] ?? "",
            "email" => $row["email"] ?? "",
            "profile_picture" => $row["profile_picture"] ?? ""
        ]);
    } else {
        echo json_encode(["success" => false, "error" => "Owner profile not found"]);
    }

} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => "Database error: " . $e->getMessage()]);
}

$conn->close();
?>
















