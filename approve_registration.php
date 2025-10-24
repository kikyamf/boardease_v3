<?php
// approve_registration.php

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

    // Get POST data
    $registrationId = $_POST['registration_id'] ?? null;
    $action = $_POST['action'] ?? null; // 'approve' or 'reject'

    if (!$registrationId || !$action) {
        throw new Exception("Missing required parameters");
    }

    if (!in_array($action, ['approve', 'reject'])) {
        throw new Exception("Invalid action. Must be 'approve' or 'reject'");
    }

    // Start transaction
    $conn->begin_transaction();

    try {
        // Get registration details
        $sql = "SELECT * FROM registrations WHERE id = ? AND status = 'pending'";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $registrationId);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows === 0) {
            throw new Exception("Registration not found or already processed");
        }
        
        $registration = $result->fetch_assoc();
        $stmt->close();

        if ($action === 'approve') {
            // Update registration status to approved
            $sql = "UPDATE registrations SET status = 'approved', approved_at = NOW() WHERE id = ?";
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("i", $registrationId);
            $stmt->execute();
            $stmt->close();

            // Create user account in users table with correct structure
            $sql = "INSERT INTO users (reg_id, user_id, profile_picture, status) 
                    VALUES (?, ?, ?, 'active')";
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("iss", 
                $registrationId, 
                $registration['email'], // user_id is the email
                null // profile_picture is null initially
            );
            $stmt->execute();
            $stmt->close();

            $message = "Registration approved successfully";
        } else {
            // Update registration status to rejected
            $sql = "UPDATE registrations SET status = 'rejected', rejected_at = NOW() WHERE id = ?";
            $stmt = $conn->prepare($sql);
            $stmt->bind_param("i", $registrationId);
            $stmt->execute();
            $stmt->close();

            $message = "Registration rejected successfully";
        }

        // Commit transaction
        $conn->commit();

        $response = array(
            "success" => true,
            "message" => $message,
            "action" => $action,
            "registration_id" => $registrationId
        );

        error_log("Registration $action: ID $registrationId, Email: " . $registration['email']);

    } catch (Exception $e) {
        // Rollback transaction on error
        $conn->rollback();
        throw $e;
    }

    echo json_encode($response);

} catch (Exception $e) {
    error_log("Approve registration error: " . $e->getMessage());
    $response = array(
        "success" => false,
        "message" => "Error processing registration: " . $e->getMessage()
    );
    echo json_encode($response);
}

$conn->close();
?>
