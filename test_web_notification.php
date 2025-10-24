<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Firebase Cloud Messaging Test</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            text-align: center;
        }
        .test-section {
            margin: 20px 0;
            padding: 20px;
            border: 1px solid #ddd;
            border-radius: 5px;
            background-color: #f9f9f9;
        }
        .btn {
            background-color: #4CAF50;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
            margin: 5px;
        }
        .btn:hover {
            background-color: #45a049;
        }
        .btn-danger {
            background-color: #f44336;
        }
        .btn-danger:hover {
            background-color: #da190b;
        }
        .result {
            margin-top: 15px;
            padding: 10px;
            border-radius: 5px;
            font-family: monospace;
            white-space: pre-wrap;
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
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔥 Firebase Cloud Messaging Test</h1>
        
        <div class="info">
            <strong>📱 Instructions:</strong><br>
            1. Make sure your Android app is running in the background<br>
            2. Click any test button below<br>
            3. Check your Android device for notifications<br>
            4. Look at the results below<br>
            <strong>🔔 For popup notifications:</strong> Use "Test 5: Popup Notification", "Test 6: Messenger-Style Popup", "Test 7: Force Popup", or "Test 8: Banner Notification" - these will show popups with sound and vibration!
        </div>

        <div class="test-section">
            <h3>🧪 Test 1: Simple Hello Notification</h3>
            <p>Send a simple "Hello" notification to your device</p>
            <button class="btn" onclick="testNotification('hello')">Send Hello Notification</button>
        </div>

        <div class="test-section">
            <h3>💬 Test 2: Chat Message Notification</h3>
            <p>Send a chat message notification (like when someone messages you)</p>
            <button class="btn" onclick="testNotification('chat')">Send Chat Notification</button>
        </div>

        <div class="test-section">
            <h3>🏠 Test 3: Boarding House Notification</h3>
            <p>Send a boarding house related notification</p>
            <button class="btn" onclick="testNotification('boarding')">Send Boarding House Notification</button>
        </div>

        <div class="test-section">
            <h3>📊 Test 4: Badge Update (Data Only)</h3>
            <p>Send a data message to update message count (no visible notification)</p>
            <button class="btn" onclick="testNotification('badge')">Send Badge Update</button>
        </div>

        <div class="test-section">
            <h3>🔔 Test 5: Popup Notification</h3>
            <p>Send a notification that should pop up on your screen with sound and vibration</p>
            <button class="btn btn-danger" onclick="testNotification('popup')">Send Popup Notification</button>
        </div>

        <div class="test-section">
            <h3>💬 Test 6: Messenger-Style Popup</h3>
            <p>Send a notification that looks exactly like Messenger with profile picture and message</p>
            <button class="btn btn-danger" onclick="testNotification('messenger')">Send Messenger-Style Popup</button>
        </div>

        <div class="test-section">
            <h3>🚨 Test 7: Force Popup Notification</h3>
            <p>Send a notification that should FORCE a popup on your screen (MAX priority)</p>
            <button class="btn btn-danger" onclick="testNotification('force_popup')">Send Force Popup</button>
            <br><small style="color: #666;">This test uses MAX priority and should definitely show a popup notification on your screen.</small>
        </div>

        <div class="test-section">
            <h3>📱 Test 8: Banner Notification</h3>
            <p>Send a notification that should appear as a banner at the top of your screen</p>
            <button class="btn btn-danger" onclick="testNotification('banner')">Send Banner Notification</button>
            <br><small style="color: #666;">This test creates a banner notification that appears at the top of your screen, like Messenger or WhatsApp.</small>
        </div>

        <div id="result"></div>
    </div>

    <script>
        function testNotification(type) {
            const resultDiv = document.getElementById('result');
            resultDiv.innerHTML = '<div class="info">⏳ Sending notification... Please wait...</div>';
            
            fetch('send_test_notification.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ type: type })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    resultDiv.innerHTML = `<div class="success">✅ SUCCESS!<br>${data.message}<br><br>Response:<br>${JSON.stringify(data.response, null, 2)}</div>`;
                } else {
                    resultDiv.innerHTML = `<div class="error">❌ FAILED<br>${data.message}<br><br>Error:<br>${JSON.stringify(data.error, null, 2)}</div>`;
                }
            })
            .catch(error => {
                resultDiv.innerHTML = `<div class="error">❌ ERROR<br>${error.message}</div>`;
            });
        }
    </script>
</body>
</html>








