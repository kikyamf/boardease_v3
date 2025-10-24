<?php
// Firebase Cloud Messaging Configuration (HTTP v1 API)
class FCMConfig {
    // Your Firebase Project ID (get from Firebase Console > Project Settings > General)
    const FIREBASE_PROJECT_ID = 'boardease2';
    
    // Path to your service account JSON file
    const SERVICE_ACCOUNT_PATH = 'fcm_service_account_key.json';
    
    // FCM API URL (HTTP v1)
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
        openssl_sign($base64Header . '.' . $base64Payload, $signature, $serviceAccount['private_key'], 'SHA256');
        $base64Signature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
        
        $jwt = $base64Header . '.' . $base64Payload . '.' . $base64Signature;
        
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
        
        if ($httpCode != 200) {
            throw new Exception('Failed to get access token: ' . $response);
        }
        
        $data = json_decode($response, true);
        return $data['access_token'] ?? null;
    }
    
    /**
     * Send push notification to a single device
     */
    public static function sendToDevice($token, $title, $body, $data = []) {
        $accessToken = self::getAccessToken();
        $url = str_replace('{PROJECT_ID}', self::FIREBASE_PROJECT_ID, self::FCM_URL);
        
        $message = [
            'message' => [
                'token' => $token,
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'android' => [
                    'priority' => 'high',
                    'notification' => [
                        'sound' => self::NOTIFICATION_SOUND,
                        'icon' => self::NOTIFICATION_ICON,
                        'color' => self::NOTIFICATION_COLOR,
                        'priority' => 'high',
                        'visibility' => 'public',
                        'notification_priority' => 'PRIORITY_HIGH',
                        'default_sound' => true,
                        'default_vibrate_timings' => true,
                        'default_light_settings' => true
                    ]
                ],
                'apns' => [
                    'payload' => [
                        'aps' => [
                            'sound' => 'default',
                            'badge' => 1,
                            'alert' => [
                                'title' => $title,
                                'body' => $body
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        if (!empty($data)) {
            // Convert all data values to strings (Firebase requirement)
            $stringData = [];
            foreach ($data as $key => $value) {
                $stringData[$key] = (string) $value;
            }
            $message['message']['data'] = $stringData;
        }
        
        return self::sendRequest($url, $accessToken, $message);
    }
    
    /**
     * Send push notification to multiple devices
     */
    public static function sendToMultipleDevices($tokens, $title, $body, $data = []) {
        $results = [];
        foreach ($tokens as $token) {
            $results[] = self::sendToDevice($token, $title, $body, $data);
        }
        return $results;
    }
    
    /**
     * Send data-only message (no notification popup)
     */
    public static function sendDataMessage($token, $data) {
        $accessToken = self::getAccessToken();
        $url = str_replace('{PROJECT_ID}', self::FIREBASE_PROJECT_ID, self::FCM_URL);
        
        // Convert all data values to strings (Firebase requirement)
        $stringData = [];
        foreach ($data as $key => $value) {
            $stringData[$key] = (string) $value;
        }
        
        $message = [
            'message' => [
                'token' => $token,
                'data' => $stringData
            ]
        ];
        
        return self::sendRequest($url, $accessToken, $message);
    }
    
    /**
     * Send the actual HTTP request to FCM
     */
    private static function sendRequest($url, $accessToken, $message) {
        $headers = [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));
        
        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        return [
            'success' => $httpCode == 200,
            'http_code' => $httpCode,
            'response' => json_decode($result, true)
        ];
    }
    
    /**
     * Log notification for debugging
     */
    public static function logNotification($userId, $type, $title, $body, $result) {
        $logData = [
            'timestamp' => date('Y-m-d H:i:s'),
            'user_id' => $userId,
            'type' => $type,
            'title' => $title,
            'body' => $body,
            'result' => $result
        ];
        
        // Log to file (you can also log to database)
        error_log("FCM Notification: " . json_encode($logData));
    }
}
?>








