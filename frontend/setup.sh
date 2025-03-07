#!/bin/bash

echo "🚀 Setting up the frontend..."

# Install dependencies
echo "📦 Installing dependencies..."
npm install

# Install Tailwind CSS & dependencies
echo "🎨 Installing Tailwind CSS..."
npm install tailwindcss @tailwindcss/vite

# Install DaisyUI
echo "🌼 Installing DaisyUI..."
npm install daisyui

# Run the dev server
echo "✅ Setup complete! Starting development server..."
npm run dev