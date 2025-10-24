<?php
// Firebase Cloud Messaging Configuration (HTTP v1 API)
class FCMConfig {
    // TODO: Replace with your actual Firebase Project ID
    const FIREBASE_PROJECT_ID = 'your-actual-project-id-here';
    
    // TODO: Replace with the path to your downloaded service account JSON file
    const SERVICE_ACCOUNT_PATH = 'your-service-account-key.json';
    
    // FCM API URL (HTTP v1) - This will be updated automatically
    const FCM_URL = 'https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send';
    
    // Notification settings
    const NOTIFICATION_SOUND = 'default';
    const NOTIFICATION_ICON = 'ic_notification';
    const NOTIFICATION_COLOR = '#FF4444';
    
    /**
     * Get access token using service account
     */
    private static function getAccessToken() {
        if (!file_exists(self::SERVICE_ACCOUNT_PATH)) {
            throw new Exception('Service account file not found: ' . self::SERVICE_ACCOUNT_PATH);
        }
        
        $serviceAccount = json_decode(file_get_contents(self::SERVICE_ACCOUNT_PATH), true);
        
        if (!$serviceAccount) {
            throw new Exception('Invalid service account JSON file');
        }
        
        // Create JWT token
        $header = json_encode(['typ' => 'JWT', 'alg' => 'RS256']);
        $now = time();
        $payload = json_encode([
            'iss' => $serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'iat' => $now,
            'exp' => $now + 3600
        ]);
        
        $base64Header = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
        $base64Payload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($payload));
        
        $signature = '';
        $privateKey = $serviceAccount['private_key'];
        openssl_sign($base64Header . '.' . $base64Payload, $signature, $privateKey, 'SHA256');
        $base64Signature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
        
        $jwt = $base64Header . '.' . $base64Payload . '.' . $base64Signature;
        
        // Exchange JWT for access token
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, 'https://oauth2.googleapis.com/token');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ]));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/x-www-form-urlencoded']);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode !== 200) {
            throw new Exception('Failed to get access token. HTTP Code: ' . $httpCode . ', Response: ' . $response);
        }
        
        $data = json_decode($response, true);
        return $data['access_token'];
    }
    
    /**
     * Send notification to a single device
     */
    public static function sendToDevice($deviceToken, $title, $body, $data = []) {
        $accessToken = self::getAccessToken();
        $url = str_replace('{PROJECT_ID}', self::FIREBASE_PROJECT_ID, self::FCM_URL);
        
        $message = [
            'message' => [
                'token' => $deviceToken,
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'android' => [
                    'notification' => [
                        'sound' => self::NOTIFICATION_SOUND,
                        'icon' => self::NOTIFICATION_ICON,
                        'color' => self::NOTIFICATION_COLOR
                    ]
                ]
            ]
        ];
        
        if (!empty($data)) {
            $message['message']['data'] = $data;
        }
        
        return self::sendRequest($url, $accessToken, $message);
    }
    
    /**
     * Send data-only message (for badge updates, etc.)
     */
    public static function sendDataMessage($deviceToken, $data) {
        $accessToken = self::getAccessToken();
        $url = str_replace('{PROJECT_ID}', self::FIREBASE_PROJECT_ID, self::FCM_URL);
        
        $message = [
            'message' => [
                'token' => $deviceToken,
                'data' => $data
            ]
        ];
        
        return self::sendRequest($url, $accessToken, $message);
    }
    
    /**
     * Send request to FCM API
     */
    private static function sendRequest($url, $accessToken, $message) {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        $responseData = json_decode($response, true);
        
        return [
            'success' => $httpCode === 200,
            'http_code' => $httpCode,
            'response' => $responseData
        ];
    }
    
    /**
     * Log notification for debugging
     */
    public static function logNotification($deviceToken, $title, $body, $result) {
        $logEntry = [
            'timestamp' => date('Y-m-d H:i:s'),
            'device_token' => substr($deviceToken, 0, 20) . '...',
            'title' => $title,
            'body' => $body,
            'success' => $result['success'],
            'http_code' => $result['http_code'],
            'response' => $result['response']
        ];
        
        file_put_contents('fcm_log.txt', json_encode($logEntry) . "\n", FILE_APPEND);
    }
}
?>
























