<?php
// view_registrations.php
// View all entries in the registrations table

// Database connection
$servername = "localhost";
$username   = "boardease";
$password   = "boardease";
$dbname     = "boardease2";

// Connect to database
$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Query all registrations
$sql = "SELECT * FROM registrations ORDER BY created_at DESC";
$result = $conn->query($sql);

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>View Registrations - BoardEase</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #A18167;
            padding-bottom: 10px;
        }
        .stats {
            background-color: #f9f9f9;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
        }
        .stats p {
            margin: 5px 0;
            font-size: 14px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #A18167;
            color: white;
            font-weight: bold;
            position: sticky;
            top: 0;
        }
        tr:hover {
            background-color: #f5f5f5;
        }
        .status-unverified {
            color: #ff9800;
            font-weight: bold;
        }
        .status-verified {
            color: #4CAF50;
            font-weight: bold;
        }
        .file-link {
            color: #2196F3;
            text-decoration: none;
        }
        .file-link:hover {
            text-decoration: underline;
        }
        .no-data {
            text-align: center;
            padding: 40px;
            color: #666;
        }
        .password {
            font-family: monospace;
            color: #999;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📋 Registrations Table - All Entries</h1>
        
        <?php
        if ($result && $result->num_rows > 0) {
            $total = $result->num_rows;
            $verified = 0;
            $unverified = 0;
            
            // Count statuses
            $countSql = "SELECT status, COUNT(*) as count FROM registrations GROUP BY status";
            $countResult = $conn->query($countSql);
            if ($countResult) {
                while ($row = $countResult->fetch_assoc()) {
                    if ($row['status'] == 'verified') {
                        $verified = $row['count'];
                    } else if ($row['status'] == 'unverified') {
                        $unverified = $row['count'];
                    }
                }
            }
        ?>
        
        <div class="stats">
            <p><strong>Total Registrations:</strong> <?php echo $total; ?></p>
            <p><strong>Verified:</strong> <span class="status-verified"><?php echo $verified; ?></span></p>
            <p><strong>Unverified:</strong> <span class="status-unverified"><?php echo $unverified; ?></span></p>
        </div>
        
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Role</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Status</th>
                    <th>ID Type</th>
                    <th>ID Number</th>
                    <th>Files</th>
                    <th>Created At</th>
                </tr>
            </thead>
            <tbody>
                <?php
                // Reset result pointer
                $result->data_seek(0);
                
                while ($row = $result->fetch_assoc()) {
                    $fullName = trim(($row['first_name'] ?? '') . ' ' . ($row['middle_name'] ?? '') . ' ' . ($row['last_name'] ?? ''));
                    $statusClass = $row['status'] == 'verified' ? 'status-verified' : 'status-unverified';
                    
                    echo "<tr>";
                    echo "<td>" . htmlspecialchars($row['reg_id'] ?? 'N/A') . "</td>";
                    echo "<td>" . htmlspecialchars($row['role'] ?? 'N/A') . "</td>";
                    echo "<td>" . htmlspecialchars($fullName) . "</td>";
                    echo "<td>" . htmlspecialchars($row['email'] ?? 'N/A') . "</td>";
                    echo "<td>" . htmlspecialchars($row['phone'] ?? 'N/A') . "</td>";
                    echo "<td class='" . $statusClass . "'>" . htmlspecialchars($row['status'] ?? 'N/A') . "</td>";
                    echo "<td>" . htmlspecialchars($row['valid_id_type'] ?? 'N/A') . "</td>";
                    echo "<td>" . htmlspecialchars($row['id_number'] ?? 'N/A') . "</td>";
                    
                    // Files column
                    echo "<td>";
                    if (!empty($row['idFrontFile'])) {
                        echo "<a href='../" . htmlspecialchars($row['idFrontFile']) . "' target='_blank' class='file-link'>Front ID</a> ";
                    }
                    if (!empty($row['idBackFile'])) {
                        echo "<a href='../" . htmlspecialchars($row['idBackFile']) . "' target='_blank' class='file-link'>Back ID</a> ";
                    }
                    if (!empty($row['gcash_qr'])) {
                        echo "<a href='../" . htmlspecialchars($row['gcash_qr']) . "' target='_blank' class='file-link'>GCash QR</a>";
                    }
                    if (empty($row['idFrontFile']) && empty($row['idBackFile']) && empty($row['gcash_qr'])) {
                        echo "No files";
                    }
                    echo "</td>";
                    
                    echo "<td>" . htmlspecialchars($row['created_at'] ?? 'N/A') . "</td>";
                    echo "</tr>";
                }
                ?>
            </tbody>
        </table>
        
        <?php
        } else {
            echo "<div class='no-data'>";
            echo "<h2>No registrations found</h2>";
            echo "<p>The registrations table is empty.</p>";
            echo "</div>";
        }
        
        $conn->close();
        ?>
        
        <div style="margin-top: 30px; padding: 15px; background-color: #f9f9f9; border-radius: 5px;">
            <p><strong>Note:</strong> This page shows all entries in the registrations table.</p>
            <p><strong>Last updated:</strong> <?php echo date('Y-m-d H:i:s'); ?></p>
        </div>
    </div>
</body>
</html>

