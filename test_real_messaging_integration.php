<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Real Messaging Integration Test</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 1000px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            text-align: center;
        }
        .test-section {
            margin: 20px 0;
            padding: 15px;
            border: 1px solid #ddd;
            border-radius: 5px;
            background-color: #f9f9f9;
        }
        .form-group {
            margin: 10px 0;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
        }
        input, select, textarea {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .btn {
            background-color: #2196F3;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            margin: 5px;
        }
        .btn:hover {
            background-color: #1976D2;
        }
        .btn-success {
            background-color: #4CAF50;
        }
        .btn-success:hover {
            background-color: #45a049;
        }
        .result {
            margin-top: 15px;
            padding: 10px;
            border-radius: 5px;
            font-family: monospace;
            white-space: pre-wrap;
            max-height: 400px;
            overflow-y: auto;
        }
        .success {
            background-color: #d4edda;
            border: 1px solid #c3e6cb;
            color: #155724;
        }
        .error {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
        }
        .info {
            background-color: #d1ecf1;
            border: 1px solid #bee5eb;
            color: #0c5460;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>💬 Real Messaging Integration Test</h1>
        
        <div class="info">
            <strong>📱 Instructions:</strong><br>
            1. Make sure your database is set up with the provided schema<br>
            2. Update the configuration in fcm_config.php<br>
            3. Test each endpoint below<br>
            4. Check your Android device for notifications<br>
            5. Monitor the server logs for debugging
        </div>

        <div class="test-section">
            <h3>🔧 Configuration Test</h3>
            <button class="btn" onclick="testConfiguration()">Test Database & FCM Configuration</button>
            <div id="configResult" class="result" style="display: none;"></div>
        </div>

        <div class="test-section">
            <h3>👤 User Management</h3>
            <div class="form-group">
                <label>Current User ID:</label>
                <input type="number" id="currentUserId" value="1" placeholder="Enter your user ID">
            </div>
            <button class="btn" onclick="getUsers()">Get All Users</button>
            <button class="btn" onclick="searchUsers()">Search Users</button>
            <div class="form-group">
                <label>Search Term:</label>
                <input type="text" id="searchTerm" placeholder="Enter name to search">
            </div>
            <div id="userResult" class="result" style="display: none;"></div>
        </div>

        <div class="test-section">
            <h3>💬 Individual Messages</h3>
            <div class="form-group">
                <label>Sender ID:</label>
                <input type="number" id="senderId" value="1">
            </div>
            <div class="form-group">
                <label>Receiver ID:</label>
                <input type="number" id="receiverId" value="2">
            </div>
            <div class="form-group">
                <label>Message:</label>
                <textarea id="messageText" placeholder="Enter your message">Hello! This is a test message from the real messaging system.</textarea>
            </div>
            <button class="btn btn-success" onclick="sendMessage()">Send Message</button>
            <button class="btn" onclick="getMessages()">Get Messages</button>
            <div id="messageResult" class="result" style="display: none;"></div>
        </div>

        <div class="test-section">
            <h3>👥 Group Messages</h3>
            <div class="form-group">
                <label>Group ID:</label>
                <input type="number" id="groupId" value="1">
            </div>
            <div class="form-group">
                <label>Group Message:</label>
                <textarea id="groupMessageText" placeholder="Enter your group message">Hello everyone! This is a test group message.</textarea>
            </div>
            <button class="btn btn-success" onclick="sendGroupMessage()">Send Group Message</button>
            <button class="btn" onclick="getGroupMessages()">Get Group Messages</button>
            <button class="btn" onclick="getGroupMembers()">Get Group Members</button>
            <div id="groupResult" class="result" style="display: none;"></div>
        </div>

        <div class="test-section">
            <h3>📊 Statistics</h3>
            <button class="btn" onclick="getChatList()">Get Chat List</button>
            <button class="btn" onclick="getUnreadCount()">Get Unread Count</button>
            <div id="statsResult" class="result" style="display: none;"></div>
        </div>
    </div>

    <script>
        function showResult(elementId, message, isSuccess = true) {
            const resultDiv = document.getElementById(elementId);
            resultDiv.style.display = 'block';
            resultDiv.className = 'result ' + (isSuccess ? 'success' : 'error');
            resultDiv.textContent = message;
        }

        async function testConfiguration() {
            showResult('configResult', 'Testing configuration...\n\nPlease wait...', true);
            
            try {
                // Test database connection
                const dbResponse = await fetch('message/get_users_for_messaging.php?current_user_id=1');
                const dbData = await dbResponse.json();
                
                let result = 'Configuration Test Results:\n\n';
                result += 'Database Connection: ' + (dbData.success ? '✅ SUCCESS' : '❌ FAILED') + '\n';
                
                if (dbData.success) {
                    result += 'Users Found: ' + dbData.data.total_count + '\n';
                } else {
                    result += 'Error: ' + dbData.message + '\n';
                }
                
                result += '\nFCM Configuration: Check fcm_config.php\n';
                result += '- Update FIREBASE_PROJECT_ID\n';
                result += '- Update SERVICE_ACCOUNT_PATH\n';
                result += '- Ensure service account key file exists\n';
                
                showResult('configResult', result, dbData.success);
            } catch (error) {
                showResult('configResult', '❌ Configuration Test Failed\n\nError: ' + error.message, false);
            }
        }

        async function getUsers() {
            const userId = document.getElementById('currentUserId').value;
            showResult('userResult', 'Getting users...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_users_for_messaging.php?current_user_id=${userId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Users Retrieved Successfully\n\nTotal Users: ${data.data.total_count}\n\n`;
                    data.data.users.forEach(user => {
                        result += `ID: ${user.user_id} | Name: ${user.full_name} | Type: ${user.user_type} | Online: ${user.is_online ? 'Yes' : 'No'}\n`;
                    });
                    showResult('userResult', result, true);
                } else {
                    showResult('userResult', '❌ Failed to get users\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('userResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function searchUsers() {
            const userId = document.getElementById('currentUserId').value;
            const searchTerm = document.getElementById('searchTerm').value;
            
            if (!searchTerm) {
                showResult('userResult', 'Please enter a search term', false);
                return;
            }
            
            showResult('userResult', `Searching for "${searchTerm}"...\n\nPlease wait...`, true);
            
            try {
                const response = await fetch(`message/search_users.php?current_user_id=${userId}&search_term=${encodeURIComponent(searchTerm)}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Search Results for "${searchTerm}"\n\nFound: ${data.data.total_count} users\n\n`;
                    data.data.users.forEach(user => {
                        result += `ID: ${user.user_id} | Name: ${user.full_name} | Type: ${user.user_type}\n`;
                    });
                    showResult('userResult', result, true);
                } else {
                    showResult('userResult', '❌ Search failed\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('userResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function sendMessage() {
            const senderId = document.getElementById('senderId').value;
            const receiverId = document.getElementById('receiverId').value;
            const message = document.getElementById('messageText').value;
            
            if (!senderId || !receiverId || !message) {
                showResult('messageResult', 'Please fill in all fields', false);
                return;
            }
            
            showResult('messageResult', 'Sending message...\n\nPlease wait...', true);
            
            try {
                const formData = new FormData();
                formData.append('sender_id', senderId);
                formData.append('receiver_id', receiverId);
                formData.append('message', message);
                
                const response = await fetch('message/send_message.php', {
                    method: 'POST',
                    body: formData
                });
                
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Message Sent Successfully\n\n`;
                    result += `Message ID: ${data.data.message_id}\n`;
                    result += `From: ${data.data.sender_name}\n`;
                    result += `To: ${data.data.receiver_name}\n`;
                    result += `Message: ${data.data.message}\n`;
                    result += `Notification Sent: ${data.data.notification_sent ? 'Yes' : 'No'}\n`;
                    result += `Reason: ${data.data.notification_reason}\n`;
                    showResult('messageResult', result, true);
                } else {
                    showResult('messageResult', '❌ Failed to send message\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('messageResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function getMessages() {
            const senderId = document.getElementById('senderId').value;
            const receiverId = document.getElementById('receiverId').value;
            
            showResult('messageResult', 'Getting messages...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_messages.php?user1_id=${senderId}&user2_id=${receiverId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Messages Retrieved\n\nTotal: ${data.data.total_count} messages\n\n`;
                    data.data.messages.forEach(msg => {
                        result += `[${msg.timestamp}] ${msg.sender_name}: ${msg.message} (${msg.status})\n`;
                    });
                    showResult('messageResult', result, true);
                } else {
                    showResult('messageResult', '❌ Failed to get messages\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('messageResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function sendGroupMessage() {
            const senderId = document.getElementById('senderId').value;
            const groupId = document.getElementById('groupId').value;
            const message = document.getElementById('groupMessageText').value;
            
            if (!senderId || !groupId || !message) {
                showResult('groupResult', 'Please fill in all fields', false);
                return;
            }
            
            showResult('groupResult', 'Sending group message...\n\nPlease wait...', true);
            
            try {
                const formData = new FormData();
                formData.append('sender_id', senderId);
                formData.append('group_id', groupId);
                formData.append('message', message);
                
                const response = await fetch('message/send_group_message.php', {
                    method: 'POST',
                    body: formData
                });
                
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Group Message Sent Successfully\n\n`;
                    result += `Message ID: ${data.data.message_id}\n`;
                    result += `Group: ${data.data.group_name}\n`;
                    result += `From: User ${data.data.sender_id}\n`;
                    result += `Message: ${data.data.message}\n`;
                    result += `Notifications Sent: ${data.data.notifications_sent}/${data.data.total_members}\n`;
                    showResult('groupResult', result, true);
                } else {
                    showResult('groupResult', '❌ Failed to send group message\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('groupResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function getGroupMessages() {
            const groupId = document.getElementById('groupId').value;
            
            showResult('groupResult', 'Getting group messages...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_group_messages.php?group_id=${groupId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Group Messages Retrieved\n\nTotal: ${data.data.total_count} messages\n\n`;
                    data.data.messages.forEach(msg => {
                        result += `[${msg.timestamp}] ${msg.sender_name}: ${msg.message} (${msg.status})\n`;
                    });
                    showResult('groupResult', result, true);
                } else {
                    showResult('groupResult', '❌ Failed to get group messages\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('groupResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function getGroupMembers() {
            const groupId = document.getElementById('groupId').value;
            
            showResult('groupResult', 'Getting group members...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_group_members.php?group_id=${groupId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Group Members Retrieved\n\n`;
                    result += `Group: ${data.data.group.group_name}\n`;
                    result += `Members: ${data.data.member_count}\n\n`;
                    data.data.members.forEach(member => {
                        result += `ID: ${member.user_id} | Name: ${member.full_name} | Type: ${member.user_type} | Online: ${member.is_online ? 'Yes' : 'No'}\n`;
                    });
                    showResult('groupResult', result, true);
                } else {
                    showResult('groupResult', '❌ Failed to get group members\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('groupResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function getChatList() {
            const userId = document.getElementById('currentUserId').value;
            
            showResult('statsResult', 'Getting chat list...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_chat_list.php?user_id=${userId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Chat List Retrieved\n\nTotal Chats: ${data.data.total_count}\n\n`;
                    data.data.chats.forEach(chat => {
                        result += `Type: ${chat.chat_type} | `;
                        if (chat.chat_type === 'individual') {
                            result += `User: ${chat.other_user_name} | `;
                        } else {
                            result += `Group: ${chat.group_name} | `;
                        }
                        result += `Unread: ${chat.unread_count} | Last: ${chat.last_message}\n`;
                    });
                    showResult('statsResult', result, true);
                } else {
                    showResult('statsResult', '❌ Failed to get chat list\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('statsResult', '❌ Error\n\n' + error.message, false);
            }
        }

        async function getUnreadCount() {
            const userId = document.getElementById('currentUserId').value;
            
            showResult('statsResult', 'Getting unread count...\n\nPlease wait...', true);
            
            try {
                const response = await fetch(`message/get_unread_count.php?user_id=${userId}`);
                const data = await response.json();
                
                if (data.success) {
                    let result = `✅ Unread Count Retrieved\n\n`;
                    result += `Individual Messages: ${data.data.individual_unread}\n`;
                    result += `Group Messages: ${data.data.group_unread}\n`;
                    result += `Total Unread: ${data.data.total_unread}\n`;
                    showResult('statsResult', result, true);
                } else {
                    showResult('statsResult', '❌ Failed to get unread count\n\nError: ' + data.message, false);
                }
            } catch (error) {
                showResult('statsResult', '❌ Error\n\n' + error.message, false);
            }
        }
    </script>
</body>
</html>









