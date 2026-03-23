#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

PIDFILE=".komorebi.pids"

# Copy .env.example if .env.local doesn't exist yet
if [ ! -f .env.local ]; then
  cp .env.example .env.local
  echo "Created .env.local from .env.example — edit it to add your API keys."
fi

# Install Node deps if needed
if [ ! -d node_modules ]; then
  echo "Installing dependencies..."
  npm install
fi

# Start Scala backend
echo "Starting backend (sbt run)..."
(cd komorebi-server && sbt run) &
SBT_PID=$!

# Wait for backend to be ready
echo "Waiting for backend on :8080..."
for i in $(seq 1 60); do
  if curl -s http://localhost:8080/api/history > /dev/null 2>&1; then
    echo "Backend ready."
    break
  fi
  sleep 1
done

# Start Next.js frontend
echo "Starting frontend (npm run dev)..."
npm run dev &
NEXT_PID=$!

# Save PIDs for stop.sh
echo "$SBT_PID $NEXT_PID" > "$PIDFILE"
echo "Running — backend PID=$SBT_PID, frontend PID=$NEXT_PID"
echo "Use ./stop.sh to stop both."

wait
