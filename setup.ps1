# Windows Powershell setup script: cmd = .\setup.p1 to execute
# Exit on any error
$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "Starting setup script..."

# --- Chocolatey ---
Write-Host ""
Write-Host "Checking for Chocolatey..."
$chocoExists = Get-Command choco -ErrorAction SilentlyContinue

if (-not $chocoExists) {
    Write-Host "Chocolatey is NOT installed. Installing now..."
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $script = (New-Object Net.WebClient).DownloadString("https://chocolatey.org/install.ps1")
    Invoke-Expression $script
    Write-Host "Chocolatey installation attempted."
} else {
    Write-Host "Chocolatey is already installed."
}

# --- Java ---
Write-Host ""
Write-Host "Checking for Java..."
$javaExists = Get-Command java -ErrorAction SilentlyContinue

if (-not $javaExists) {
    Write-Host "Java is NOT installed. Installing OpenJDK 17..."
    choco install openjdk --version=17 -y
    Write-Host "Java installation attempted."
} else {
    Write-Host "Java is already installed."
}

# --- MySQL setup ---
Write-Host ""
Write-Host "Checking for MySQL..."

$mysqlExists = Get-Command mysql -ErrorAction SilentlyContinue

if (-not $mysqlExists) {
    Write-Host "MySQL is NOT installed. Installing..."
    choco install mysql -y
    Write-Host "MySQL installation attempted."
} else {
    Write-Host "MySQL is already installed."
}

# Attempt to start the MySQL service
Write-Host "Trying to start MySQL service..."
try {
    Start-Service -Name 'mysql' -ErrorAction Stop
    Write-Host "MySQL service started."
} catch {
    Write-Host "MySQL service could not be started. It may already be running or require manual setup."
}

# Run SQL to create database and users table
Write-Host "Running MySQL setup script..."

$setupSql = @"
CREATE DATABASE IF NOT EXISTS cbt;
USE cbt;
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
"@

try {
    mysql -u root -e $setupSql
    Write-Host "MySQL setup completed successfully."
} catch {
    Write-Host "Failed to connect to MySQL. You may need to set the root password or configure access."
}

# --- Redis setup ---
Write-Host ""
Write-Host "Checking for Redis..."

$redisExists = Get-Command redis-server -ErrorAction SilentlyContinue

if (-not $redisExists) {
    Write-Host "Redis is NOT installed. Installing..."
    choco install redis-64 -y
    Write-Host "Redis installation attempted."
} else {
    Write-Host "Redis is already installed."
}

# Start Redis service
Write-Host "Attempting to start Redis service..."
try {
    Start-Service -Name 'Redis' -ErrorAction Stop
    Write-Host "Redis service started."
} catch {
    Write-Host "Could not start Redis service. It may already be running or may need manual setup."
}


# --- Backend setup ---
$backendDir = "backend"
Write-Host ""
Write-Host "Checking for backend directory..."

if (Test-Path $backendDir) {
    Write-Host "Backend directory found. Entering..."

    Push-Location $backendDir

    Write-Host "Checking for mvnd (Maven Daemon)..."
    $mvndExists = Get-Command mvnd -ErrorAction SilentlyContinue

    if (-not $mvndExists) {
        Write-Host "mvnd is NOT installed. Installing..."
        choco install mvnd -y
        Write-Host "mvnd installation attempted."
    } else {
        Write-Host "mvnd is already installed."
    }

    Write-Host "Building backend with mvnd..."
    mvnd clean install -Dnet.bytebuddy.experimental=true

    Pop-Location
} else {
    Write-Host "Backend directory not found. Skipping backend setup."
}

# --- Frontend setup ---
$frontendDir = "frontend"
Write-Host ""
Write-Host "Checking for frontend directory..."

if (Test-Path $frontendDir) {
    Write-Host "Frontend directory found. Entering..."
    Push-Location $frontendDir

    # Node.js check
    Write-Host "Checking for Node.js..."
    $nodeExists = Get-Command node -ErrorAction SilentlyContinue
    if (-not $nodeExists) {
        Write-Host "Node.js is NOT installed. Installing..."
        choco install nodejs -y
        Write-Host "Node.js installation attempted."
    } else {
        Write-Host "Node.js is already installed."
    }

    # npm install
    Write-Host "Checking for node_modules..."
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing frontend dependencies with npm..."
        npm install
    } else {
        Write-Host "Frontend dependencies already installed."
    }

    # Tailwind check
    Write-Host "Checking for Tailwind CSS..."
    $tailwindInstalled = npm list tailwindcss --depth=0 | Out-String
    if ($tailwindInstalled -notmatch "tailwindcss") {
        Write-Host "Installing Tailwind CSS and plugin..."
        npm install tailwindcss @tailwindcss/vite
    } else {
        Write-Host "Tailwind CSS already installed."
    }

    # DaisyUI check
    Write-Host "Checking for DaisyUI..."
    $daisyInstalled = npm list daisyui --depth=0 | Out-String
    if ($daisyInstalled -notmatch "daisyui") {
        Write-Host "Installing DaisyUI..."
        npm install daisyui@latest
    } else {
        Write-Host "DaisyUI already installed."
    }

    # Vite check
    Write-Host "Checking for Vite..."
    $viteInstalled = npm list vite --depth=0 | Out-String
    if ($viteInstalled -notmatch "vite") {
        Write-Host "Installing Vite..."
        npm install -D vite
    } else {
        Write-Host "Vite already installed."
    }

    Write-Host "Frontend setup complete. You can now run 'npm run dev' manually."

    Pop-Location
} else {
    Write-Host "Frontend directory not found. Skipping frontend setup."
}

Write-Host ""
Write-Host "Script finished."
