<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Verification - BoardEase</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 2rem;
        }

        .verification-container {
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            padding: 3rem;
            max-width: 500px;
            width: 100%;
            text-align: center;
        }

        .verification-icon {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 2rem;
            font-size: 2rem;
            color: white;
        }

        .success-icon {
            background: linear-gradient(135deg, #28a745, #20c997);
        }

        .error-icon {
            background: linear-gradient(135deg, #dc3545, #e74c3c);
        }

        .verification-title {
            font-size: 1.8rem;
            font-weight: 600;
            margin-bottom: 1rem;
            color: #333;
        }

        .verification-message {
            font-size: 1.1rem;
            color: #666;
            line-height: 1.6;
            margin-bottom: 2rem;
        }

        .btn {
            display: inline-block;
            padding: 12px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 25px;
            font-weight: 500;
            transition: transform 0.3s ease;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }

        .status-info {
            background: #f8f9fa;
            border-radius: 10px;
            padding: 1.5rem;
            margin-top: 2rem;
            border-left: 4px solid #667eea;
        }

        .status-info h3 {
            color: #333;
            margin-bottom: 1rem;
            font-size: 1.2rem;
        }

        .status-info p {
            color: #666;
            margin-bottom: 0.5rem;
        }

        .next-steps {
            background: #e8f5e8;
            border-radius: 10px;
            padding: 1.5rem;
            margin-top: 1.5rem;
            border-left: 4px solid #28a745;
        }

        .next-steps h3 {
            color: #155724;
            margin-bottom: 1rem;
            font-size: 1.2rem;
        }

        .next-steps ul {
            text-align: left;
            color: #155724;
            margin-left: 1rem;
        }

        .next-steps li {
            margin-bottom: 0.5rem;
        }
    </style>
</head>
<body>
    <div class="verification-container">
        <?php if (isset($success)): ?>
            <div class="verification-icon success-icon">
                <i class="fas fa-check"></i>
            </div>
            <h1 class="verification-title">Email Verified Successfully!</h1>
            <p class="verification-message"><?php echo htmlspecialchars($success); ?></p>
            
            <div class="status-info">
                <h3><i class="fas fa-info-circle"></i> What happens next?</h3>
                <p>Your account is now pending admin approval.</p>
                <p>Our admin team will review your registration and send you an approval email once your account is activated.</p>
            </div>
            
            <div class="next-steps">
                <h3><i class="fas fa-list-check"></i> Next Steps:</h3>
                <ul>
                    <li>Wait for admin approval (usually within 24-48 hours)</li>
                    <li>Check your email for approval notification</li>
                    <li>Once approved, you can log in to your account</li>
                    <li>Contact support if you have any questions</li>
                </ul>
            </div>
            
        <?php else: ?>
            <div class="verification-icon error-icon">
                <i class="fas fa-times"></i>
            </div>
            <h1 class="verification-title">Verification Failed</h1>
            <p class="verification-message"><?php echo htmlspecialchars($error); ?></p>
            
            <div class="status-info">
                <h3><i class="fas fa-exclamation-triangle"></i> Possible reasons:</h3>
                <p>• The verification link has expired</p>
                <p>• The link has already been used</p>
                <p>• The link is invalid or corrupted</p>
            </div>
        <?php endif; ?>
        
        <a href="index.html" class="btn">
            <i class="fas fa-home"></i> Back to Home
        </a>
    </div>
</body>
</html>


























