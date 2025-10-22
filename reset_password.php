<?php
// Simple password reset page
// In a real application, you would:
// 1. Validate the token
// 2. Check if it's not expired
// 3. Allow user to set a new password
// 4. Update the password in the database
// 5. Invalidate the token

$token = isset($_GET['token']) ? $_GET['token'] : '';

if (empty($token)) {
    echo "<h1>Invalid Reset Link</h1>";
    echo "<p>The password reset link is invalid or has expired.</p>";
    echo "<p><a href='login.php'>Back to Login</a></p>";
    exit();
}

// In a real application, you would validate the token here
// For this demo, we'll just show a simple form
?>
<!DOCTYPE html>
<html>
<head>
    <title>Reset Password - BoardEase</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 400px; margin: 50px auto; padding: 20px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; }
        input[type="password"] { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
        button { background: #A18167; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background: #8a6f5a; }
        .error { color: red; }
        .success { color: green; }
    </style>
</head>
<body>
    <h1>Reset Your Password</h1>
    
    <?php if (isset($_POST['new_password'])): ?>
        <?php
        // In a real application, you would:
        // 1. Validate the token
        // 2. Update the password in the database
        // 3. Invalidate the token
        
        $new_password = $_POST['new_password'];
        $confirm_password = $_POST['confirm_password'];
        
        if ($new_password !== $confirm_password) {
            echo "<p class='error'>Passwords do not match.</p>";
        } elseif (strlen($new_password) < 6) {
            echo "<p class='error'>Password must be at least 6 characters long.</p>";
        } else {
            // In a real application, update the password here
            echo "<p class='success'>Password has been reset successfully!</p>";
            echo "<p><a href='login.php'>Back to Login</a></p>";
            exit();
        }
        ?>
    <?php endif; ?>
    
    <form method="POST">
        <div class="form-group">
            <label for="new_password">New Password:</label>
            <input type="password" id="new_password" name="new_password" required>
        </div>
        
        <div class="form-group">
            <label for="confirm_password">Confirm Password:</label>
            <input type="password" id="confirm_password" name="confirm_password" required>
        </div>
        
        <button type="submit">Reset Password</button>
    </form>
    
    <p><a href="login.php">Back to Login</a></p>
</body>
</html>
