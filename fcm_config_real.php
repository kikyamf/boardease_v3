<?php
// Firebase Cloud Messaging Configuration for Real Use
class FCMConfig {
    // Update these with your actual Firebase project details
    private static $FIREBASE_PROJECT_ID = 'boardease2'; // Your Firebase project ID
    private static $SERVICE_ACCOUNT_PATH = 'path/to/your/service-account-key.json'; // Path to your service account key
    
    public static function getAccessToken() {
        if (!file_exists(self::$SERVICE_ACCOUNT_PATH)) {
            throw new Exception("Service account file not found: " . self::$SERVICE_ACCOUNT_PATH);
        }
        
        $serviceAccount = json_decode(file_get_contents(self::$SERVICE_ACCOUNT_PATH), true);
        
        $header = json_encode(['typ' => 'JWT', 'alg' => 'RS256']);
        $payload = json_encode([
            'iss' => $serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'iat' => time(),
            'exp' => time() + 3600
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
        
        if ($httpCode !== 200) {
            throw new Exception("Failed to get access token: HTTP $httpCode - $response");
        }
        
        $data = json_decode($response, true);
        return $data['access_token'];
    }
    
    public static function sendToDevice($deviceToken, $title, $body, $data = []) {
        try {
            $accessToken = self::getAccessToken();
            
            $message = [
                'message' => [
                    'token' => $deviceToken,
                    'notification' => [
                        'title' => $title,
                        'body' => $body
                    ],
                    'android' => [
                        'priority' => 'high',
                        'notification' => [
                            'sound' => 'default',
                            'priority' => 'high',
                            'visibility' => 'public',
                            'notification_priority' => 'PRIORITY_HIGH'
                        ]
                    ],
                    'apns' => [
                        'payload' => [
                            'aps' => [
                                'sound' => 'default',
                                'badge' => 1
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
            
            return self::sendRequest($accessToken, $message);
        } catch (Exception $e) {
            return [
                'success' => false,
                'error' => $e->getMessage()
            ];
        }
    }
    
    public static function sendToMultipleDevices($deviceTokens, $title, $body, $data = []) {
        try {
            $accessToken = self::getAccessToken();
            
            $message = [
                'message' => [
                    'tokens' => $deviceTokens,
                    'notification' => [
                        'title' => $title,
                        'body' => $body
                    ],
                    'android' => [
                        'priority' => 'high',
                        'notification' => [
                            'sound' => 'default',
                            'priority' => 'high',
                            'visibility' => 'public',
                            'notification_priority' => 'PRIORITY_HIGH'
                        ]
                    ],
                    'apns' => [
                        'payload' => [
                            'aps' => [
                                'sound' => 'default',
                                'badge' => 1
                            ]
                        ]
                    ]
                ]
            ];
            
            if (!empty($data)) {
                $stringData = [];
                foreach ($data as $key => $value) {
                    $stringData[$key] = (string) $value;
                }
                $message['message']['data'] = $stringData;
            }
            
            return self::sendRequest($accessToken, $message);
        } catch (Exception $e) {
            return [
                'success' => false,
                'error' => $e->getMessage()
            ];
        }
    }
    
    private static function sendRequest($accessToken, $message) {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, 'https://fcm.googleapis.com/v1/projects/' . self::$FIREBASE_PROJECT_ID . '/messages:send');
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
        
        $result = [
            'success' => $httpCode === 200,
            'http_code' => $httpCode,
            'response' => json_decode($response, true)
        ];
        
        if ($httpCode === 200) {
            self::logNotification($message, $result);
        }
        
        return $result;
    }
    
    private static function logNotification($message, $result) {
        $logEntry = [
            'timestamp' => date('Y-m-d H:i:s'),
            'message' => $message,
            'result' => $result
        ];
        
        file_put_contents('fcm_notifications.log', json_encode($logEntry) . "\n", FILE_APPEND);
    }
}
?>
























