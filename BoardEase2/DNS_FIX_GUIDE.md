# Complete Guide: Fixing DNS Resolution Issue for Buonzz API

## Understanding the Problem

**Error:** `Could not resolve host: ph-locations-api.buonzz.com`

This means your server cannot translate the domain name `ph-locations-api.buonzz.com` into an IP address. Think of DNS (Domain Name System) like a phone book - it translates human-readable names (like "ph-locations-api.buonzz.com") into IP addresses (like "123.45.67.89") that computers use to communicate.

## Step 1: Diagnose the Issue

### Run the Test Script

First, run this command from your server's terminal/SSH:

```bash
php BoardEase2/test_api_connectivity_buonzz.php
```

This will show you:
- ✅ If DNS can resolve the hostname
- ✅ If the server has internet connectivity
- ✅ If cURL can reach the API
- ✅ What the exact problem is

### Manual DNS Test

Test DNS resolution manually:

```bash
# Test 1: Try to resolve the domain
nslookup ph-locations-api.buonzz.com

# Expected output if working:
# Server: 8.8.8.8
# Address: 8.8.8.8#53
# 
# Non-authoritative answer:
# Name: ph-locations-api.buonzz.com
# Address: [some IP address]

# If it fails, you'll see:
# ** server can't find ph-locations-api.buonzz.com: NXDOMAIN
```

```bash
# Test 2: Try using dig (if available)
dig ph-locations-api.buonzz.com

# Test 3: Try using host command
host ph-locations-api.buonzz.com
```

### Test Internet Connectivity

```bash
# Test if server has internet at all
ping -c 3 8.8.8.8

# Test if DNS servers are reachable
ping -c 3 8.8.8.8  # Google DNS
ping -c 3 1.1.1.1  # Cloudflare DNS
```

---

## Step 2: Check Current DNS Configuration

### For Linux/Unix Servers

```bash
# View current DNS servers
cat /etc/resolv.conf

# Example output (good):
# nameserver 8.8.8.8
# nameserver 8.8.4.4
# search localdomain

# Example output (bad):
# nameserver 127.0.0.1  # Only local DNS, won't work for external domains
```

### Check if DNS is being managed by NetworkManager

```bash
# Check NetworkManager status
systemctl status NetworkManager

# View DNS settings
nmcli device show | grep IP4.DNS
```

### For cPanel/WHM Servers

1. Log into **WHM** (Web Host Manager)
2. Go to: **Server Configuration** → **Resolver Configuration**
3. Check the DNS servers listed
4. They should be external DNS servers (like 8.8.8.8, not just localhost)

---

## Step 3: Fix DNS Configuration

### Method 1: Fix DNS on Linux/Unix (Ubuntu/Debian/CentOS)

#### Option A: Edit `/etc/resolv.conf` directly

```bash
# 1. Backup current config
sudo cp /etc/resolv.conf /etc/resolv.conf.backup

# 2. Edit the file
sudo nano /etc/resolv.conf

# 3. Replace content with:
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1
search localdomain

# 4. Save (Ctrl+X, then Y, then Enter)

# 5. Test the fix
nslookup ph-locations-api.buonzz.com
```

**⚠️ Warning:** On some systems (Ubuntu 18.04+), `/etc/resolv.conf` might be overwritten by NetworkManager. Use Method 2 if this happens.

#### Option B: Configure NetworkManager (Ubuntu 18.04+)

```bash
# 1. Edit NetworkManager config
sudo nano /etc/NetworkManager/NetworkManager.conf

# 2. Add or modify the [main] section:
[main]
dns=8.8.8.8 8.8.4.4 1.1.1.1

# 3. Save and restart NetworkManager
sudo systemctl restart NetworkManager

# 4. Verify
cat /etc/resolv.conf
```

#### Option C: Use `systemd-resolved` (Ubuntu 18.04+)

```bash
# 1. Edit systemd-resolved config
sudo nano /etc/systemd/resolved.conf

# 2. Modify DNS settings:
[Resolve]
DNS=8.8.8.8 8.8.4.4 1.1.1.1
FallbackDNS=8.8.8.8 1.1.1.1

# 3. Restart service
sudo systemctl restart systemd-resolved

# 4. Test
systemd-resolve --status
```

#### Option D: For CentOS/RHEL (using NetworkManager)

```bash
# 1. Edit network interface config
sudo nano /etc/sysconfig/network-scripts/ifcfg-eth0

# 2. Add DNS settings:
DNS1=8.8.8.8
DNS2=8.8.4.4
DNS3=1.1.1.1

# 3. Restart network
sudo systemctl restart network

# 4. Verify
cat /etc/resolv.conf
```

---

### Method 2: Fix DNS on cPanel/WHM Servers

#### Step-by-step in WHM:

1. **Login to WHM**
   - Usually: `https://your-server-ip:2087`

2. **Navigate to DNS Settings**
   - Go to: **Server Configuration** → **Resolver Configuration**
   - Or search for "Resolver" in the search box

3. **Configure DNS Servers**
   - You'll see fields for primary, secondary, tertiary DNS
   - Enter:
     - **Primary DNS:** `8.8.8.8` (Google DNS)
     - **Secondary DNS:** `8.8.4.4` (Google DNS backup)
     - **Tertiary DNS:** `1.1.1.1` (Cloudflare DNS)

4. **Save Changes**
   - Click "Save" button

5. **Restart DNS Resolver (if needed)**
   - Go to: **Service Configuration** → **Service Manager**
   - Find "named" or "BIND" service
   - Click "Restart"

6. **Test**
   ```bash
   nslookup ph-locations-api.buonzz.com
   ```

#### Alternative: Edit `/etc/resolv.conf` directly on cPanel

```bash
# SSH into your cPanel server
ssh root@your-server-ip

# Edit resolv.conf
nano /etc/resolv.conf

# Add/change to:
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1

# Save and test
nslookup ph-locations-api.buonzz.com
```

---

### Method 3: Fix DNS on Windows Server

1. **Open Network Settings**
   - Press `Win + R`
   - Type: `ncpa.cpl` and press Enter
   - Or: Control Panel → Network and Sharing Center → Change adapter settings

2. **Select Your Network Adapter**
   - Right-click on your active network connection
   - Select "Properties"

3. **Configure IPv4 DNS**
   - Select "Internet Protocol Version 4 (TCP/IPv4)"
   - Click "Properties"

4. **Set DNS Servers**
   - Select "Use the following DNS server addresses"
   - Preferred DNS server: `8.8.8.8`
   - Alternate DNS server: `8.8.4.4`
   - Click "OK"

5. **Flush DNS Cache**
   - Open Command Prompt as Administrator
   - Run:
   ```cmd
   ipconfig /flushdns
   netsh winsock reset
   ```

6. **Test**
   ```cmd
   nslookup ph-locations-api.buonzz.com
   ```

---

### Method 4: Fix DNS on Docker Containers

If your PHP is running in a Docker container:

```dockerfile
# In your Dockerfile or docker-compose.yml

# Option A: Set DNS in docker-compose.yml
version: '3'
services:
  php:
    image: php:8.0-apache
    dns:
      - 8.8.8.8
      - 8.8.4.4
      - 1.1.1.1
```

Or run with DNS flag:
```bash
docker run --dns 8.8.8.8 --dns 8.8.4.4 your-php-image
```

---

## Step 4: Check Firewall Settings

Even if DNS is fixed, firewall might block connections.

### Check if Firewall is Blocking

```bash
# Test HTTPS connectivity directly
curl -v https://ph-locations-api.buonzz.com/v1/provinces

# If it fails with "Connection refused" or timeout, firewall might be blocking
```

### Allow HTTPS Outbound (Port 443)

#### For `iptables` (Linux):

```bash
# Check current rules
sudo iptables -L -n

# Allow HTTPS outbound (usually already allowed, but verify)
sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT

# Save rules (varies by distro)
sudo iptables-save > /etc/iptables/rules.v4  # Debian/Ubuntu
# OR
sudo service iptables save  # CentOS/RHEL
```

#### For `ufw` (Ubuntu):

```bash
# Allow HTTPS outbound
sudo ufw allow out 443/tcp

# Check status
sudo ufw status
```

#### For `firewalld` (CentOS/RHEL):

```bash
# Allow HTTPS
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

---

## Step 5: Fix PHP cURL DNS Resolution

Sometimes PHP's cURL might have its own DNS issues.

### Option 1: Use IP Address Directly (Temporary)

First, find the IP address:
```bash
nslookup ph-locations-api.buonzz.com
# Note the IP address shown
```

Then modify PHP code to use IP (NOT RECOMMENDED for production, but can work temporarily):
```php
// In philippine_address_api.php, you could temporarily use:
$ip = "XXX.XXX.XXX.XXX"; // Replace with actual IP
$apiUrl = "https://$ip/v1/provinces";

// BUT you'll need to add Host header:
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Host: ph-locations-api.buonzz.com',
    'Accept: application/json'
]);
```

### Option 2: Configure PHP's DNS Cache

```php
// In your PHP code, you can clear DNS cache:
dns_clear_cache(); // If available

// Or use specific DNS server in cURL:
curl_setopt($ch, CURLOPT_DNS_SERVERS, "8.8.8.8,8.8.4.4");
```

---

## Step 6: Verify the Fix

### Test DNS Resolution

```bash
# Should now work
nslookup ph-locations-api.buonzz.com

# Expected output:
# Server: 8.8.8.8
# Address: 8.8.8.8#53
# 
# Non-authoritative answer:
# Name: ph-locations-api.buonzz.com
# Address: [IP address]
```

### Test API from Command Line

```bash
# Test with curl
curl https://ph-locations-api.buonzz.com/v1/provinces

# Should return JSON data, not an error
```

### Test with PHP Script

```bash
# Run the test script again
php BoardEase2/test_api_connectivity_buonzz.php

# Should show ✅ SUCCESS for all tests
```

### Test from Web Browser

Open in browser:
```
https://your-server.com/BoardEase2/philippine_address_api.php?action=provinces
```

Should return JSON with provinces data.

---

## Step 7: Common Issues and Solutions

### Issue 1: DNS changes are being overwritten

**Problem:** You edit `/etc/resolv.conf` but it gets reset.

**Solution:**
- Use NetworkManager or systemd-resolved configuration (see Method 1, Option B/C)
- Make DNS settings permanent

### Issue 2: Only localhost DNS server

**Problem:** `/etc/resolv.conf` shows `nameserver 127.0.0.1` only

**Solution:**
- Add external DNS servers (8.8.8.8) in addition to localhost
- Or configure your local DNS to forward queries

### Issue 3: SELinux blocking (CentOS/RHEL)

**Problem:** DNS works but connections still fail

**Solution:**
```bash
# Check SELinux status
getenforce

# If Enforcing, temporarily allow network connections
sudo setenforce 0

# Test if this fixes it
# If yes, make it permanent:
sudo nano /etc/selinux/config
# Set: SELINUX=permissive
```

### Issue 4: Corporate/Managed Server

**Problem:** You don't have root access to change DNS

**Solution:**
- Contact your server administrator/hosting provider
- Ask them to:
  1. Add external DNS servers (8.8.8.8, 8.8.4.4)
  2. Allow HTTPS outbound connections (port 443)
  3. Allow DNS queries (UDP port 53)

### Issue 5: Shared Hosting

**Problem:** You can't modify server DNS settings

**Solution:**
- Contact hosting provider support
- Explain: "Server cannot resolve ph-locations-api.buonzz.com"
- Ask them to check DNS configuration
- Alternative: Use a different hosting/VPS where you have control

---

## Step 8: Permanent Solution - Use Reliable DNS

### Recommended DNS Servers

Use these reliable, public DNS servers:

1. **Google DNS** (Most Popular)
   - Primary: `8.8.8.8`
   - Secondary: `8.8.4.4`

2. **Cloudflare DNS** (Fast)
   - Primary: `1.1.1.1`
   - Secondary: `1.0.0.1`

3. **Quad9 DNS** (Security-focused)
   - Primary: `9.9.9.9`
   - Secondary: `149.112.112.112`

4. **OpenDNS**
   - Primary: `208.67.222.222`
   - Secondary: `208.67.220.220`

### Best Practice Configuration

```bash
# /etc/resolv.conf
nameserver 8.8.8.8      # Google DNS (Primary)
nameserver 8.8.4.4      # Google DNS (Secondary)
nameserver 1.1.1.1      # Cloudflare DNS (Backup)
nameserver 9.9.9.9      # Quad9 (Extra backup)
```

---

## Step 9: Monitoring and Troubleshooting

### Check DNS Resolution from PHP

Create a test file `test_dns_from_php.php`:

```php
<?php
$host = 'ph-locations-api.buonzz.com';
$ip = gethostbyname($host);

if ($ip === $host) {
    echo "❌ DNS resolution FAILED\n";
    echo "Host cannot be resolved\n";
} else {
    echo "✅ DNS resolution SUCCESS\n";
    echo "Resolved to: $ip\n";
}

// Test multiple DNS servers
$dns_servers = ['8.8.8.8', '8.8.4.4', '1.1.1.1'];
foreach ($dns_servers as $dns) {
    $result = dns_get_record($host, DNS_A, ['nameserver' => $dns]);
    echo "Testing with DNS $dns: " . (empty($result) ? "FAILED" : "SUCCESS") . "\n";
}
?>
```

### Log DNS Queries (Advanced)

```bash
# Enable DNS query logging (temporary, for debugging)
sudo rndc querylog on

# Check DNS logs
tail -f /var/log/named.log  # BIND
# or
journalctl -u systemd-resolved -f  # systemd-resolved
```

---

## Quick Reference: Commands Summary

```bash
# 1. Test DNS
nslookup ph-locations-api.buonzz.com
dig ph-locations-api.buonzz.com

# 2. Check current DNS
cat /etc/resolv.conf

# 3. Fix DNS (Linux)
sudo nano /etc/resolv.conf
# Add: nameserver 8.8.8.8

# 4. Test connectivity
ping 8.8.8.8
curl https://ph-locations-api.buonzz.com/v1/provinces

# 5. Run test script
php BoardEase2/test_api_connectivity_buonzz.php

# 6. Flush DNS cache (if needed)
sudo systemd-resolve --flush-caches  # systemd
sudo service named restart  # BIND
ipconfig /flushdns  # Windows
```

---

## Still Having Issues?

If DNS is still not working after trying all methods:

1. **Contact your hosting provider** - They may have restrictions
2. **Check if server has internet access** - `ping 8.8.8.8`
3. **Try from different server** - Test if it's server-specific
4. **Check if domain is accessible** - Visit `https://ph-locations-api.buonzz.com/v1/provinces` in browser
5. **Review server logs** - Check `/var/log/messages` or `/var/log/syslog`

---

## Success Indicators

You'll know DNS is fixed when:

✅ `nslookup ph-locations-api.buonzz.com` returns an IP address  
✅ `curl https://ph-locations-api.buonzz.com/v1/provinces` returns JSON  
✅ Test script shows all ✅ SUCCESS  
✅ PHP API returns data instead of errors  
✅ No more "Could not resolve host" errors in logs  

Good luck! 🚀






