#!/bin/bash
set -e

# TradingSandbox Manual Deploy Script
# Usage: ./deploy.sh [SERVER_IP]

SERVER_IP="${1:-}"
SERVER_USER="ec2-user"
APP_DIR="/opt/tradingsandbox"

if [ -z "$SERVER_IP" ]; then
    echo "Usage: ./deploy.sh <SERVER_IP>"
    echo "Example: ./deploy.sh 54.123.45.67"
    exit 1
fi

echo "🚀 Deploying TradingSandbox to $SERVER_IP..."

# Sync code to server (excluding unnecessary files)
echo "📦 Syncing code..."
rsync -avz --progress \
    --exclude '.git' \
    --exclude 'node_modules' \
    --exclude 'target' \
    --exclude 'venv' \
    --exclude '__pycache__' \
    --exclude '.env' \
    --exclude '.env.prod' \
    --exclude 'dist' \
    --exclude '*.log' \
    ./ ${SERVER_USER}@${SERVER_IP}:${APP_DIR}/

# Build and restart on server
echo "🔨 Building and starting services..."
ssh ${SERVER_USER}@${SERVER_IP} << 'ENDSSH'
cd /opt/tradingsandbox

# Check if .env.prod exists
if [ ! -f .env.prod ]; then
    echo "⚠️  .env.prod not found! Copy from .env.prod.example and configure."
    exit 1
fi

# Build and restart
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d

# Show status
echo ""
echo "✅ Deployment complete!"
docker-compose -f docker-compose.prod.yml ps
ENDSSH

echo ""
echo "🌐 Your app is running at:"
echo "   Frontend: http://${SERVER_IP}"
echo "   API:      http://${SERVER_IP}:8080"
echo "   Data API: http://${SERVER_IP}:8000"
