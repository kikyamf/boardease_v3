<?php
/**
 * BoardEase Messaging System Setup Script
 * This script helps you set up the messaging system step by step
 */

header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BoardEase Messaging System Setup</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .step { margin: 20px 0; padding: 15px; border-left: 4px solid #007bff; background: #f8f9fa; }
        .step h3 { margin-top: 0; color: #007bff; }
        .success { border-left-color: #28a745; background: #d4edda; }
        .warning { border-left-color: #ffc107; background: #fff3cd; }
        .error { border-left-color: #dc3545; background: #f8d7da; }
        .code { background: #f8f9fa; padding: 10px; border-radius: 4px; font-family: monospace; margin: 10px 0; }
        .btn { background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; margin: 5px; }
        .btn:hover { background: #0056b3; }
        .btn-success { background: #28a745; }
        .btn-warning { background: #ffc107; color: #212529; }
        .status { padding: 10px; margin: 10px 0; border-radius: 4px; }
        .status.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .status.error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .status.warning { background: #fff3cd; color: #856404; border: 1px solid #ffeaa7; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 BoardEase Messaging System Setup</h1>
        <p>This setup will help you configure the messaging system with FCM notifications.</p>

        <?php
        // Check current status
        $db_connected = false;
        $tables_exist = false;
        $has_data = false;
        $fcm_configured = false;
        
        try {
            if (file_exists('db_connection.php')) {
                require_once 'db_connection.php';
                $db = getDB();
                if ($db) {
                    $db_connected = true;
                    
                    // Check tables
                    $tables = ['users', 'messages', 'chat_groups', 'group_members', 'group_messages', 'device_tokens'];
                    $existing_tables = 0;
                    foreach ($tables as $table) {
                        $stmt = $db->prepare("SHOW TABLES LIKE ?");
                        $stmt->execute([$table]);
                        if ($stmt->rowCount() > 0) {
                            $existing_tables++;
                        }
                    }
                    
                    if ($existing_tables == count($tables)) {
                        $tables_exist = true;
                        
                        // Check for data
                        $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
                        $stmt->execute();
                        $user_count = $stmt->fetch()['count'];
                        if ($user_count > 0) {
                            $has_data = true;
                        }
                    }
                }
            }
            
            // Check FCM config
            if (file_exists('fcm_config_real.php')) {
                $fcm_configured = true;
            }
            
        } catch (Exception $e) {
            // Handle errors silently for now
        }
        ?>

        <!-- Step 1: Database Connection -->
        <div class="step <?php echo $db_connected ? 'success' : 'error'; ?>">
            <h3>Step 1: Database Connection</h3>
            <?php if ($db_connected): ?>
                <div class="status success">✅ Database connection successful!</div>
            <?php else: ?>
                <div class="status error">❌ Database connection failed</div>
                <p>Make sure your <code>db_connection.php</code> is configured correctly.</p>
            <?php endif; ?>
        </div>

        <!-- Step 2: Create Tables -->
        <div class="step <?php echo $tables_exist ? 'success' : 'warning'; ?>">
            <h3>Step 2: Create Database Tables</h3>
            <?php if ($tables_exist): ?>
                <div class="status success">✅ All required tables exist!</div>
            <?php else: ?>
                <div class="status warning">⚠️ Tables need to be created</div>
                <p>Run the following SQL in your database:</p>
                <div class="code">
                    <a href="messaging_database_schema.sql" target="_blank">📄 messaging_database_schema.sql</a>
                </div>
                <p>Or copy the contents and run in your database management tool.</p>
            <?php endif; ?>
        </div>

        <!-- Step 3: Insert Test Data -->
        <div class="step <?php echo $has_data ? 'success' : 'warning'; ?>">
            <h3>Step 3: Insert Test Data</h3>
            <?php if ($has_data): ?>
                <div class="status success">✅ Database has sample data!</div>
            <?php else: ?>
                <div class="status warning">⚠️ No data found</div>
                <p>Insert sample data for testing:</p>
                <div class="code">
                    <a href="insert_test_data.sql" target="_blank">📄 insert_test_data.sql</a>
                </div>
                <p>This will create sample users, groups, and messages for testing.</p>
            <?php endif; ?>
        </div>

        <!-- Step 4: FCM Configuration -->
        <div class="step <?php echo $fcm_configured ? 'success' : 'warning'; ?>">
            <h3>Step 4: Firebase Cloud Messaging Setup</h3>
            <?php if ($fcm_configured): ?>
                <div class="status success">✅ FCM configuration file exists!</div>
            <?php else: ?>
                <div class="status warning">⚠️ FCM configuration needed</div>
                <p>Set up Firebase Cloud Messaging:</p>
                <ol>
                    <li>Go to <a href="https://console.firebase.google.com" target="_blank">Firebase Console</a></li>
                    <li>Create a new project or select existing one</li>
                    <li>Enable Cloud Messaging</li>
                    <li>Generate a service account key</li>
                    <li>Update <code>fcm_config_real.php</code> with your credentials</li>
                </ol>
            <?php endif; ?>
        </div>

        <!-- Testing Section -->
        <div class="step">
            <h3>Step 5: Test the System</h3>
            <p>Once everything is set up, test the messaging system:</p>
            
            <a href="test_configuration_safe.php" class="btn">🔧 Test Configuration</a>
            <a href="test_real_messaging_integration.php" class="btn btn-success">📱 Test Messaging</a>
            <a href="test_web_notification.php" class="btn btn-warning">🔔 Test Notifications</a>
        </div>

        <!-- API Endpoints -->
        <div class="step">
            <h3>📡 Available API Endpoints</h3>
            <p>Your messaging system includes these endpoints:</p>
            <ul>
                <li><strong>Individual Messages:</strong>
                    <ul>
                        <li><code>send_message.php</code> - Send individual message</li>
                        <li><code>get_messages.php</code> - Get conversation messages</li>
                    </ul>
                </li>
                <li><strong>Group Messages:</strong>
                    <ul>
                        <li><code>send_group_message.php</code> - Send group message</li>
                        <li><code>get_group_messages.php</code> - Get group messages</li>
                        <li><code>create_group_chat.php</code> - Create new group</li>
                        <li><code>get_group_members.php</code> - Get group members</li>
                    </ul>
                </li>
                <li><strong>User Management:</strong>
                    <ul>
                        <li><code>get_users_for_messaging.php</code> - Get all users</li>
                        <li><code>search_users.php</code> - Search users by name</li>
                        <li><code>register_device_token.php</code> - Register FCM token</li>
                    </ul>
                </li>
                <li><strong>Chat Management:</strong>
                    <ul>
                        <li><code>get_chat_list.php</code> - Get user's chat list</li>
                        <li><code>get_unread_count.php</code> - Get unread message count</li>
                        <li><code>mark_messages_read.php</code> - Mark messages as read</li>
                    </ul>
                </li>
            </ul>
        </div>

        <!-- Android Integration -->
        <div class="step">
            <h3>📱 Android App Integration</h3>
            <p>To integrate with your Android app:</p>
            <ol>
                <li>Update your Android app to use these PHP endpoints</li>
                <li>Replace dummy data calls with real API calls</li>
                <li>Test with the sample data</li>
                <li>Configure FCM in your Android app</li>
            </ol>
        </div>

        <!-- Status Summary -->
        <div class="step">
            <h3>📊 Current Status</h3>
            <ul>
                <li>Database Connection: <?php echo $db_connected ? '✅' : '❌'; ?></li>
                <li>Tables Created: <?php echo $tables_exist ? '✅' : '❌'; ?></li>
                <li>Sample Data: <?php echo $has_data ? '✅' : '❌'; ?></li>
                <li>FCM Configured: <?php echo $fcm_configured ? '✅' : '❌'; ?></li>
            </ul>
            
            <?php if ($db_connected && $tables_exist && $has_data && $fcm_configured): ?>
                <div class="status success">
                    🎉 <strong>System Ready!</strong> Your messaging system is fully configured and ready to use.
                </div>
            <?php else: ?>
                <div class="status warning">
                    ⚠️ <strong>Setup Incomplete</strong> Please complete the missing steps above.
                </div>
            <?php endif; ?>
        </div>
    </div>
</body>
</html>























