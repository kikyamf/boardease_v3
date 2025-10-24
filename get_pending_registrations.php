<?php
// get_pending_registrations.php

// Disable error display to prevent HTML output
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Set content type to JSON
header('Content-Type: application/json');

try {
    // Database connection
    $servername = "localhost";
    $username   = "boardease";
    $password   = "boardease";
    $dbname     = "boardease2";

    $conn = new mysqli($servername, $username, $password, $dbname);

    if ($conn->connect_error) {
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }

    // Get pending registrations
    $sql = "SELECT id, role, first_name, middle_name, last_name, birth_date, phone, address, email, 
                   gcash_num, valid_id_type, id_number, idFrontFile, idBackFile, gcash_qr, 
                   status, created_at
            FROM registrations 
            WHERE status = 'pending' 
            ORDER BY created_at DESC";

    $result = $conn->query($sql);

    if (!$result) {
        throw new Exception("Query failed: " . $conn->error);
    }

    $registrations = array();
    while ($row = $result->fetch_assoc()) {
        $registrations[] = array(
            "id" => $row['id'],
            "role" => $row['role'],
            "first_name" => $row['first_name'],
            "middle_name" => $row['middle_name'],
            "last_name" => $row['last_name'],
            "full_name" => trim($row['first_name'] . ' ' . $row['middle_name'] . ' ' . $row['last_name']),
            "birth_date" => $row['birth_date'],
            "phone" => $row['phone'],
            "address" => $row['address'],
            "email" => $row['email'],
            "gcash_num" => $row['gcash_num'],
            "valid_id_type" => $row['valid_id_type'],
            "id_number" => $row['id_number'],
            "id_front_file" => $row['idFrontFile'],
            "id_back_file" => $row['idBackFile'],
            "gcash_qr" => $row['gcash_qr'],
            "status" => $row['status'],
            "created_at" => $row['created_at']
        );
    }

    $response = array(
        "success" => true,
        "data" => $registrations,
        "count" => count($registrations)
    );

    echo json_encode($response);

} catch (Exception $e) {
    error_log("Get pending registrations error: " . $e->getMessage());
    $response = array(
        "success" => false,
        "message" => "Error retrieving pending registrations: " . $e->getMessage()
    );
    echo json_encode($response);
}

$conn->close();
?>










