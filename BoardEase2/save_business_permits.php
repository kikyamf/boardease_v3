<?php
// save_business_permits.php
// Save business permits separately after registration

// Start output buffering
ob_start();

// Disable error display
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Set content type to JSON
header('Content-Type: application/json');

// Log the request
error_log("Business permits save request received at " . date('Y-m-d H:i:s'));
error_log("POST data: " . print_r($_POST, true));
error_log("FILES data: " . print_r($_FILES, true));

try {
    // Database connection (using direct IP/localhost)
    $servername = "localhost";
    $username   = "boardease";
    $password   = "boardease";
    $dbname     = "boardease2";

    // Enable mysqli exception mode
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

    error_log("Attempting database connection - Server: " . $servername . ", Database: " . $dbname);
    $conn = new mysqli($servername, $username, $password, $dbname);

    if ($conn->connect_error) {
        error_log("Database connection failed: " . $conn->connect_error);
        $response = array(
            "success" => false,
            "message" => "Database connection failed. Please try again later."
        );
        ob_clean();
        header('Content-Type: application/json');
        echo json_encode($response);
        exit;
    }

    error_log("Database connection successful!");

    // Get registration ID from POST
    $regId = $_POST['reg_id'] ?? null;
    
    if (!$regId || !is_numeric($regId)) {
        error_log("Invalid or missing reg_id: " . ($regId ?? "null"));
        $response = array(
            "success" => false,
            "message" => "Invalid registration ID."
        );
        ob_clean();
        header('Content-Type: application/json');
        echo json_encode($response);
        $conn->close();
        exit;
    }

    error_log("Processing business permits for registration ID: " . $regId);

    // Verify the registration exists
    $verifySql = "SELECT id, email, status FROM registrations WHERE id = ?";
    $verifyStmt = $conn->prepare($verifySql);
    if (!$verifyStmt) {
        error_log("Failed to prepare verification query: " . $conn->error);
        $response = array(
            "success" => false,
            "message" => "Database error: " . $conn->error
        );
        ob_clean();
        header('Content-Type: application/json');
        echo json_encode($response);
        $conn->close();
        exit;
    }

    $verifyStmt->bind_param("i", $regId);
    $verifyStmt->execute();
    $verifyResult = $verifyStmt->get_result();
    
    if ($verifyResult->num_rows == 0) {
        error_log("Registration ID " . $regId . " not found in database");
        $response = array(
            "success" => false,
            "message" => "Registration not found. Please complete registration first."
        );
        ob_clean();
        header('Content-Type: application/json');
        echo json_encode($response);
        $verifyStmt->close();
        $conn->close();
        exit;
    }

    $verifyRow = $verifyResult->fetch_assoc();
    error_log("Registration found - ID: " . $verifyRow['id'] . ", Email: " . $verifyRow['email'] . ", Status: " . $verifyRow['status']);
    $verifyStmt->close();

    // Handle business permit file uploads
    $permitUploadDir = "../uploads/business_permits/";
    if (!is_dir($permitUploadDir)) {
        mkdir($permitUploadDir, 0777, true);
        error_log("Created business permits directory: " . $permitUploadDir);
    }

    function saveFile($fileKey, $uploadDir) {
        if (!isset($_FILES[$fileKey])) {
            error_log("File key '" . $fileKey . "' not found in FILES array");
            return null;
        }
        
        if ($_FILES[$fileKey]['error'] !== UPLOAD_ERR_OK) {
            error_log("File upload error for '" . $fileKey . "': " . $_FILES[$fileKey]['error']);
            return null;
        }
        
        $fileTmp  = $_FILES[$fileKey]['tmp_name'];
        $fileName = uniqid() . "_" . basename($_FILES[$fileKey]['name']);
        $filePath = $uploadDir . $fileName;
        
        error_log("Attempting to save file: " . $fileKey . " from " . $fileTmp . " to " . $filePath);

        if (move_uploaded_file($fileTmp, $filePath)) {
            error_log("File successfully moved to: " . $filePath);
            if (file_exists($filePath)) {
                error_log("File verified to exist at: " . $filePath . " (size: " . filesize($filePath) . " bytes)");
                return $filePath;
            } else {
                error_log("ERROR: File was moved but does not exist at: " . $filePath);
                return null;
            }
        } else {
            error_log("ERROR: Failed to move uploaded file from " . $fileTmp . " to " . $filePath);
            return null;
        }
    }

    $permitFiles = array();
    for ($i = 1; $i <= 3; $i++) {
        $permitKey = "permitFile" . $i;
        error_log("Checking for permit file key: " . $permitKey);
        if (isset($_FILES[$permitKey])) {
            error_log("Permit file " . $i . " found in FILES array. Error code: " . ($_FILES[$permitKey]['error'] ?? 'not set'));
        } else {
            error_log("Permit file " . $i . " NOT found in FILES array");
        }
        $permitPath = saveFile($permitKey, $permitUploadDir);
        if ($permitPath) {
            $permitFiles[$i] = $permitPath;
            error_log("Successfully saved permit file " . $i . " to: " . $permitPath);
        } else {
            error_log("Failed to save permit file " . $i);
        }
    }

    error_log("Business permit files uploaded: " . count($permitFiles));

    if (empty($permitFiles)) {
        error_log("No permit files to save");
        $response = array(
            "success" => false,
            "message" => "No business permit files were uploaded."
        );
        ob_clean();
        header('Content-Type: application/json');
        echo json_encode($response);
        $conn->close();
        exit;
    }

    // Insert business permits into database
    $permitsInserted = 0;
    $permitSql = "INSERT INTO bs_permits (reg_id, permit_file, permit_number, created_at) VALUES (?, ?, ?, NOW())";
    
    foreach ($permitFiles as $permitNumber => $permitPath) {
        error_log("Inserting permit " . $permitNumber . " with path: " . $permitPath . " for reg_id: " . $regId);
        $permitStmt = $conn->prepare($permitSql);
        
        if ($permitStmt) {
            $permitStmt->bind_param("isi", $regId, $permitPath, $permitNumber);
            if (!$permitStmt->execute()) {
                error_log("Failed to insert business permit " . $permitNumber . ": " . $permitStmt->error);
            } else {
                $permitsInserted++;
                $permitId = $conn->insert_id;
                error_log("Successfully inserted business permit " . $permitNumber . " for reg_id " . $regId . " (permit_id: " . $permitId . ")");
            }
            $permitStmt->close();
        } else {
            error_log("Failed to prepare permit insert statement for permit " . $permitNumber . ": " . $conn->error);
        }
    }

    if ($permitsInserted > 0) {
        $response = array(
            "success" => true,
            "message" => "Successfully saved " . $permitsInserted . " business permit(s).",
            "permits_inserted" => $permitsInserted,
            "reg_id" => $regId
        );
        error_log("Successfully saved " . $permitsInserted . " business permit(s) for reg_id: " . $regId);
    } else {
        $response = array(
            "success" => false,
            "message" => "Failed to save business permits to database.",
            "permits_inserted" => 0
        );
        error_log("Failed to save any business permits for reg_id: " . $regId);
    }

    ob_clean();
    header('Content-Type: application/json');
    echo json_encode($response);
    $conn->close();
    exit;

} catch (Exception $e) {
    error_log("Business permits save error: " . $e->getMessage());
    error_log("Error trace: " . $e->getTraceAsString());
    $response = array(
        "success" => false,
        "message" => "Server error: " . $e->getMessage()
    );
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode($response);
    exit;
} catch (Error $e) {
    error_log("Fatal error: " . $e->getMessage());
    error_log("Error trace: " . $e->getTraceAsString());
    $response = array(
        "success" => false,
        "message" => "Server error: " . $e->getMessage()
    );
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode($response);
    exit;
}
?>

