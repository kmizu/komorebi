#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Copy .env.example if .env.local doesn't exist yet
if [ ! -f .env.local ]; then
  cp .env.example .env.local
  echo "Created .env.local from .env.example — edit it to add your API keys."
fi

# Install deps if needed
if [ ! -d node_modules ]; then
  echo "Installing dependencies..."
  npm install
fi

echo "Starting dev server..."
npm run dev
