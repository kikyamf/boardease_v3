<?php
// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept");

// Database configuration
$servername = "localhost";
$username = "boardease";
$password = "boardease";
$database = "boardease2";

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die(json_encode(["success" => false, "error" => "Connection failed: " . $conn->connect_error]));
}

// Get JSON input
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);

// If JSON decode failed, try to get from POST
if ($input === null && !empty($_POST)) {
    $input = $_POST;
}

// Get parameters from JSON or POST request
$user_id = $input["user_id"] ?? $_POST["user_id"] ?? null;
$room_id = isset($input["room_id"]) ? ($input["room_id"] === null ? null : intval($input["room_id"])) : (isset($_POST["room_id"]) ? intval($_POST["room_id"]) : null);
$subject = $input["subject"] ?? $_POST["subject"] ?? null;
$area_for_maintenance = $input["area_for_maintenance"] ?? $_POST["area_for_maintenance"] ?? null;
$description = $input["description"] ?? $_POST["description"] ?? $_POST["mr_description"] ?? null;

// Validate required fields
if (!$user_id) {
    echo json_encode(["success" => false, "error" => "User ID is required"]);
    exit;
}

if (!$subject || trim($subject) === '') {
    echo json_encode(["success" => false, "error" => "Subject is required"]);
    exit;
}

if (!$area_for_maintenance || trim($area_for_maintenance) === '') {
    echo json_encode(["success" => false, "error" => "Area for maintenance is required"]);
    exit;
}

if (!$description || trim($description) === '') {
    echo json_encode(["success" => false, "error" => "Description is required"]);
    exit;
}

// Validate area_for_maintenance value
$valid_areas = ['BH Room', 'Bathroom', 'Kitchen', 'Others'];
if (!in_array($area_for_maintenance, $valid_areas)) {
    echo json_encode(["success" => false, "error" => "Invalid area for maintenance. Must be one of: " . implode(', ', $valid_areas)]);
    exit;
}

// Check for duplicate request (same user, same subject, same description within last 5 seconds)
// This only prevents rapid double-clicks, not legitimate resubmissions
try {
    $duplicate_check = $conn->prepare("
        SELECT request_id 
        FROM maintenance_requests 
        WHERE user_id = ? 
        AND subject = ? 
        AND mr_description = ? 
        AND mr_created_at > DATE_SUB(NOW(), INTERVAL 5 SECOND)
        LIMIT 1
    ");
    if ($duplicate_check) {
        $duplicate_check->bind_param("iss", $user_id, $subject, $description);
        $duplicate_check->execute();
        $duplicate_result = $duplicate_check->get_result();
        if ($duplicate_result->num_rows > 0) {
            $duplicate_check->close();
            echo json_encode([
                "success" => false,
                "error" => "Please wait a moment before submitting again."
            ]);
            $conn->close();
            exit;
        }
        $duplicate_check->close();
    }
} catch (Exception $dup_ex) {
    // Log but don't block submission if duplicate check fails
    error_log("Warning: Duplicate check failed: " . $dup_ex->getMessage());
}

try {
    // Prepare SQL statement
    // room_id can be NULL, so we handle it conditionally
    if ($room_id && $room_id > 0) {
        $stmt = $conn->prepare("INSERT INTO maintenance_requests (user_id, room_id, subject, area_for_maintenance, mr_description, mr_status) VALUES (?, ?, ?, ?, ?, 'Pending')");
        if (!$stmt) {
            throw new Exception("Prepare failed: " . $conn->error);
        }
        $stmt->bind_param("iisss", $user_id, $room_id, $subject, $area_for_maintenance, $description);
    } else {
        // If room_id is not provided or is 0, set it to NULL
        $stmt = $conn->prepare("INSERT INTO maintenance_requests (user_id, room_id, subject, area_for_maintenance, mr_description, mr_status) VALUES (?, NULL, ?, ?, ?, 'Pending')");
        if (!$stmt) {
            throw new Exception("Prepare failed: " . $conn->error);
        }
        $stmt->bind_param("isss", $user_id, $subject, $area_for_maintenance, $description);
    }
    
    // Execute statement
    if ($stmt->execute()) {
        $request_id = $conn->insert_id;
        
        // Send notification to owner about new maintenance request (ONLY ONCE)
        $notification_sent = false;
        try {
            // Check if activity_notifications.php exists
            $notification_file = __DIR__ . '/activity_notifications.php';
            if (file_exists($notification_file)) {
                require_once $notification_file;
                
                if (class_exists('ActivityNotifications')) {
                    // Get owner_id, boarder_name, and room_name
                    $owner_id = null;
                    $boarder_name = 'A boarder';
                    $room_name = 'a room';
                    
                    if ($room_id && $room_id > 0) {
                        // Get owner from room -> boarding_house
                        $owner_query = "
                            SELECT 
                                bh.user_id as owner_id,
                                COALESCE(bhr.room_name, CONCAT('Room ', ru.room_number)) as room_name
                            FROM room_units ru
                            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                            WHERE ru.room_id = ?
                        ";
                        $owner_stmt = $conn->prepare($owner_query);
                        if ($owner_stmt) {
                            $owner_stmt->bind_param("i", $room_id);
                            $owner_stmt->execute();
                            $owner_result = $owner_stmt->get_result();
                            if ($owner_row = $owner_result->fetch_assoc()) {
                                $owner_id = $owner_row['owner_id'];
                                if (!empty($owner_row['room_name'])) {
                                    $room_name = trim($owner_row['room_name']);
                                }
                            }
                            $owner_stmt->close();
                        }
                    } else {
                        // If no room_id, try to get owner from user's active booking or current room
                        $owner_query = "
                            SELECT 
                                bh.user_id as owner_id,
                                COALESCE(bhr.room_name, CONCAT('Room ', ru.room_number)) as room_name
                            FROM bookings b
                            JOIN room_units ru ON b.room_id = ru.room_id
                            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                            WHERE b.user_id = ? 
                            AND b.booking_status = 'Confirmed'
                            ORDER BY b.booking_date DESC
                            LIMIT 1
                        ";
                        $owner_stmt = $conn->prepare($owner_query);
                        if ($owner_stmt) {
                            $owner_stmt->bind_param("i", $user_id);
                            $owner_stmt->execute();
                            $owner_result = $owner_stmt->get_result();
                            if ($owner_row = $owner_result->fetch_assoc()) {
                                $owner_id = $owner_row['owner_id'];
                                if (!empty($owner_row['room_name'])) {
                                    $room_name = trim($owner_row['room_name']);
                                }
                            }
                            $owner_stmt->close();
                        }
                    }
                    
                    // Get boarder name
                    $name_query = "
                        SELECT CONCAT(
                            COALESCE(r.first_name, ''), ' ', 
                            COALESCE(r.middle_name, ''), ' ', 
                            COALESCE(r.last_name, ''), 
                            CASE WHEN r.suffix IS NOT NULL AND r.suffix != '' THEN CONCAT(' ', r.suffix) ELSE '' END
                        ) as boarder_name
                        FROM users u
                        LEFT JOIN registrations r ON u.reg_id = r.id
                        WHERE u.user_id = ?
                    ";
                    $name_stmt = $conn->prepare($name_query);
                    if ($name_stmt) {
                        $name_stmt->bind_param("i", $user_id);
                        $name_stmt->execute();
                        $name_result = $name_stmt->get_result();
                        if ($name_row = $name_result->fetch_assoc()) {
                            $full_name = trim($name_row['boarder_name']);
                            if (!empty($full_name)) {
                                $boarder_name = $full_name;
                            }
                        }
                        $name_stmt->close();
                    }
                    
                    // Send notification if owner_id is found (ONLY ONCE - prevent duplicates)
                    if ($owner_id && $owner_id > 0 && !$notification_sent) {
                        // Format message directly with boarder name and room name
                        $notification_title = "New Maintenance Request";
                        $notification_message = $boarder_name . " has submitted a maintenance request for " . $room_name . ": " . $subject;
                        
                        // Check if notification already exists for this request (prevent duplicates)
                        $check_stmt = $conn->prepare("SELECT notif_id FROM notifications WHERE user_id = ? AND notif_title = ? AND notif_message = ? AND notif_type = 'maintenance' AND notif_created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE) LIMIT 1");
                        $duplicate_found = false;
                        if ($check_stmt) {
                            $check_stmt->bind_param("iss", $owner_id, $notification_title, $notification_message);
                            $check_stmt->execute();
                            $check_result = $check_stmt->get_result();
                            if ($check_result->num_rows > 0) {
                                $duplicate_found = true;
                                error_log("Duplicate notification prevented for maintenance request (request_id: $request_id)");
                            }
                            $check_stmt->close();
                        }
                        
                        // Only send if not duplicate
                        if (!$duplicate_found) {
                            // Try to use ActivityNotifications if available
                            if (class_exists('ActivityNotifications')) {
                                // Override the template message to ensure boarder name and room name are shown
                                $result = ActivityNotifications::notifyMaintenanceRequest($owner_id, [
                                    'boarder_name' => $boarder_name,
                                    'room_name' => $room_name,
                                    'title' => $subject,
                                    'request_id' => $request_id,
                                    'area' => $area_for_maintenance,
                                    'description' => $description,
                                    'formatted_message' => $notification_message // Pass formatted message directly
                                ]);
                                $notification_sent = ($result && isset($result['success']) && $result['success']);
                            } else {
                                // Fallback: Direct database insert with properly formatted message
                                $notif_stmt = $conn->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, 'maintenance', 'unread')");
                                if ($notif_stmt) {
                                    $notif_stmt->bind_param("iss", $owner_id, $notification_title, $notification_message);
                                    $notification_sent = $notif_stmt->execute();
                                    $notif_stmt->close();
                                    
                                    // Send FCM notification if database insert succeeded
                                    if ($notification_sent) {
                                        // Call FCM send function if available
                                        $fcm_file = __DIR__ . '/send_fcm_notification.php';
                                        if (file_exists($fcm_file)) {
                                            require_once $fcm_file;
                                            if (function_exists('sendFCMNotification')) {
                                                sendFCMNotification($owner_id, $notification_title, $notification_message, ['type' => 'maintenance', 'request_id' => $request_id]);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            error_log("Maintenance notification sent to owner (user_id: $owner_id) - Boarder: $boarder_name, Room: $room_name, Message: $notification_message");
                        }
                    } else {
                        if (!$owner_id || $owner_id <= 0) {
                            error_log("Warning: Could not find owner_id for maintenance request. room_id: " . ($room_id ?? 'NULL') . ", user_id: $user_id");
                        }
                    }
                }
            }
        } catch (Exception $notif_ex) {
            // Don't fail the request if notification fails
            error_log("Warning: Failed to send maintenance request notification: " . $notif_ex->getMessage());
        }
        
        echo json_encode([
            "success" => true,
            "message" => "Maintenance request submitted successfully",
            "request_id" => $request_id
        ]);
    } else {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    $stmt->close();
    
} catch (Exception $e) {
    error_log("Error submitting maintenance request: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "error" => "Failed to submit maintenance request: " . $e->getMessage()
    ]);
} finally {
    $conn->close();
}

?>

