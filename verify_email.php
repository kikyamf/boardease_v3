<?php
// Email Verification Handler for BoardEase
// This file handles email verification when users click the verification link

require_once 'dbConfig.php';

// Check if token is provided
if (!isset($_GET['token']) || empty($_GET['token'])) {
    $error = "Invalid verification link.";
    include 'verification_result.php';
    exit;
}

$token = $_GET['token'];

try {
    // Find user with this verification token
    $sql = "SELECT user_id, first_name, last_name, email, verification_token_expires, email_verified 
            FROM users 
            WHERE verification_token = ? AND status = 'pending_approval'";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $token);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        $error = "Invalid or expired verification link.";
        include 'verification_result.php';
        exit;
    }
    
    $user = $result->fetch_assoc();
    
    // Check if token has expired
    $now = new DateTime();
    $expires = new DateTime($user['verification_token_expires']);
    
    if ($now > $expires) {
        $error = "Verification link has expired. Please request a new one.";
        include 'verification_result.php';
        exit;
    }
    
    // Check if already verified
    if ($user['email_verified'] == 1) {
        $success = "Your email has already been verified. Your account is pending admin approval.";
        include 'verification_result.php';
        exit;
    }
    
    // Update email verification status
    $updateSql = "UPDATE users SET email_verified = 1, verification_token = NULL, verification_token_expires = NULL WHERE user_id = ?";
    $updateStmt = $conn->prepare($updateSql);
    $updateStmt->bind_param("i", $user['user_id']);
    
    if ($updateStmt->execute()) {
        $success = "Your email has been verified successfully! Your account is now pending admin approval. You will receive an email once your account is approved.";
        
        // Log the verification
        $logSql = "INSERT INTO user_logs (user_id, action, details, created_at) VALUES (?, 'email_verified', ?, NOW())";
        $logStmt = $conn->prepare($logSql);
        $details = "Email verified for user: " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['email'] . ")";
        $logStmt->bind_param("is", $user['user_id'], $details);
        $logStmt->execute();
        
    } else {
        $error = "Failed to verify email. Please try again or contact support.";
    }
    
} catch (Exception $e) {
    $error = "An error occurred during verification. Please try again.";
    error_log("Email verification error: " . $e->getMessage());
}

include 'verification_result.php';
$conn->close();
?>


























