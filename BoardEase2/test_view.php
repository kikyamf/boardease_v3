<?php
// Simple test file
echo "PHP is working!";
echo "<br>Current directory: " . __DIR__;
echo "<br>File exists: " . (file_exists(__DIR__ . '/view_registrations.php') ? 'YES' : 'NO');
phpinfo();
?>

