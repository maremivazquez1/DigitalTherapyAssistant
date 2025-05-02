#!/bin/bash

# Exit on error
set -e

echo "🚀 Starting full-stack setup..."

# Step 1: Install or Update Essential Tools (Global)
echo "🔍 Checking Homebrew..."
if ! command -v brew &> /dev/null; then
    echo "📥 Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
else
    echo "✅ Homebrew is already installed. Updating..."
    brew update
fi

# Install Java (OpenJDK)
echo "🔍 Checking Java..."
if ! command -v java &> /dev/null; then
    echo "📥 Installing Java JDK..."
    brew install openjdk
    echo 'export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"' >> ~/.zshrc
    source ~/.zshrc
else
    echo "✅ Java is already installed."
fi

# Step 2: Backend Setup
BACKEND_DIR="backend"

if [ -d "$BACKEND_DIR" ]; then
    cd "$BACKEND_DIR"
    echo "📂 Switched to backend directory: $BACKEND_DIR"

    # Check and Install/Update Maven
    echo "🔍 Checking Maven..."
    if command -v mvn &> /dev/null; then
        echo "🔄 Maven is already installed. Updating..."
        brew upgrade maven || echo "✅ Maven is already up to date."
    else
        echo "📥 Installing Maven..."
        brew install maven
    fi

    # Check and Install/Update MySQL
    echo "🔍 Checking MySQL..."
    if command -v mysql &> /dev/null; then
        echo "🔄 MySQL is already installed. Updating..."
        brew upgrade mysql || echo "✅ MySQL is already up to date."
    else
        echo "📥 Installing MySQL..."
        brew install mysql
        brew services start mysql
    fi


    # Redis Setup
    echo "🔍 Checking Redis..."
    if command -v redis-server &> /dev/null; then
        echo "🔄 Redis is already installed. Updating..."
        brew upgrade redis || echo "✅ Redis is already up to date."
    else
        echo "📥 Installing Redis..."
        brew install redis
    fi

    # Start Redis service
    echo "🚀 Starting Redis service..."
    brew services start redis || echo "✅ Redis service is already running."

    # Verify installations
    echo "✅ Verifying installations..."
    java -version
    brew --version
    mvn -version
    mysql --version

    # MySQL Setup
    echo "🛠️ Configuring MySQL..."
    mysql -u root <<EOF
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
EOF
    echo "✅ MySQL setup completed."


    echo "🔍 Checking ffmpeg dependency..."
    if command -v ffmpeg &> /dev/null; then
        echo "🔄 ffmpeg is already installed. Updating..."
        brew upgrade ffmpeg || echo "✅ ffmpeg is already up to date."
    else
        echo "Installing ffmpeg dependency..."
        brew install ffmpeg
    fi

    # Build and run backend
    echo "🚀 Building the backend..."
    mvn clean install -Dnet.bytebuddy.experimental=true
    echo "✅ Backend setup complete."

    cd ..
else
    echo "⚠️ Backend directory '$BACKEND_DIR' not found. Skipping backend setup."
fi

# Step 3: Frontend Setup
FRONTEND_DIR="frontend"

if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    echo "📂 Switched to frontend directory: $FRONTEND_DIR"

    echo "🔍 Checking Node.js..."
    if ! command -v node &> /dev/null; then
        echo "📥 Installing Node.js..."
        brew install node
    else
        echo "✅ Node.js is already installed."
    fi

    echo "📦 Checking and installing frontend dependencies..."
    if [ ! -d "node_modules" ]; then
        npm install
    else
        echo "✅ Frontend dependencies are already installed."
    fi

    echo "🎨 Checking and installing Tailwind CSS..."
    if ! npm list tailwindcss &> /dev/null; then
        npm install tailwindcss @tailwindcss/vite
    else
        echo "✅ Tailwind CSS is already installed."
    fi

    echo "🌼 Checking and installing DaisyUI..."
    if ! npm list daisyui &> /dev/null; then
        npm install daisyui@latest
    else
        echo "✅ DaisyUI is already installed."
    fi

    echo "⚡ Checking and installing Vite..."
    if ! npm list vite &> /dev/null; then
        npm install -D vite
    else
        echo "✅ Vite is already installed."
    fi

    echo "✅ Frontend setup complete! Starting development server..."
    npm run dev

    cd ..
else
    echo "⚠️ Frontend directory '$FRONTEND_DIR' not found. Skipping frontend setup."
fi

echo "🎉 Full-stack setup complete!"