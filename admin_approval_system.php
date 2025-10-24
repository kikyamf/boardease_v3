<?php
// Admin Approval System for BoardEase
// This file handles user approval, rejection, and email verification

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Include database configuration
require_once 'dbConfig.php';

class AdminApprovalSystem {
    private $conn;
    
    public function __construct($connection) {
        $this->conn = $connection;
    }
    
    // Get pending user approvals
    public function getPendingApprovals() {
        $sql = "SELECT 
                    u.user_id,
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.phone,
                    u.user_type,
                    u.registration_date,
                    u.email_verified,
                    u.status,
                    r.business_name,
                    r.university,
                    r.student_id
                FROM users u
                LEFT JOIN registration r ON u.user_id = r.user_id
                WHERE u.status = 'pending_approval'
                ORDER BY u.registration_date ASC";
        
        $result = $this->conn->query($sql);
        
        if ($result && $result->num_rows > 0) {
            $approvals = [];
            while ($row = $result->fetch_assoc()) {
                $approvals[] = [
                    'user_id' => $row['user_id'],
                    'name' => $row['first_name'] . ' ' . $row['last_name'],
                    'email' => $row['email'],
                    'phone' => $row['phone'],
                    'user_type' => $row['user_type'],
                    'registration_date' => $row['registration_date'],
                    'email_verified' => $row['email_verified'],
                    'business_name' => $row['business_name'],
                    'university' => $row['university'],
                    'student_id' => $row['student_id']
                ];
            }
            return ['success' => true, 'approvals' => $approvals];
        }
        
        return ['success' => true, 'approvals' => []];
    }
    
    // Approve a user
    public function approveUser($userId, $adminId) {
        $this->conn->begin_transaction();
        
        try {
            // Update user status to active
            $sql = "UPDATE users SET status = 'active', approved_by = ?, approved_date = NOW() WHERE user_id = ?";
            $stmt = $this->conn->prepare($sql);
            $stmt->bind_param("ii", $adminId, $userId);
            
            if (!$stmt->execute()) {
                throw new Exception("Failed to update user status");
            }
            
            // Get user details for email
            $userSql = "SELECT first_name, last_name, email, user_type FROM users WHERE user_id = ?";
            $userStmt = $this->conn->prepare($userSql);
            $userStmt->bind_param("i", $userId);
            $userStmt->execute();
            $userResult = $userStmt->get_result();
            $user = $userResult->fetch_assoc();
            
            // Send approval email
            $emailSent = $this->sendApprovalEmail($user['email'], $user['first_name'], $user['user_type']);
            
            // Log the approval
            $logSql = "INSERT INTO admin_logs (admin_id, action, target_user_id, details, created_at) VALUES (?, 'user_approved', ?, ?, NOW())";
            $logStmt = $this->conn->prepare($logSql);
            $details = "User approved: " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['email'] . ")";
            $logStmt->bind_param("iis", $adminId, $userId, $details);
            $logStmt->execute();
            
            $this->conn->commit();
            
            return [
                'success' => true, 
                'message' => 'User approved successfully',
                'email_sent' => $emailSent
            ];
            
        } catch (Exception $e) {
            $this->conn->rollback();
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }
    
    // Reject a user
    public function rejectUser($userId, $reason, $adminId) {
        $this->conn->begin_transaction();
        
        try {
            // Update user status to rejected
            $sql = "UPDATE users SET status = 'rejected', rejection_reason = ?, rejected_by = ?, rejected_date = NOW() WHERE user_id = ?";
            $stmt = $this->conn->prepare($sql);
            $stmt->bind_param("sii", $reason, $adminId, $userId);
            
            if (!$stmt->execute()) {
                throw new Exception("Failed to update user status");
            }
            
            // Get user details for email
            $userSql = "SELECT first_name, last_name, email, user_type FROM users WHERE user_id = ?";
            $userStmt = $this->conn->prepare($userSql);
            $userStmt->bind_param("i", $userId);
            $userStmt->execute();
            $userResult = $userStmt->get_result();
            $user = $userResult->fetch_assoc();
            
            // Send rejection email
            $emailSent = $this->sendRejectionEmail($user['email'], $user['first_name'], $reason);
            
            // Log the rejection
            $logSql = "INSERT INTO admin_logs (admin_id, action, target_user_id, details, created_at) VALUES (?, 'user_rejected', ?, ?, NOW())";
            $logStmt = $this->conn->prepare($logSql);
            $details = "User rejected: " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['email'] . ") - Reason: " . $reason;
            $logStmt->bind_param("iis", $adminId, $userId, $details);
            $logStmt->execute();
            
            $this->conn->commit();
            
            return [
                'success' => true, 
                'message' => 'User rejected successfully',
                'email_sent' => $emailSent
            ];
            
        } catch (Exception $e) {
            $this->conn->rollback();
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }
    
    // Resend verification email
    public function resendVerificationEmail($userId) {
        try {
            // Get user details
            $sql = "SELECT first_name, last_name, email, user_type FROM users WHERE user_id = ?";
            $stmt = $this->conn->prepare($sql);
            $stmt->bind_param("i", $userId);
            $stmt->execute();
            $result = $stmt->get_result();
            $user = $result->fetch_assoc();
            
            if (!$user) {
                return ['success' => false, 'message' => 'User not found'];
            }
            
            // Generate new verification token
            $verificationToken = bin2hex(random_bytes(32));
            
            // Update verification token
            $updateSql = "UPDATE users SET verification_token = ?, verification_token_expires = DATE_ADD(NOW(), INTERVAL 24 HOUR) WHERE user_id = ?";
            $updateStmt = $this->conn->prepare($updateSql);
            $updateStmt->bind_param("si", $verificationToken, $userId);
            $updateStmt->execute();
            
            // Send verification email
            $emailSent = $this->sendVerificationEmail($user['email'], $user['first_name'], $verificationToken);
            
            return [
                'success' => true, 
                'message' => 'Verification email resent successfully',
                'email_sent' => $emailSent
            ];
            
        } catch (Exception $e) {
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }
    
    // Send approval email
    private function sendApprovalEmail($email, $firstName, $userType) {
        $subject = "Welcome to BoardEase - Your Account Has Been Approved!";
        
        $message = "
        <html>
        <head>
            <title>Account Approved</title>
        </head>
        <body>
            <h2>Congratulations, $firstName!</h2>
            <p>Your BoardEase account has been approved by our admin team.</p>
            <p><strong>Account Type:</strong> " . ucfirst($userType) . "</p>
            <p>You can now log in to your account and start using BoardEase.</p>
            <br>
            <p>If you have any questions, please contact our support team.</p>
            <br>
            <p>Best regards,<br>BoardEase Team</p>
        </body>
        </html>
        ";
        
        return $this->sendEmail($email, $subject, $message);
    }
    
    // Send rejection email
    private function sendRejectionEmail($email, $firstName, $reason) {
        $subject = "BoardEase Account Application - Update";
        
        $message = "
        <html>
        <head>
            <title>Account Application Update</title>
        </head>
        <body>
            <h2>Hello $firstName,</h2>
            <p>Thank you for your interest in BoardEase.</p>
            <p>After reviewing your application, we regret to inform you that your account request has not been approved at this time.</p>
            <p><strong>Reason:</strong> $reason</p>
            <p>If you believe this decision was made in error or if you have additional information to provide, please contact our support team.</p>
            <br>
            <p>Thank you for your understanding.</p>
            <br>
            <p>Best regards,<br>BoardEase Team</p>
        </body>
        </html>
        ";
        
        return $this->sendEmail($email, $subject, $message);
    }
    
    // Send verification email
    private function sendVerificationEmail($email, $firstName, $token) {
        $subject = "Verify Your BoardEase Account";
        $verificationLink = "http://192.168.101.6/BoardEase2/verify_email.php?token=" . $token;
        
        $message = "
        <html>
        <head>
            <title>Email Verification</title>
        </head>
        <body>
            <h2>Hello $firstName,</h2>
            <p>Please verify your email address to complete your BoardEase registration.</p>
            <p>Click the link below to verify your account:</p>
            <p><a href='$verificationLink' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Verify Email Address</a></p>
            <p>This link will expire in 24 hours.</p>
            <br>
            <p>If you didn't create an account with BoardEase, please ignore this email.</p>
            <br>
            <p>Best regards,<br>BoardEase Team</p>
        </body>
        </html>
        ";
        
        return $this->sendEmail($email, $subject, $message);
    }
    
    // Send email using PHP mail function (you can replace with SMTP)
    private function sendEmail($to, $subject, $message) {
        $headers = "MIME-Version: 1.0" . "\r\n";
        $headers .= "Content-type:text/html;charset=UTF-8" . "\r\n";
        $headers .= "From: noreply@boardease.com" . "\r\n";
        
        return mail($to, $subject, $message, $headers);
    }
}

// Handle API requests
$method = $_SERVER['REQUEST_METHOD'];
$input = json_decode(file_get_contents('php://input'), true);

$approvalSystem = new AdminApprovalSystem($conn);

switch ($method) {
    case 'GET':
        if (isset($_GET['action']) && $_GET['action'] === 'pending_approvals') {
            echo json_encode($approvalSystem->getPendingApprovals());
        }
        break;
        
    case 'POST':
        if (isset($input['action'])) {
            switch ($input['action']) {
                case 'approve':
                    if (isset($input['userId']) && isset($input['adminId'])) {
                        echo json_encode($approvalSystem->approveUser($input['userId'], $input['adminId']));
                    } else {
                        echo json_encode(['success' => false, 'message' => 'Missing required parameters']);
                    }
                    break;
                    
                case 'reject':
                    if (isset($input['userId']) && isset($input['reason']) && isset($input['adminId'])) {
                        echo json_encode($approvalSystem->rejectUser($input['userId'], $input['reason'], $input['adminId']));
                    } else {
                        echo json_encode(['success' => false, 'message' => 'Missing required parameters']);
                    }
                    break;
                    
                case 'resend_verification':
                    if (isset($input['userId'])) {
                        echo json_encode($approvalSystem->resendVerificationEmail($input['userId']));
                    } else {
                        echo json_encode(['success' => false, 'message' => 'Missing user ID']);
                    }
                    break;
                    
                default:
                    echo json_encode(['success' => false, 'message' => 'Invalid action']);
            }
        } else {
            echo json_encode(['success' => false, 'message' => 'No action specified']);
        }
        break;
        
    default:
        echo json_encode(['success' => false, 'message' => 'Method not allowed']);
}

$conn->close();
?>


























